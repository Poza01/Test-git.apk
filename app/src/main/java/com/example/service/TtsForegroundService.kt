package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class TtsForegroundService : Service(), TextToSpeech.OnInitListener {

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val _playbackState = MutableStateFlow(TtsPlaybackState())
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val prefs by lazy { com.example.data.NovelPreferences(applicationContext) }

    private var currentUtteranceId = 0L
    private var pendingSpeechAction: (() -> Unit)? = null
    private var currentWebUtteranceId: String? = null
    private var isWebAudioPlaying: Boolean = false
    private var webIdleJob: kotlinx.coroutines.Job? = null
    @Volatile
    private var isUserPaused: Boolean = false

    data class WebSpeechHistoryItem(val text: String, val title: String, val utteranceId: String)
    private val webSpeechHistory = mutableListOf<WebSpeechHistoryItem>()
    private var webHistoryIndex: Int = -1

    var onUtteranceEvent: ((event: String, utteranceId: String) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): TtsForegroundService = this@TtsForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        acquireWakeLock()
        initTtsEngine()
        startAsForegroundService()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NovelAI::TtsForegroundWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 60 * 1000L) // 10 hours safety window
            }
            Log.d(TAG, "WakeLock acquired for background reading")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun initTtsEngine() {
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val thaiLocale = Locale("th", "TH")
            val langResult = tts?.setLanguage(thaiLocale)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Thai locale missing data or not supported, falling back to default")
                tts?.setLanguage(Locale.getDefault())
            }

            val defaultEngine = tts?.defaultEngine ?: "Android TTS"

            _playbackState.update { current ->
                current.copy(
                    isInitialized = true,
                    engineName = defaultEngine,
                    availableVoices = emptyList(),
                    selectedVoiceName = "ค่าเริ่มต้นของระบบ ROM",
                    speechRate = 1.0f,
                    speechPitch = 1.0f
                )
            }

            setupUtteranceListener()
            updateForegroundNotification()
            Log.d(TAG, "TextToSpeech initialized with raw native engine=$defaultEngine")

            // Execute any pending speech requested before initialization was complete
            val pending = pendingSpeechAction
            pendingSpeechAction = null
            pending?.invoke()
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine, status=$status")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                serviceScope.launch {
                    isUserPaused = false
                    _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
                    updateForegroundNotification()
                    if (!utteranceId.isNullOrBlank()) {
                        onUtteranceEvent?.invoke("onstart", utteranceId)
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                serviceScope.launch {
                    if (isUserPaused || _playbackState.value.isPaused) {
                        Log.d(TAG, "Ignoring onDone because user is paused: $utteranceId")
                        return@launch
                    }
                    if (!utteranceId.isNullOrBlank()) {
                        onUtteranceEvent?.invoke("ondone", utteranceId)
                    }
                    if (utteranceId?.startsWith("web_utt_") == true) {
                        // If we are rewound in history and there are subsequent lines in history, read next line automatically!
                        if (webHistoryIndex >= 0 && webHistoryIndex < webSpeechHistory.size - 1) {
                            webHistoryIndex++
                            val nextItem = webSpeechHistory[webHistoryIndex]
                            currentWebUtteranceId = nextItem.utteranceId
                            _playbackState.update {
                                it.copy(
                                    chapterTitle = nextItem.title.ifBlank { "อ่านนิยายเว็บ" },
                                    currentText = nextItem.text,
                                    isPlaying = true,
                                    isPaused = false
                                )
                            }
                            updateForegroundNotification()
                            val nextUtteranceId = "web_utt_${nextItem.utteranceId}"
                            tts?.speak(nextItem.text, TextToSpeech.QUEUE_FLUSH, null, nextUtteranceId)
                        } else {
                            val expectedId = "web_utt_$currentWebUtteranceId"
                            if (utteranceId == expectedId) {
                                webIdleJob?.cancel()
                                webIdleJob = serviceScope.launch {
                                    // Wait 8 seconds of continuous silence before marking as idle/stopped
                                    kotlinx.coroutines.delay(8000L)
                                    if (_playbackState.value.isPlaying && !isWebAudioPlaying && tts?.isSpeaking != true) {
                                        _playbackState.update { it.copy(isPlaying = false, isPaused = false) }
                                        updateForegroundNotification()
                                    }
                                }
                            }
                        }
                    } else {
                        handleParagraphCompleted()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                Log.w(TAG, "Utterance error: $utteranceId, currentWebUtteranceId=$currentWebUtteranceId, isUserPaused=$isUserPaused")
                // If the error was triggered by pausing the playback (tts.stop()), do NOT treat as fatal error!
                if (isUserPaused || _playbackState.value.isPaused) {
                    Log.d(TAG, "Ignoring onError for paused utterance: $utteranceId")
                    if (!utteranceId.isNullOrBlank()) {
                        onUtteranceEvent?.invoke("onpause", utteranceId)
                    }
                    return
                }

                if (utteranceId?.startsWith("web_utt_") == true) {
                    val expectedId = "web_utt_$currentWebUtteranceId"
                    if (utteranceId != expectedId) {
                        Log.d(TAG, "Ignoring onError for flushed utterance: $utteranceId")
                        return
                    }
                }
                serviceScope.launch {
                    if (!utteranceId.isNullOrBlank()) {
                        onUtteranceEvent?.invoke("onerror", utteranceId)
                    }
                    if (utteranceId?.startsWith("web_utt_") == true) {
                        webIdleJob?.cancel()
                        _playbackState.update { it.copy(isPlaying = false, isPaused = false) }
                        updateForegroundNotification()
                    } else {
                        handleParagraphCompleted()
                    }
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                Log.d(TAG, "Utterance stopped/interrupted: $utteranceId, interrupted=$interrupted, isUserPaused=$isUserPaused")
                if (isUserPaused || _playbackState.value.isPaused) {
                    if (!utteranceId.isNullOrBlank()) {
                        onUtteranceEvent?.invoke("onpause", utteranceId)
                    }
                }
            }
        })
    }

    private fun handleParagraphCompleted() {
        val state = _playbackState.value
        if (!state.isPlaying || state.isPaused) return

        val nextIndex = state.activeParagraphIndex + 1
        if (nextIndex < state.paragraphs.size) {
            speakParagraphInternal(nextIndex)
        } else {
            // End of chapter playlist - Notify bridge to load next chapter / flip page automatically!
            Log.d(TAG, "Chapter playlist reached end, triggering automatic next chapter transition")
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isPaused = false,
                    activeParagraphIndex = -1,
                    currentText = "",
                    paragraphs = emptyList()
                )
            }
            updateForegroundNotification()
            com.example.bridge.NovelTtsBridge.notifyNextChapterFromService()
        }
    }

    fun playPlaylist(paragraphs: List<String>, title: String, startIndex: Int = 0) {
        val cleanList = paragraphs.filter { it.isNotBlank() }
        if (cleanList.isEmpty()) return

        if (!_playbackState.value.isInitialized || tts == null) {
            Log.d(TAG, "TTS not ready yet, queuing playPlaylist")
            pendingSpeechAction = { playPlaylist(paragraphs, title, startIndex) }
            return
        }

        val safeStart = startIndex.coerceIn(0, cleanList.size - 1)
        _playbackState.update {
            it.copy(
                paragraphs = cleanList,
                chapterTitle = title.ifBlank { "กำลังอ่านนิยาย" },
                totalParagraphs = cleanList.size,
                activeParagraphIndex = safeStart,
                isPlaying = true,
                isPaused = false
            )
        }

        startAsForegroundService()
        speakParagraphInternal(safeStart)
    }

    fun playSingleText(text: String, title: String = "อ่านข้อความ") {
        if (text.isBlank()) return
        val paragraphs = text.split("\n\n", "\n").filter { it.isNotBlank() }
        playPlaylist(paragraphs, title, 0)
    }

    fun speakFromWeb(text: String, title: String, webUtteranceId: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        webIdleJob?.cancel()
        currentWebUtteranceId = webUtteranceId
        isWebAudioPlaying = false

        // Keep history for rewinding sentences seamlessly
        if (webSpeechHistory.isEmpty() || webSpeechHistory.lastOrNull()?.text != cleanText) {
            webSpeechHistory.add(WebSpeechHistoryItem(cleanText, title, webUtteranceId))
            if (webSpeechHistory.size > 300) {
                webSpeechHistory.removeAt(0)
            }
        }
        webHistoryIndex = webSpeechHistory.size - 1

        if (!_playbackState.value.isInitialized || tts == null) {
            Log.d(TAG, "TTS not ready yet, queuing speakFromWeb")
            pendingSpeechAction = { speakFromWeb(text, title, webUtteranceId) }
            return
        }

        _playbackState.update {
            it.copy(
                chapterTitle = title.ifBlank { "อ่านนิยายเว็บ" },
                currentText = cleanText,
                isPlaying = true,
                isPaused = false,
                engineName = "เสียงในเครื่อง (Device TTS)"
            )
        }

        startAsForegroundService()

        val utteranceId = "web_utt_$webUtteranceId"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun onWebAudioStarted(title: String, text: String, engineName: String) {
        webIdleJob?.cancel()
        isWebAudioPlaying = true
        _playbackState.update {
            it.copy(
                isPlaying = true,
                isPaused = false,
                chapterTitle = title.ifBlank { "กำลังอ่านนิยาย" },
                currentText = text.ifBlank { "กำลังเล่นเสียง..." },
                engineName = engineName.ifBlank { "Google / Microsoft TTS" }
            )
        }
        startAsForegroundService()
        updateForegroundNotification()
    }

    fun onWebAudioPaused() {
        webIdleJob?.cancel()
        isWebAudioPlaying = false
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isPaused = true
            )
        }
        updateForegroundNotification()
    }

    fun onWebAudioEnded() {
        webIdleJob?.cancel()
        webIdleJob = serviceScope.launch {
            kotlinx.coroutines.delay(2000L)
            if (isWebAudioPlaying) {
                isWebAudioPlaying = false
                _playbackState.update { it.copy(isPlaying = false) }
                updateForegroundNotification()
            }
        }
    }

    private fun speakParagraphInternal(index: Int) {
        val state = _playbackState.value
        if (index < 0 || index >= state.paragraphs.size) return

        val textToSpeak = state.paragraphs[index].trim()
        if (textToSpeak.isBlank()) {
            handleParagraphCompleted()
            return
        }

        _playbackState.update {
            it.copy(
                activeParagraphIndex = index,
                currentText = textToSpeak,
                isPlaying = true,
                isPaused = false
            )
        }

        updateForegroundNotification()

        currentUtteranceId++
        val utteranceId = "novel_tts_${currentUtteranceId}_$index"

        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun pause() {
        isUserPaused = true
        webIdleJob?.cancel()
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _playbackState.update { it.copy(isPlaying = false, isPaused = true) }
        updateForegroundNotification()
        com.example.bridge.NovelTtsBridge.notifyPauseFromService()
    }

    fun togglePlayPause() {
        val currentlyPlaying = _playbackState.value.isPlaying || (tts?.isSpeaking == true)
        if (currentlyPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun resume() {
        isUserPaused = false
        val state = _playbackState.value
        webIdleJob?.cancel()
        startAsForegroundService()
        _playbackState.update { it.copy(isPlaying = true, isPaused = false) }
        updateForegroundNotification()

        if (state.activeParagraphIndex >= 0 && state.activeParagraphIndex < state.paragraphs.size) {
            speakParagraphInternal(state.activeParagraphIndex)
        } else if (state.paragraphs.isNotEmpty()) {
            speakParagraphInternal(0)
        } else if (isWebAudioPlaying) {
            // Web Audio (HTML5 <audio>): resume via WebView controls
            com.example.bridge.NovelTtsBridge.notifyPlayResumeFromService()
        } else if (state.currentText.isNotBlank() && state.currentText != "กำลังเล่นเสียง...") {
            val uttId = currentWebUtteranceId ?: "0"
            val utteranceId = "web_utt_$uttId"
            tts?.speak(state.currentText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            com.example.bridge.NovelTtsBridge.notifyPlayResumeFromService()
        } else if (webSpeechHistory.isNotEmpty()) {
            val lastItem = webSpeechHistory.getOrNull(webHistoryIndex) ?: webSpeechHistory.last()
            currentWebUtteranceId = lastItem.utteranceId
            val utteranceId = "web_utt_${lastItem.utteranceId}"
            _playbackState.update {
                it.copy(
                    chapterTitle = lastItem.title.ifBlank { "อ่านนิยายเว็บ" },
                    currentText = lastItem.text,
                    isPlaying = true,
                    isPaused = false
                )
            }
            tts?.speak(lastItem.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            com.example.bridge.NovelTtsBridge.notifyPlayResumeFromService()
        } else {
            // Web Audio (Google / Microsoft) or general web reader
            com.example.bridge.NovelTtsBridge.notifyPlayResumeFromService()
        }
    }

    fun pauseFromWeb() {
        isUserPaused = true
        webIdleJob?.cancel()
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _playbackState.update { it.copy(isPlaying = false, isPaused = true) }
        updateForegroundNotification()
    }

    fun stopFromWeb() {
        isUserPaused = false
        webIdleJob?.cancel()
        tts?.stop()
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                currentText = ""
            )
        }
        updateForegroundNotification()
    }

    fun stop() {
        isUserPaused = false
        webIdleJob?.cancel()
        tts?.stop()
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                activeParagraphIndex = -1,
                currentText = "",
                paragraphs = emptyList()
            )
        }
        updateForegroundNotification()
        com.example.bridge.NovelTtsBridge.notifyStopFromService()
    }

    fun skipNext() {
        val state = _playbackState.value
        if (state.paragraphs.isNotEmpty() && state.activeParagraphIndex < state.paragraphs.size - 1) {
            val nextIndex = (state.activeParagraphIndex + 1).coerceAtMost(state.paragraphs.size - 1)
            speakParagraphInternal(nextIndex)
        } else if (webSpeechHistory.isNotEmpty() && webHistoryIndex >= 0 && webHistoryIndex < webSpeechHistory.size - 1) {
            webHistoryIndex++
            val nextItem = webSpeechHistory[webHistoryIndex]
            webIdleJob?.cancel()
            currentWebUtteranceId = nextItem.utteranceId
            _playbackState.update {
                it.copy(
                    chapterTitle = nextItem.title.ifBlank { "อ่านนิยายเว็บ" },
                    currentText = nextItem.text,
                    isPlaying = true,
                    isPaused = false
                )
            }
            updateForegroundNotification()
            val utteranceId = "web_utt_${nextItem.utteranceId}"
            tts?.speak(nextItem.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            com.example.bridge.NovelTtsBridge.notifyNextFromService()
        } else {
            com.example.bridge.NovelTtsBridge.notifyNextFromService()
        }
    }

    fun skipPrevious() {
        val state = _playbackState.value
        if (state.paragraphs.isNotEmpty() && state.activeParagraphIndex > 0) {
            val prevIndex = (state.activeParagraphIndex - 1).coerceAtLeast(0)
            speakParagraphInternal(prevIndex)
        } else if (webSpeechHistory.isNotEmpty() && webHistoryIndex > 0) {
            webHistoryIndex--
            val prevItem = webSpeechHistory[webHistoryIndex]
            webIdleJob?.cancel()
            currentWebUtteranceId = prevItem.utteranceId
            _playbackState.update {
                it.copy(
                    chapterTitle = prevItem.title.ifBlank { "อ่านนิยายเว็บ" },
                    currentText = prevItem.text,
                    isPlaying = true,
                    isPaused = false
                )
            }
            updateForegroundNotification()
            val utteranceId = "web_utt_${prevItem.utteranceId}"
            tts?.speak(prevItem.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            com.example.bridge.NovelTtsBridge.notifyPrevFromService()
        } else {
            com.example.bridge.NovelTtsBridge.notifyPrevFromService()
        }
    }

    fun setSpeechRate(rate: Float) {
        val cleanRate = rate.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(cleanRate)
        prefs.defaultRate = cleanRate
        _playbackState.update { it.copy(speechRate = cleanRate) }
    }

    fun setSpeechPitch(pitch: Float) {
        val cleanPitch = pitch.coerceIn(0.5f, 1.8f)
        tts?.setPitch(cleanPitch)
        prefs.defaultPitch = cleanPitch
        _playbackState.update { it.copy(speechPitch = cleanPitch) }
    }

    fun setVoice(voiceName: String) {
        try {
            val targetVoice = tts?.voices?.find { it.name == voiceName }
            if (targetVoice != null) {
                tts?.voice = targetVoice
                prefs.selectedVoice = voiceName
                _playbackState.update { it.copy(selectedVoiceName = voiceName) }
                Log.d(TAG, "Voice changed and saved: $voiceName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error changing voice", e)
        }
    }

    private fun startAsForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateForegroundNotification() {
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundIfIdle() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(): Notification {
        val state = _playbackState.value
        val title = state.chapterTitle.ifBlank { "เครื่องเล่นเสียงนิยาย" }
        val paraInfo = if (state.totalParagraphs > 0 && state.activeParagraphIndex >= 0) {
            "ย่อหน้าที่ ${state.activeParagraphIndex + 1} จาก ${state.totalParagraphs}"
        } else if (state.currentText.isNotBlank()) {
            state.currentText.take(60)
        } else {
            "พร้อมอ่านเสียงภาษาไทยเบื้องหลัง"
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous intent
        val prevIntent = Intent(this, TtsForegroundService::class.java).apply { action = ACTION_PREV }
        val pendingPrev = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Play/Pause toggle intent
        val playPauseIntent = Intent(this, TtsForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val pendingPlayPause = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next intent
        val nextIntent = Intent(this, TtsForegroundService::class.java).apply { action = ACTION_NEXT }
        val pendingNext = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Stop intent
        val stopIntent = Intent(this, TtsForegroundService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return try {
            val isPlaying = state.isPlaying
            val compactViews = RemoteViews(packageName, R.layout.notification_custom_player).apply {
                setImageViewResource(
                    R.id.btn_notif_play_pause,
                    if (isPlaying) R.drawable.ic_notif_pause_dark else R.drawable.ic_notif_play_dark
                )
                setOnClickPendingIntent(R.id.btn_notif_prev, pendingPrev)
                setOnClickPendingIntent(R.id.btn_notif_play_pause, pendingPlayPause)
                setOnClickPendingIntent(R.id.btn_notif_next, pendingNext)
                setOnClickPendingIntent(R.id.btn_notif_stop, pendingStop)
                setOnClickPendingIntent(R.id.notif_root, pendingContentIntent)
            }

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_play)
                .setContentIntent(pendingContentIntent)
                .setOngoing(state.isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(compactViews)
                .build()
        } catch (e: Exception) {
            Log.e("TtsService", "Error creating custom notification, fallback to standard", e)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(paraInfo)
                .setSmallIcon(R.drawable.ic_notif_play)
                .setContentIntent(pendingContentIntent)
                .setOngoing(state.isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .addAction(R.drawable.ic_notif_prev, "ย้อนบรรทัด", pendingPrev)
                .addAction(if (state.isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play, if (state.isPlaying) "พัก" else "เล่น", pendingPlayPause)
                .addAction(R.drawable.ic_notif_next, "ถัดไป", pendingNext)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "เสียงอ่านนิยายเบื้องหลัง (Novel TTS)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "การแจ้งเตือนควบคุมเสียงอ่านนิยายภาษาไทยไม่ดับเบื้องหลัง"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_PREV -> skipPrevious()
            ACTION_NEXT -> skipNext()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        tts?.stop()
        tts?.shutdown()
        tts = null
        releaseWakeLock()
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: TtsForegroundService? = null
            private set

        const val TAG = "TtsForegroundService"
        const val CHANNEL_ID = "novel_tts_playback_channel"
        const val NOTIFICATION_ID = 10086

        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.noveltts.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_PLAY = "com.example.noveltts.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.noveltts.ACTION_PAUSE"
        const val ACTION_PREV = "com.example.noveltts.ACTION_PREV"
        const val ACTION_NEXT = "com.example.noveltts.ACTION_NEXT"
        const val ACTION_STOP = "com.example.noveltts.ACTION_STOP"
    }
}

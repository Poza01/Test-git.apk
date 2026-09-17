package com.example.bridge

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.example.service.TtsForegroundService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class NovelTtsBridge(
    private val context: Context,
    private val getService: () -> TtsForegroundService?,
    private val getWebView: () -> WebView?
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        activeBridge = this
    }

    var onUtteranceEvent: ((event: String, utteranceId: String) -> Unit)? = null
    var onTranslationStatusChange: ((status: String, lang: String) -> Unit)? = null

    fun attachServiceListener() {
        activeBridge = this
        val service = getService() ?: TtsForegroundService.instance ?: return
        service.onUtteranceEvent = { event, utteranceId ->
            mainHandler.post {
                val cleanId = utteranceId.removePrefix("web_utt_")
                val js = "if (window.__android_tts_callback) { window.__android_tts_callback('$event', '$cleanId'); }"
                getWebView()?.evaluateJavascript(js, null)
            }
        }
    }

    fun onNextFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    if (window.__android_tts_next && typeof window.__android_tts_next === 'function') {
                        return window.__android_tts_next();
                    }
                    return false;
                })();
            """.trimIndent(), null)
        }
    }

    fun onPrevFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    if (window.__android_tts_prev && typeof window.__android_tts_prev === 'function') {
                        return window.__android_tts_prev();
                    }
                    return false;
                })();
            """.trimIndent(), null)
        }
    }

    fun onResumeFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    if (window.__android_tts_resume && typeof window.__android_tts_resume === 'function') {
                        return window.__android_tts_resume();
                    }
                    return false;
                })();
            """.trimIndent(), null)
        }
    }

    fun onPauseFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    if (window.__android_tts_pause && typeof window.__android_tts_pause === 'function') {
                        return window.__android_tts_pause();
                    }
                    return false;
                })();
            """.trimIndent(), null)
        }
    }

    @JavascriptInterface
    fun onWebAudioStarted(title: String?, text: String?, engineName: String?) {
        mainHandler.post {
            attachServiceListener()
            val service = getService() ?: TtsForegroundService.instance
            service?.onWebAudioStarted(
                title ?: "กำลังอ่านนิยาย",
                text ?: "",
                engineName ?: "Google / Microsoft TTS"
            )
        }
    }

    @JavascriptInterface
    fun onWebAudioPaused() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.onWebAudioPaused()
        }
    }

    @JavascriptInterface
    fun onWebAudioEnded() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.onWebAudioEnded()
        }
    }

    var onScrollDirectionChange: ((Boolean) -> Unit)? = null

    @JavascriptInterface
    fun onScrollDirection(isDown: Boolean) {
        mainHandler.post {
            onScrollDirectionChange?.invoke(isDown)
        }
    }

    @JavascriptInterface
    fun onTranslationStatus(status: String?, lang: String?) {
        val s = status ?: ""
        val l = lang ?: ""
        mainHandler.post {
            onTranslationStatusChange?.invoke(s, l)
        }
    }

    fun translatePage(targetLang: String = "th") {
        mainHandler.post {
            val js = """
                (function() {
                    if (typeof window.__chrome_translate_to === 'function') {
                        window.__chrome_translate_to('$targetLang');
                    }
                })();
            """.trimIndent()
            getWebView()?.evaluateJavascript(js, null)
        }
    }

    fun restoreOriginal() {
        mainHandler.post {
            val wv = getWebView()
            val current = wv?.url
            if (!current.isNullOrBlank()) {
                try {
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    val uri = android.net.Uri.parse(current)
                    val host = uri.host ?: ""
                    cookieManager.setCookie(current, "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
                    cookieManager.setCookie(current, "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=$host")
                    cookieManager.setCookie(current, "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=.$host")
                    cookieManager.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val js = """
                (function() {
                    try {
                        if (typeof window.__chrome_translate_restore === 'function') {
                            return window.__chrome_translate_restore();
                        }
                    } catch(e){}
                    return false;
                })();
            """.trimIndent()
            wv?.evaluateJavascript(js) { result ->
                val restored = result?.trim()?.removeSurrounding("\"")?.toBooleanStrictOrNull() == true
                if (!restored) {
                    // If in-place restore could not find cached snapshot, fallback to reload while preserving scroll
                    wv.evaluateJavascript("""
                        (function() {
                            try {
                                sessionStorage.setItem('__novel_saved_scroll', window.scrollY || 0);
                            } catch(e){}
                        })();
                    """.trimIndent()) {
                        wv.reload()
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun speakFromWeb(text: String?, title: String?, utteranceId: String?) {
        if (text.isNullOrBlank()) return
        val uttId = utteranceId ?: "0"
        mainHandler.post {
            attachServiceListener()
            val service = getService() ?: TtsForegroundService.instance
            service?.speakFromWeb(text, title ?: "อ่านนิยายเว็บ", uttId)
        }
    }

    @JavascriptInterface
    fun speak(text: String?, title: String?) {
        if (text.isNullOrBlank()) return
        mainHandler.post {
            attachServiceListener()
            val service = getService() ?: TtsForegroundService.instance
            service?.playSingleText(text, title ?: "อ่านนิยาย")
        }
    }

    @JavascriptInterface
    fun speakParagraphs(paragraphsJson: String?, title: String?, startIndex: Int) {
        if (paragraphsJson.isNullOrBlank()) return
        try {
            val jsonArray = JSONArray(paragraphsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
            mainHandler.post {
                attachServiceListener()
                val service = getService() ?: TtsForegroundService.instance
                service?.playPlaylist(list, title ?: "อ่านนิยาย", startIndex)
            }
        } catch (e: Exception) {
            mainHandler.post {
                attachServiceListener()
                val service = getService() ?: TtsForegroundService.instance
                service?.playSingleText(paragraphsJson, title ?: "อ่านนิยาย")
            }
        }
    }

    @JavascriptInterface
    fun pauseFromWeb() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.pauseFromWeb()
        }
    }

    @JavascriptInterface
    fun stopFromWeb() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.stopFromWeb()
        }
    }

    @JavascriptInterface
    fun pause() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.pause()
        }
    }

    @JavascriptInterface
    fun resume() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.resume()
        }
    }

    @JavascriptInterface
    fun stop() {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.stop()
        }
    }

    @JavascriptInterface
    fun setRate(rate: Float) {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.setSpeechRate(rate)
        }
    }

    @JavascriptInterface
    fun setPitch(pitch: Float) {
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.setSpeechPitch(pitch)
        }
    }

    @JavascriptInterface
    fun setVoice(voiceName: String?) {
        if (voiceName.isNullOrBlank()) return
        mainHandler.post {
            val service = getService() ?: TtsForegroundService.instance
            service?.setVoice(voiceName)
        }
    }

    @JavascriptInterface
    fun getAvailableVoicesJson(): String {
        val service = getService() ?: TtsForegroundService.instance
        val voices = service?.playbackState?.value?.availableVoices ?: emptyList()
        val jsonArray = JSONArray()
        if (voices.isNotEmpty()) {
            for (v in voices) {
                val obj = JSONObject().apply {
                    put("name", v.name)
                    put("locale", v.locale)
                    put("quality", v.quality)
                }
                jsonArray.put(obj)
            }
        } else {
            val defaultThai = JSONObject().apply {
                put("name", "th-th-x-default")
                put("locale", "th_TH")
                put("quality", "Thai Voice (Auto Sync)")
            }
            jsonArray.put(defaultThai)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun isAppBridgeActive(): Boolean = true

    @JavascriptInterface
    fun saveBase64File(base64Data: String?, mimeType: String?, fileName: String?) {
        if (base64Data.isNullOrBlank()) return
        val rawName = (fileName?.ifBlank { null } ?: "novel_${System.currentTimeMillis()}.txt")
        val cleanFileName = rawName.replace("[/\\\\:*?\"<>|]".toRegex(), "_")
        val cleanMime = mimeType?.ifBlank { null } ?: if (cleanFileName.endsWith(".txt", ignoreCase = true)) "text/plain" else "application/octet-stream"

        Thread {
            try {
                val pureBase64 = if (base64Data.contains(",")) {
                    base64Data.substringAfter(",")
                } else {
                    base64Data
                }
                val bytes = Base64.decode(pureBase64, Base64.DEFAULT)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, cleanMime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(bytes)
                            os.flush()
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                } else {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadDir.exists()) downloadDir.mkdirs()
                    val targetFile = File(downloadDir, cleanFileName)
                    FileOutputStream(targetFile).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                }

                mainHandler.post {
                    Toast.makeText(context, "ดาวน์โหลดไฟล์สำเร็จ: $cleanFileName\n(บันทึกในโฟลเดอร์ Download)", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึกไฟล์: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    companion object {
        const val JS_INTERFACE_NAME = "AndroidTtsBridge"

        @Volatile
        var activeBridge: NovelTtsBridge? = null
            private set

        fun notifyNextFromService() {
            activeBridge?.onNextFromNotification()
        }

        fun notifyPrevFromService() {
            activeBridge?.onPrevFromNotification()
        }

        fun notifyPlayResumeFromService() {
            activeBridge?.onResumeFromNotification()
        }

        fun notifyPauseFromService() {
            activeBridge?.onPauseFromNotification()
        }

        /**
         * Comprehensive Web Speech API (SpeechSynthesis & SpeechSynthesisUtterance) Polyfill Script
         * Injected into the WebView to provide full standard getVoices(), speak(), pause(), cancel(),
         * and bidirectional callback support (onstart, onend, onerror) so automatic continuous paragraph
         * progression on web novel readers works seamlessly.
         */
        const val INJECTION_SCRIPT = """
            (function() {
                try {
                    console.log("[NovelAI Android Bridge] Initializing Full Web Speech & TTS Bridge v2");

                    // 1. Prepare speech synthesis voices polyfill
                    let cachedVoices = [];
                    function updateVoicesFromBridge() {
                        if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.getAvailableVoicesJson === 'function') {
                            try {
                                const raw = window.AndroidTtsBridge.getAvailableVoicesJson();
                                const parsed = JSON.parse(raw || "[]");
                                if (Array.isArray(parsed) && parsed.length > 0) {
                                    cachedVoices = parsed.map(v => ({
                                        voiceURI: v.name || "th-th-default",
                                        name: v.name || "Thai Voice",
                                        lang: v.locale || "th-TH",
                                        localService: true,
                                        default: (v.locale || '').toLowerCase().includes('th')
                                    }));
                                }
                            } catch(e) {
                                console.warn("[Bridge] Voice parse error", e);
                            }
                        }
                        if (!cachedVoices || cachedVoices.length === 0) {
                            cachedVoices = [
                                { voiceURI: "th-th-native", name: "Thai (Default Google TTS)", lang: "th-TH", localService: true, default: true },
                                { voiceURI: "en-us-native", name: "English (US)", lang: "en-US", localService: true, default: false }
                            ];
                        }
                    }
                    updateVoicesFromBridge();

                    // 2. Ensure SpeechSynthesisUtterance constructor
                    if (typeof window.SpeechSynthesisUtterance === 'undefined') {
                        window.SpeechSynthesisUtterance = function(text) {
                            this.text = text || '';
                            this.lang = 'th-TH';
                            this.voice = null;
                            this.volume = 1.0;
                            this.rate = 1.0;
                            this.pitch = 1.0;
                            this.onstart = null;
                            this.onend = null;
                            this.onerror = null;
                            this.onpause = null;
                            this.onresume = null;
                        };
                    }

                    // 3. Utterance & Sentence History Tracking Map
                    window.__android_tts_utterances = window.__android_tts_utterances || {};
                    window.__android_tts_history = window.__android_tts_history || [];
                    let uttCounter = 0;

                    // Global Android -> JS Event Dispatcher
                    window.__android_tts_callback = function(event, utteranceId) {
                        try {
                            const utt = window.__android_tts_utterances[utteranceId];
                            if (event === 'onstart') {
                                window.__android_active_utterance_id = utteranceId;
                                if (window.speechSynthesis) {
                                    window.speechSynthesis.speaking = true;
                                    window.speechSynthesis.paused = false;
                                }
                                if (utt && typeof utt.onstart === 'function') {
                                    utt.onstart({ type: 'start', utterance: utt });
                                }
                            } else if (event === 'ondone') {
                                if (utt) {
                                    delete window.__android_tts_utterances[utteranceId];
                                    if (window.__android_active_utterance_id === utteranceId) {
                                        window.__android_active_utterance_id = null;
                                    }
                                    if (window.speechSynthesis && Object.keys(window.__android_tts_utterances).length === 0) {
                                        window.speechSynthesis.speaking = false;
                                        window.speechSynthesis.paused = false;
                                    }
                                    if (typeof utt.onend === 'function') {
                                        utt.onend({ type: 'end', utterance: utt });
                                    }
                                }
                            } else if (event === 'onerror') {
                                if (utt) {
                                    delete window.__android_tts_utterances[utteranceId];
                                    if (window.__android_active_utterance_id === utteranceId) {
                                        window.__android_active_utterance_id = null;
                                    }
                                    if (window.speechSynthesis && Object.keys(window.__android_tts_utterances).length === 0) {
                                        window.speechSynthesis.speaking = false;
                                        window.speechSynthesis.paused = false;
                                    }
                                    if (typeof utt.onerror === 'function') {
                                        utt.onerror({ type: 'error', error: 'native_tts_error', utterance: utt });
                                    }
                                }
                            }
                        } catch(cbErr) {
                            console.error("[TTS Callback Error]", cbErr);
                        }
                    };

                    // 4. Complete SpeechSynthesis Mock / Wrapper
                    const synth = {
                        speaking: false,
                        paused: false,
                        pending: false,
                        onvoiceschanged: null,
                        getVoices: function() {
                            updateVoicesFromBridge();
                            return cachedVoices;
                        },
                        speak: function(utterance) {
                            try {
                                if (!utterance) return;
                                const text = (typeof utterance === 'string') ? utterance : (utterance.text || '');
                                if (!text || text.trim().length === 0) {
                                    if (utterance && typeof utterance.onend === 'function') {
                                        setTimeout(() => utterance.onend({ type: 'end', utterance: utterance }), 10);
                                    }
                                    return;
                                }

                                const id = "utt_" + (++uttCounter);
                                window.__android_tts_utterances[id] = utterance;
                                window.__android_active_utterance_id = id;

                                // Keep history of up to 500 lines for infinite previous rewinds
                                if (!window.__android_tts_history) window.__android_tts_history = [];
                                window.__android_tts_history.push({ id: id, utterance: utterance, text: text });
                                if (window.__android_tts_history.length > 500) {
                                    window.__android_tts_history.shift();
                                }

                                if (utterance.rate && utterance.rate !== 1.0 && window.AndroidTtsBridge && window.AndroidTtsBridge.setRate) {
                                    window.AndroidTtsBridge.setRate(utterance.rate);
                                }
                                if (utterance.pitch && utterance.pitch !== 1.0 && window.AndroidTtsBridge && window.AndroidTtsBridge.setPitch) {
                                    window.AndroidTtsBridge.setPitch(utterance.pitch);
                                }
                                if (utterance.voice && utterance.voice.name && window.AndroidTtsBridge && window.AndroidTtsBridge.setVoice) {
                                    window.AndroidTtsBridge.setVoice(utterance.voice.name);
                                }

                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.speakFromWeb === 'function') {
                                    window.AndroidTtsBridge.speakFromWeb(text, document.title || "อ่านนิยาย", id);
                                } else if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.speak === 'function') {
                                    window.AndroidTtsBridge.speak(text, document.title || "อ่านนิยาย");
                                }
                            } catch(err) {
                                console.error("[Bridge speak error]", err);
                                if (utterance && typeof utterance.onerror === 'function') utterance.onerror({ error: err });
                            }
                        },
                        cancel: function() {
                            try {
                                window.__android_tts_utterances = {};
                                window.__android_active_utterance_id = null;
                                synth.speaking = false;
                                synth.paused = false;
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.stopFromWeb === 'function') {
                                    window.AndroidTtsBridge.stopFromWeb();
                                }
                            } catch(e) {}
                        },
                        pause: function() {
                            try {
                                synth.paused = true;
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.pauseFromWeb === 'function') {
                                    window.AndroidTtsBridge.pauseFromWeb();
                                }
                            } catch(e) {}
                        },
                        resume: function() {
                            try {
                                synth.paused = false;
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.resume === 'function') {
                                    window.AndroidTtsBridge.resume();
                                }
                            } catch(e) {}
                        }
                    };

                    // Overwrite window.speechSynthesis
                    Object.defineProperty(window, 'speechSynthesis', {
                        value: synth,
                        writable: true,
                        configurable: true
                    });

                    // Dispatch onvoiceschanged
                    function triggerVoicesChanged() {
                        try {
                            updateVoicesFromBridge();
                            if (window.speechSynthesis && typeof window.speechSynthesis.onvoiceschanged === 'function') {
                                window.speechSynthesis.onvoiceschanged();
                            }
                            window.dispatchEvent(new Event('voiceschanged'));
                        } catch(e) {}
                    }

                    window.__android_tts_sync_ready = function() {
                        triggerVoicesChanged();
                    };

                    triggerVoicesChanged();
                    setTimeout(triggerVoicesChanged, 50);
                    setTimeout(triggerVoicesChanged, 300);

                    // 5. HTML5 Audio & Online TTS Engines (Google / Microsoft) Interception & Tracking
                    function getAudioEngineName(audio) {
                        try {
                            const src = (audio && audio.src) ? audio.src.toLowerCase() : '';
                            if (src.includes('google') || src.includes('translate_tts')) return 'Google TTS';
                            if (src.includes('microsoft') || src.includes('edge') || src.includes('azure') || src.includes('speech.platform')) return 'Microsoft TTS';
                            const activeEngineEl = document.querySelector('.engine-select .active, [data-engine].active, #tts-engine option:checked, select[name*="engine"] option:checked, .voice-engine .selected, .tts-type .active');
                            if (activeEngineEl) {
                                const txt = (activeEngineEl.textContent || activeEngineEl.value || '').trim();
                                if (txt) return txt;
                            }
                        } catch(e) {}
                        return 'Google / Microsoft TTS';
                    }

                    function getNovelReadingText() {
                        try {
                            const activePara = document.querySelector('.reading, .tts-reading, .active-sentence, .highlight-reading, .highlight, [data-reading="true"], .current-read, .reading-active');
                            if (activePara) {
                                const t = (activePara.innerText || activePara.textContent || '').trim();
                                if (t) return t.substring(0, 100);
                            }
                            if (navigator.mediaSession && navigator.mediaSession.metadata && navigator.mediaSession.metadata.title) {
                                return navigator.mediaSession.metadata.title;
                            }
                        } catch(e) {}
                        return (document.title || 'อ่านนิยาย').substring(0, 80);
                    }

                    let webAudioDebounceTimer = null;

                    function notifyAudioPlaying(audio) {
                        try {
                            window.__active_html5_audio = audio;
                            if (webAudioDebounceTimer) clearTimeout(webAudioDebounceTimer);
                            webAudioDebounceTimer = setTimeout(function() {
                                try {
                                    if (!audio || audio.paused) return;
                                    const engine = getAudioEngineName(audio);
                                    const title = (document.querySelector('h1, .chapter-title, #chapter-title, .title') || {}).innerText || document.title || "อ่านนิยายเว็บ";
                                    const text = getNovelReadingText();
                                    if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.onWebAudioStarted === 'function') {
                                        window.AndroidTtsBridge.onWebAudioStarted(title.trim(), text, engine);
                                    }
                                } catch(e) {}
                            }, 50);
                        } catch(e) {}
                    }

                    function notifyAudioPaused(audio) {
                        try {
                            if (window.speechSynthesis && window.speechSynthesis.speaking) return;
                            setTimeout(function() {
                                try {
                                    if (window.__active_html5_audio && !window.__active_html5_audio.paused) return;
                                    let anyPlaying = false;
                                    document.querySelectorAll('audio').forEach(function(a) {
                                        if (!a.paused) anyPlaying = true;
                                    });
                                    if (anyPlaying) return;
                                    // Give 3.5s grace period between chunked audio clips
                                    setTimeout(function() {
                                        let stillPlaying = false;
                                        document.querySelectorAll('audio').forEach(function(a) {
                                            if (!a.paused) stillPlaying = true;
                                        });
                                        if (stillPlaying) return;
                                        if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.onWebAudioPaused === 'function') {
                                            window.AndroidTtsBridge.onWebAudioPaused();
                                        }
                                    }, 3500);
                                } catch(e) {}
                            }, 500);
                        } catch(e) {}
                    }

                    function notifyAudioEnded() {
                        try {
                            if (window.speechSynthesis && window.speechSynthesis.speaking) return;
                            setTimeout(function() {
                                try {
                                    if (window.__active_html5_audio && !window.__active_html5_audio.paused) return;
                                    let anyPlaying = false;
                                    document.querySelectorAll('audio').forEach(function(a) {
                                        if (!a.paused) anyPlaying = true;
                                    });
                                    if (anyPlaying) return;
                                    // Grace period of 4.5s for online TTS to load next chunk/sentence
                                    setTimeout(function() {
                                        let stillPlaying = false;
                                        document.querySelectorAll('audio').forEach(function(a) {
                                            if (!a.paused) stillPlaying = true;
                                        });
                                        if (stillPlaying) return;
                                        if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.onWebAudioEnded === 'function') {
                                            window.AndroidTtsBridge.onWebAudioEnded();
                                        }
                                    }, 4500);
                                } catch(e) {}
                            }, 500);
                        } catch(e) {}
                    }

                    // Safe, non-intrusive event capture on window
                    try {
                        window.addEventListener('play', function(e) {
                            const target = e.target;
                            if (target && (target.tagName === 'AUDIO' || target instanceof HTMLMediaElement)) {
                                notifyAudioPlaying(target);
                            }
                        }, true);

                        window.addEventListener('pause', function(e) {
                            const target = e.target;
                            if (target && (target.tagName === 'AUDIO' || target instanceof HTMLMediaElement)) {
                                notifyAudioPaused(target);
                            }
                        }, true);

                        window.addEventListener('ended', function(e) {
                            const target = e.target;
                            if (target && (target.tagName === 'AUDIO' || target instanceof HTMLMediaElement)) {
                                notifyAudioEnded();
                            }
                        }, true);
                    } catch(e) {}

                    window.__mediaSessionHandlers = window.__mediaSessionHandlers || {};
                    try {
                        if (navigator.mediaSession) {
                            const origSetActionHandler = navigator.mediaSession.setActionHandler.bind(navigator.mediaSession);
                            navigator.mediaSession.setActionHandler = function(action, handler) {
                                window.__mediaSessionHandlers[action] = handler;
                                return origSetActionHandler(action, handler);
                            };
                        }
                    } catch(e) {}

                    function simulateFullClick(el) {
                        if (!el) return false;
                        try {
                            el.focus();
                            ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(evtType => {
                                const evt = new MouseEvent(evtType, {
                                    bubbles: true,
                                    cancelable: true,
                                    view: window,
                                    buttons: 1
                                });
                                el.dispatchEvent(evt);
                            });
                            if (typeof el.click === 'function') {
                                el.click();
                            }
                            // If element is a link with href, navigate explicitly if default click didn't change location
                            if (el.tagName === 'A' && el.href && !el.href.startsWith('javascript:void') && !el.href.startsWith('#')) {
                                setTimeout(function() {
                                    if (location.href !== el.href) {
                                        window.location.href = el.href;
                                    }
                                }, 50);
                            }
                            return true;
                        } catch(e) {
                            try {
                                if (typeof el.click === 'function') el.click();
                                if (el.tagName === 'A' && el.href) window.location.href = el.href;
                                return true;
                            } catch(err) {}
                        }
                        return false;
                    }

                    // 6. Unified Notification Control Actions for all engines (Device, Google, Microsoft)
                    window.__android_tts_resume = function() {
                        try {
                            if (window.__mediaSessionHandlers && typeof window.__mediaSessionHandlers['play'] === 'function') {
                                try { window.__mediaSessionHandlers['play'](); } catch(e){}
                            }
                            if (window.__active_html5_audio && window.__active_html5_audio.paused) {
                                window.__active_html5_audio.play().catch(() => {});
                            }
                            document.querySelectorAll('audio').forEach(a => {
                                if (a.paused && a.src) a.play().catch(() => {});
                            });
                            if (window.speechSynthesis) {
                                window.speechSynthesis.paused = false;
                                window.speechSynthesis.speaking = true;
                            }
                            if (window.__android_active_utterance_id) {
                                const utt = (window.__android_tts_utterances || {})[window.__android_active_utterance_id];
                                if (utt && typeof utt.onresume === 'function') {
                                    utt.onresume({ type: 'resume', utterance: utt });
                                }
                            }
                            const playBtn = document.querySelector('.tts-play, .btn-play, [data-action="play"], #play-button, .reader-play, .audio-play, .play-btn, .btn-read-play, [aria-label*="Play" i], [title*="เล่น" i], [title*="Play" i], .fa-play');
                            if (playBtn) { simulateFullClick(playBtn); }
                            return true;
                        } catch(e) {
                            console.error("Resume error", e);
                            return false;
                        }
                    };

                    window.__android_tts_pause = function() {
                        try {
                            if (window.__mediaSessionHandlers && typeof window.__mediaSessionHandlers['pause'] === 'function') {
                                try { window.__mediaSessionHandlers['pause'](); } catch(e){}
                            }
                            if (window.__active_html5_audio && !window.__active_html5_audio.paused) {
                                window.__active_html5_audio.pause();
                            }
                            document.querySelectorAll('audio').forEach(a => {
                                if (!a.paused) a.pause();
                            });
                            if (window.speechSynthesis) {
                                window.speechSynthesis.paused = true;
                                window.speechSynthesis.speaking = false;
                            }
                            if (window.__android_active_utterance_id) {
                                const utt = (window.__android_tts_utterances || {})[window.__android_active_utterance_id];
                                if (utt && typeof utt.onpause === 'function') {
                                    utt.onpause({ type: 'pause', utterance: utt });
                                }
                            }
                            const pauseBtn = document.querySelector('.tts-pause, [data-action="pause"], #pause-button, .reader-pause, .audio-pause, .pause-btn, [aria-label*="Pause" i], [title*="หยุด" i]');
                            if (pauseBtn) { simulateFullClick(pauseBtn); }
                            return true;
                        } catch(e) {
                            console.error("Pause error", e);
                            return false;
                        }
                    };

                    window.__android_tts_next = function() {
                        try {
                            try { sessionStorage.setItem('__novel_auto_play_next', 'true'); } catch(e){}
                            if (window.__mediaSessionHandlers && typeof window.__mediaSessionHandlers['nexttrack'] === 'function') {
                                try { window.__mediaSessionHandlers['nexttrack'](); return 'mediasession'; } catch(e){}
                            }

                            // 1. Check direct JS methods on window
                            if (window.reader && typeof window.reader.next === 'function') {
                                try { window.reader.next(); return 'reader_next'; } catch(e){}
                            }
                            if (window.player && typeof window.player.next === 'function') {
                                try { window.player.next(); return 'player_next'; } catch(e){}
                            }

                            // 2. Next paragraph/line/sentence controls on web readers (Right side buttons / floating toolbar)
                            const nextParaSelectors = [
                                '.tts-next', '.btn-next-para', '[data-action="next-para"]', '.reader-next',
                                '.next-sentence', '.next-para', '[title*="ถัดไป" i]', '[title*="หน้า" i]',
                                '[aria-label*="Next" i]', '[aria-label*="ถัดไป" i]', '.btn-next-sentence',
                                '.btn-next-line', '#next-sentence-btn', '.audio-next', '.fa-step-forward',
                                'button:has(.fa-step-forward)', 'a:has(.fa-step-forward)', '.novel-next-para',
                                '[data-action="next"]', '.tts-btn-next', '#tts-next', '#btn-next', '.btn-next',
                                '[data-cmd*="next" i]', '[data-role*="next" i]', '[class*="next-line" i]',
                                '[class*="next-sentence" i]', '[class*="btn-next" i]'
                            ];
                            for (let s of nextParaSelectors) {
                                let el = document.querySelector(s);
                                if (el && el.offsetParent !== null) {
                                    simulateFullClick(el);
                                    return 'clicked_next_para_' + s;
                                }
                            }
                            for (let s of nextParaSelectors) {
                                let el = document.querySelector(s);
                                if (el) {
                                    simulateFullClick(el);
                                    return 'clicked_next_para_' + s;
                                }
                            }

                            // 3. If speech utterance active, fast forward
                            if (window.__android_active_utterance_id) {
                                const currUtt = (window.__android_tts_utterances || {})[window.__android_active_utterance_id];
                                if (currUtt && typeof currUtt.onend === 'function') {
                                    delete window.__android_tts_utterances[window.__android_active_utterance_id];
                                    window.__android_active_utterance_id = null;
                                    currUtt.onend({ type: 'end', utterance: currUtt });
                                    return 'advanced_speech_utterance';
                                }
                            }

                            if (typeof window.__novel_next_chapter === 'function') {
                                try { window.__novel_next_chapter(); return 'custom_chapter'; } catch(e){}
                            }

                            const chapterSelectors = [
                                '#next_url', '.next_page', '#next-chapter', '.next-chapter', '.btn-next',
                                'a[rel="next"]', 'button.next', 'a.next', 'a.nextChapter', '.chapter-next a',
                                '#nextLink', '.nav-next a', 'a:has(.fa-chevron-right)', 'a:has(.fa-arrow-right)'
                            ];
                            for (let cs of chapterSelectors) {
                                let cEl = document.querySelector(cs);
                                if (cEl && cEl.offsetParent !== null) { simulateFullClick(cEl); return 'clicked_chapter_' + cs; }
                            }
                            for (let cs of chapterSelectors) {
                                let cEl = document.querySelector(cs);
                                if (cEl) { simulateFullClick(cEl); return 'clicked_chapter_' + cs; }
                            }
                            const xpathList = [
                                "//a[contains(text(), 'ตอนต่อไป') or contains(text(), 'บทถัดไป') or contains(text(), 'ถัดไป') or contains(text(), 'ตอนหน้า')]",
                                "//a[contains(text(), '下一章') or contains(text(), '下一页') or contains(text(), 'Next Chapter') or contains(text(), 'Next')]",
                                "//button[contains(text(), 'ตอนต่อไป') or contains(text(), 'บทถัดไป') or contains(text(), 'ถัดไป') or contains(text(), 'Next')]"
                            ];
                            for (let xp of xpathList) {
                                let res = document.evaluate(xp, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                                if (res) { simulateFullClick(res); return 'clicked_xpath'; }
                            }
                            window.scrollBy({ top: window.innerHeight * 0.75, behavior: 'smooth' });
                            return 'scrolled';
                        } catch(e) {
                            console.error("Next error", e);
                            return false;
                        }
                    };

                    window.__android_tts_prev = function() {
                        try {
                            try { sessionStorage.setItem('__novel_auto_play_next', 'true'); } catch(e){}

                            // 1. Check MediaSession previoustrack handler first
                            if (window.__mediaSessionHandlers && typeof window.__mediaSessionHandlers['previoustrack'] === 'function') {
                                try { window.__mediaSessionHandlers['previoustrack'](); return 'mediasession'; } catch(e){}
                            }

                            // 2. Check direct JS methods on window
                            if (window.reader && typeof window.reader.prev === 'function') {
                                try { window.reader.prev(); return 'reader_prev'; } catch(e){}
                            }
                            if (window.player && typeof window.player.prev === 'function') {
                                try { window.player.prev(); return 'player_prev'; } catch(e){}
                            }
                            if (typeof window.readPrev === 'function') {
                                try { window.readPrev(); return 'readPrev'; } catch(e){}
                            }

                            // 3. Click previous paragraph/sentence controls on web novel readers (Floating right panel / toolbar)
                            const prevParaSelectors = [
                                '.tts-prev', '.btn-prev-para', '[data-action="prev-para"]', '.reader-prev',
                                '.prev-sentence', '.prev-para', '[title*="ย้อน" i]', '[title*="ก่อน" i]',
                                '[aria-label*="ย้อน" i]', '[aria-label*="ก่อน" i]', '[aria-label*="Previous" i]',
                                '.btn-prev-sentence', '.btn-prev-speech', '#btn-prev-tts', '.tts-backward',
                                '[data-action="prev-sentence"]', '.btn-prev-line', '#prev-sentence-btn',
                                '.audio-prev', '.fa-step-backward', '.fa-backward',
                                'button:has(.fa-step-backward)', 'a:has(.fa-step-backward)',
                                'button:has(.fa-backward)', 'a:has(.fa-backward)',
                                '.novel-prev-para', '[data-action="prev"]', '.tts-btn-prev', '#tts-prev',
                                '#btn-prev', '.btn-prev', '[data-cmd*="prev" i]', '[data-role*="prev" i]',
                                '[class*="prev-line" i]', '[class*="prev-sentence" i]', '[class*="btn-prev" i]'
                            ];
                            for (let s of prevParaSelectors) {
                                let el = document.querySelector(s);
                                if (el && el.offsetParent !== null) {
                                    simulateFullClick(el);
                                    return 'clicked_prev_para_' + s;
                                }
                            }
                            for (let s of prevParaSelectors) {
                                let el = document.querySelector(s);
                                if (el) {
                                    simulateFullClick(el);
                                    return 'clicked_prev_para_' + s;
                                }
                            }

                            // 4. Web Novel Reader Active Sentence / Element Navigation: Find previous element in DOM
                            const activeSentenceSelectors = [
                                '.reading', '.tts-reading', '.active-sentence', '.highlight-reading',
                                '.highlight', '[data-reading="true"]', '.current-read', '.reading-active',
                                '.active-para', '.speech-highlight', '.speaking', '.tts-active',
                                'span.active', 'p.active', '.text-reading', '.current-sentence'
                            ];
                            let currentReadingEl = null;
                            for (let sel of activeSentenceSelectors) {
                                currentReadingEl = document.querySelector(sel);
                                if (currentReadingEl) break;
                            }

                            if (currentReadingEl) {
                                let prevEl = currentReadingEl.previousElementSibling;
                                while (prevEl && prevEl.tagName !== 'P' && prevEl.tagName !== 'DIV' && prevEl.tagName !== 'SPAN' && !prevEl.classList.contains('sentence') && !prevEl.classList.contains('text-line')) {
                                    prevEl = prevEl.previousElementSibling;
                                }
                                if (!prevEl && currentReadingEl.parentElement) {
                                    let parentPrev = currentReadingEl.parentElement.previousElementSibling;
                                    if (parentPrev) {
                                        prevEl = parentPrev.querySelector('p, .sentence, span, .text-line') || parentPrev;
                                    }
                                }
                                if (prevEl) {
                                    try {
                                        currentReadingEl.classList.remove('reading', 'tts-reading', 'active-sentence', 'highlight', 'current-read', 'reading-active', 'active');
                                        prevEl.classList.add('reading', 'active-sentence');
                                    } catch(e){}
                                    simulateFullClick(prevEl);
                                    prevEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                    return 'clicked_prev_dom_element';
                                }
                            }

                            // 5. If playing HTML5 Audio, rewind 10s or restart current audio clip
                            if (window.__active_html5_audio) {
                                try {
                                    if (window.__active_html5_audio.currentTime > 3) {
                                        window.__active_html5_audio.currentTime = 0;
                                        return 'restarted_audio';
                                    } else {
                                        window.__active_html5_audio.currentTime = Math.max(0, window.__active_html5_audio.currentTime - 10);
                                        return 'rewound_audio';
                                    }
                                } catch(e){}
                            }

                            // 6. Custom chapter hook
                            if (typeof window.__novel_prev_chapter === 'function') {
                                try { window.__novel_prev_chapter(); return 'custom_chapter'; } catch(e){}
                            }

                            // 7. Click previous chapter link / button
                            const chapterSelectors = [
                                '#prev_url', '.prev_page', '#prev-chapter', '.prev-chapter', '.btn-prev',
                                'a[rel="prev"]', 'button.prev', 'a.prev', 'a.prevChapter', '.chapter-prev a',
                                '#prevLink', '.nav-prev a', '#btn-prev-chapter', '.btn-prev-chap', '.read-prev',
                                'a:has(.fa-chevron-left)', 'a:has(.fa-arrow-left)', 'a:has(.fa-angle-left)',
                                '.nav-previous a', '.previous-chapter a', '.chapter-nav-prev'
                            ];
                            for (let cs of chapterSelectors) {
                                let cEl = document.querySelector(cs);
                                if (cEl && cEl.offsetParent !== null) { simulateFullClick(cEl); return 'clicked_chapter_' + cs; }
                            }
                            for (let cs of chapterSelectors) {
                                let cEl = document.querySelector(cs);
                                if (cEl) { simulateFullClick(cEl); return 'clicked_chapter_' + cs; }
                            }

                            // 8. XPath fallback for Thai / English previous chapter buttons
                            const xpathList = [
                                "//a[contains(text(), 'ตอนก่อนหน้า') or contains(text(), 'บทก่อนหน้า') or contains(text(), 'ก่อนหน้า') or contains(text(), 'ตอนที่แล้ว')]",
                                "//a[contains(text(), '上一章') or contains(text(), '上一页') or contains(text(), 'Previous Chapter') or contains(text(), 'Prev')]",
                                "//button[contains(text(), 'ตอนก่อนหน้า') or contains(text(), 'บทก่อนหน้า') or contains(text(), 'ก่อนหน้า') or contains(text(), 'ตอนที่แล้ว') or contains(text(), 'Prev')]"
                            ];
                            for (let xp of xpathList) {
                                let res = document.evaluate(xp, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                                if (res) { simulateFullClick(res); return 'clicked_xpath'; }
                            }
                            window.scrollBy({ top: -window.innerHeight * 0.75, behavior: 'smooth' });
                            return 'scrolled';
                        } catch(e) {
                            console.error("Prev error", e);
                            return false;
                        }
                    };

                    // 7. Expose convenient novel reader helper functions
                    window.readWithAndroidTts = function(text, title) {
                        if (window.AndroidTtsBridge) {
                            window.AndroidTtsBridge.speak(text, title || document.title);
                        }
                    };

                    window.readNovelPlaylist = function(paragraphs, title, startIndex) {
                        if (!window.AndroidTtsBridge) return;
                        if (Array.isArray(paragraphs)) {
                            window.AndroidTtsBridge.speakParagraphs(JSON.stringify(paragraphs), title || document.title, startIndex || 0);
                        } else {
                            window.AndroidTtsBridge.speak(paragraphs, title || document.title);
                        }
                    };

                    // 6. Universal Client-Side Download Interceptor (.txt / Blob / Data URLs)
                    try {
                        document.addEventListener('click', function(e) {
                            var target = e.target;
                            while (target && target !== document.body && target.tagName !== 'A') {
                                target = target.parentElement;
                            }
                            if (target && target.tagName === 'A') {
                                var href = target.href || '';
                                var downloadAttr = target.getAttribute('download');
                                if (downloadAttr !== null || href.startsWith('blob:') || href.startsWith('data:')) {
                                    if (href.startsWith('blob:') || href.startsWith('data:')) {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        var fname = downloadAttr || ('novel_' + Date.now() + '.txt');
                                        fetch(href)
                                            .then(function(res) { return res.blob(); })
                                            .then(function(blob) {
                                                var reader = new FileReader();
                                                reader.onloadend = function() {
                                                    if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.saveBase64File === 'function') {
                                                        window.AndroidTtsBridge.saveBase64File(reader.result, blob.type || 'text/plain', fname);
                                                    }
                                                };
                                                reader.readAsDataURL(blob);
                                            })
                                            .catch(function(err) {
                                                console.error("[Download Blob Error]", err);
                                            });
                                    }
                                }
                            }
                        }, true);

                        if (typeof window.saveAs === 'undefined') {
                            window.saveAs = function(blob, filename) {
                                var reader = new FileReader();
                                reader.onloadend = function() {
                                    if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.saveBase64File === 'function') {
                                        window.AndroidTtsBridge.saveBase64File(reader.result, (blob && blob.type) || 'text/plain', filename || ('novel_' + Date.now() + '.txt'));
                                    }
                                };
                                reader.readAsDataURL(blob);
                            };
                        }
                    } catch(dlErr) {
                        console.error("[Download Interceptor Error]", dlErr);
                    }

                    console.log("[NovelAI Android Bridge] Bidirectional Web Speech & Download Polyfill successfully installed.");
                } catch(globalErr) {
                    console.error("[NovelAI Bridge Fatal]", globalErr);
                }
            })();
        """

        /**
         * Google Chrome-style Webpage Translation Injection Engine
         * Embeds Google Translate Element cleanly, strips unwanted banners/styles,
         * translates the entire DOM in-place to Thai/target language so reading and TTS work seamlessly.
         */
        const val TRANSLATE_INJECTION_SCRIPT = """
            (function() {
                if (window.__chrome_translate_installed) return;
                window.__chrome_translate_installed = true;

                try {
                    // 1. Clean up Google styles to hide iframe / banners
                    var style = document.createElement('style');
                    style.id = 'chrome-translate-style';
                    style.innerHTML = `
                        .goog-te-banner-frame { display: none !important; }
                        .goog-te-banner-frame.skiptranslate { display: none !important; }
                        .goog-te-gadget { display: none !important; font-size: 0px !important; }
                        .goog-te-gadget span { display: none !important; }
                        .goog-tooltip { display: none !important; }
                        .goog-tooltip:hover { display: none !important; }
                        .goog-text-highlight { background-color: transparent !important; box-shadow: none !important; }
                        body { top: 0px !important; position: static !important; }
                        #goog-gt-tt { display: none !important; }
                        #google_translate_element { display: none !important; }
                        .skiptranslate iframe { display: none !important; }
                    `;
                    (document.head || document.documentElement).appendChild(style);

                    // 2. Prepare hidden container
                    function ensureTranslateElement() {
                        if (!document.getElementById('google_translate_element')) {
                            var div = document.createElement('div');
                            div.id = 'google_translate_element';
                            div.style.display = 'none';
                            (document.body || document.documentElement).appendChild(div);
                        }
                    }

                    // Snapshot pristine original page state
                    function captureOriginalSnapshot() {
                        try {
                            if (!window.__original_html_snapshot || window.__snapshot_url !== location.href) {
                                if (document.body && document.body.innerHTML && document.body.innerHTML.length > 50 &&
                                    !document.documentElement.classList.contains('translated-ltr') &&
                                    !document.documentElement.classList.contains('translated-rtl')) {
                                    window.__original_html_snapshot = document.body.innerHTML;
                                    window.__snapshot_url = location.href;
                                }
                            }
                        } catch(e){}
                    }

                    // 3. Init Google translate callback
                    window.googleTranslateElementInit = function() {
                        try {
                            new google.translate.TranslateElement({
                                pageLanguage: 'auto',
                                autoDisplay: false,
                                multilanguagePage: true
                            }, 'google_translate_element');
                        } catch(e) {
                            console.error('Google Translate Init Error', e);
                        }
                    };

                    // 4. Load Google Translate SDK immediately in background
                    function loadGoogleTranslateScript() {
                        ensureTranslateElement();
                        if (!document.getElementById('google-translate-script')) {
                            var script = document.createElement('script');
                            script.id = 'google-translate-script';
                            script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                            script.async = true;
                            (document.head || document.documentElement).appendChild(script);
                        }
                    }

                    window.__chrome_translate_interval = null;

                    // 5. Trigger Translation
                    window.__chrome_translate_to = function(targetLang) {
                        try {
                            targetLang = targetLang || 'th';
                            captureOriginalSnapshot();
                            ensureTranslateElement();
                            loadGoogleTranslateScript();

                            if (window.__chrome_translate_interval) {
                                clearInterval(window.__chrome_translate_interval);
                                window.__chrome_translate_interval = null;
                            }

                            if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                window.AndroidTtsBridge.onTranslationStatus('translating', targetLang);
                            }

                            // Set googtrans cookie immediately
                            var host = location.hostname;
                            document.cookie = "googtrans=/auto/" + targetLang + "; path=/;";
                            document.cookie = "googtrans=/auto/" + targetLang + "; path=/; domain=" + host;
                            document.cookie = "googtrans=/auto/" + targetLang + "; path=/; domain=." + host;

                            function triggerSelect(select) {
                                if (select) {
                                    select.value = targetLang;
                                    select.dispatchEvent(new Event('change', { bubbles: true }));
                                    select.dispatchEvent(new Event('input', { bubbles: true }));
                                    if (typeof select.onchange === 'function') {
                                        select.onchange();
                                    }
                                }
                            }

                            var select = document.querySelector('.goog-te-combo');
                            if (select) {
                                triggerSelect(select);
                                if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                    window.AndroidTtsBridge.onTranslationStatus('translated', targetLang);
                                }
                                return;
                            }

                            // Keep checking up to 70 attempts (10.5 seconds) for the dropdown to render
                            var attempts = 0;
                            window.__chrome_translate_interval = setInterval(function() {
                                attempts++;
                                var sel = document.querySelector('.goog-te-combo');
                                if (sel) {
                                    clearInterval(window.__chrome_translate_interval);
                                    window.__chrome_translate_interval = null;
                                    triggerSelect(sel);
                                    if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                        window.AndroidTtsBridge.onTranslationStatus('translated', targetLang);
                                    }
                                } else if (attempts >= 70) {
                                    clearInterval(window.__chrome_translate_interval);
                                    window.__chrome_translate_interval = null;
                                    if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                        window.AndroidTtsBridge.onTranslationStatus('translated', targetLang);
                                    }
                                }
                            }, 150);
                        } catch(e) {
                            console.error("Translate error", e);
                            if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                window.AndroidTtsBridge.onTranslationStatus('error', e.message);
                            }
                        }
                    };

                    // 6. Restore Original (Zero-reload instantaneous DOM rollback & disable Google Translate engine)
                    window.__chrome_translate_restore = function() {
                        try {
                            if (window.__chrome_translate_interval) {
                                clearInterval(window.__chrome_translate_interval);
                                window.__chrome_translate_interval = null;
                            }

                            // 1. Delete all googtrans cookies across all domains and paths
                            var host = location.hostname;
                            var domains = [host, '.' + host, ''];
                            var parts = host.split('.');
                            while (parts.length > 1) {
                                domains.push('.' + parts.join('.'));
                                parts.shift();
                            }
                            var paths = ['/', location.pathname, ''];
                            domains.forEach(function(d) {
                                paths.forEach(function(p) {
                                    document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC;' + (d ? ' domain=' + d + ';' : '') + (p ? ' path=' + p + ';' : '');
                                });
                            });

                            // 2. Clear Google Translate Script & Observers to prevent re-translation loop
                            try {
                                var script = document.getElementById('google-translate-script');
                                if (script && script.parentNode) { script.parentNode.removeChild(script); }
                                var elem = document.getElementById('google_translate_element');
                                if (elem && elem.parentNode) { elem.parentNode.removeChild(elem); }
                                var gtTt = document.getElementById('goog-gt-tt');
                                if (gtTt && gtTt.parentNode) { gtTt.parentNode.removeChild(gtTt); }
                                var iframes = document.querySelectorAll('.goog-te-banner-frame, .skiptranslate');
                                iframes.forEach(function(f) { if (f && f.parentNode) f.parentNode.removeChild(f); });
                                if (window.google && window.google.translate) {
                                    delete window.google.translate;
                                }
                            } catch(e) {}

                            // 3. In-place instantaneous DOM restoration from pristine snapshot
                            if (window.__original_html_snapshot && window.__snapshot_url === location.href) {
                                var currentY = window.scrollY || window.pageYOffset || (document.documentElement ? document.documentElement.scrollTop : 0) || 0;

                                document.body.innerHTML = window.__original_html_snapshot;

                                document.documentElement.classList.remove('translated-ltr', 'translated-rtl');
                                document.body.classList.remove('translated-ltr', 'translated-rtl');
                                document.body.style.top = '0px';
                                document.body.style.position = '';

                                window.scrollTo(0, currentY);
                                setTimeout(function() {
                                    window.scrollTo(0, currentY);
                                }, 30);

                                if (window.__android_tts_sync_ready) {
                                    window.__android_tts_sync_ready();
                                }

                                if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                    window.AndroidTtsBridge.onTranslationStatus('original', '');
                                }
                                return true;
                            }

                            // 4. Fallback if snapshot wasn't available: reset combo and classes
                            var select = document.querySelector('.goog-te-combo');
                            if (select) {
                                var origOption = select.querySelector('option[value=""]') || select.options[0];
                                if (origOption) {
                                    select.value = origOption.value;
                                    select.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            }
                            document.documentElement.classList.remove('translated-ltr', 'translated-rtl');
                            document.body.classList.remove('translated-ltr', 'translated-rtl');

                            if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                window.AndroidTtsBridge.onTranslationStatus('original', '');
                            }
                            return false;
                        } catch(e) {
                            console.error("Restore error", e);
                            return false;
                        }
                    };

                    // Pre-capture original snapshot and preload Google Translate script in background
                    captureOriginalSnapshot();
                    loadGoogleTranslateScript();
                } catch(err) {
                    console.error("Translate engine setup error", err);
                }
            })();
        """
    }
}

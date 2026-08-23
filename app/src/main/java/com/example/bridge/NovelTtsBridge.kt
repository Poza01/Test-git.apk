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
                    const selectors = [
                        '#next_url', '.next_page', '#next-chapter', '.next-chapter', '.btn-next',
                        'a[rel="next"]', 'button.next', 'a.next'
                    ];
                    for (let s of selectors) {
                        let el = document.querySelector(s);
                        if (el) { el.click(); return 'clicked_' + s; }
                    }
                    const xpathList = [
                        "//a[contains(text(), 'ตอนต่อไป') or contains(text(), 'บทถัดไป') or contains(text(), 'ถัดไป')]",
                        "//a[contains(text(), '下一章') or contains(text(), '下一页') or contains(text(), 'Next Chapter') or contains(text(), 'Next')]",
                        "//button[contains(text(), 'ตอนต่อไป') or contains(text(), 'บทถัดไป') or contains(text(), 'ถัดไป') or contains(text(), 'Next')]"
                    ];
                    for (let xp of xpathList) {
                        let res = document.evaluate(xp, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                        if (res) { res.click(); return 'clicked_xpath'; }
                    }
                    window.scrollBy({ top: window.innerHeight * 0.75, behavior: 'smooth' });
                    return 'scrolled';
                })();
            """.trimIndent(), null)
        }
    }

    fun onPrevFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    const selectors = [
                        '#prev_url', '.prev_page', '#prev-chapter', '.prev-chapter', '.btn-prev',
                        'a[rel="prev"]', 'button.prev', 'a.prev'
                    ];
                    for (let s of selectors) {
                        let el = document.querySelector(s);
                        if (el) { el.click(); return 'clicked_' + s; }
                    }
                    const xpathList = [
                        "//a[contains(text(), 'ตอนก่อนหน้า') or contains(text(), 'บทก่อนหน้า') or contains(text(), 'ก่อนหน้า')]",
                        "//a[contains(text(), '上一章') or contains(text(), '上一页') or contains(text(), 'Previous Chapter') or contains(text(), 'Prev')]",
                        "//button[contains(text(), 'ตอนก่อนหน้า') or contains(text(), 'บทก่อนหน้า') or contains(text(), 'ก่อนหน้า') or contains(text(), 'Prev')]"
                    ];
                    for (let xp of xpathList) {
                        let res = document.evaluate(xp, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                        if (res) { res.click(); return 'clicked_xpath'; }
                    }
                    window.scrollBy({ top: -window.innerHeight * 0.75, behavior: 'smooth' });
                    return 'scrolled';
                })();
            """.trimIndent(), null)
        }
    }

    fun onResumeFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    if (window.speechSynthesis) {
                        window.speechSynthesis.paused = false;
                        window.speechSynthesis.speaking = true;
                    }
                    const playBtn = document.querySelector('.tts-play, .btn-play, [data-action="play"], #play-button');
                    if (playBtn) { playBtn.click(); }
                })();
            """.trimIndent(), null)
        }
    }

    fun onPauseFromNotification() {
        mainHandler.post {
            getWebView()?.evaluateJavascript("""
                (function() {
                    if (window.speechSynthesis) {
                        window.speechSynthesis.paused = true;
                        window.speechSynthesis.speaking = false;
                    }
                    const pauseBtn = document.querySelector('.tts-pause, .btn-pause, [data-action="pause"], #pause-button');
                    if (pauseBtn) { pauseBtn.click(); }
                })();
            """.trimIndent(), null)
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
            val js = """
                (function() {
                    if (typeof window.__chrome_translate_restore === 'function') {
                        window.__chrome_translate_restore();
                    }
                })();
            """.trimIndent()
            getWebView()?.evaluateJavascript(js, null)
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

                    // 3. Utterance Tracking Map for callbacks
                    window.__android_tts_utterances = window.__android_tts_utterances || {};
                    let uttCounter = 0;

                    // Global Android -> JS Event Dispatcher
                    window.__android_tts_callback = function(event, utteranceId) {
                        try {
                            const utt = window.__android_tts_utterances[utteranceId];
                            if (event === 'onstart') {
                                if (window.speechSynthesis) {
                                    window.speechSynthesis.speaking = true;
                                    window.speechSynthesis.paused = false;
                                }
                                if (utt && typeof utt.onstart === 'function') {
                                    utt.onstart({ type: 'start', utterance: utt });
                                }
                            } else if (event === 'ondone') {
                                if (window.speechSynthesis) {
                                    window.speechSynthesis.speaking = false;
                                    window.speechSynthesis.paused = false;
                                }
                                if (utt) {
                                    delete window.__android_tts_utterances[utteranceId];
                                    if (typeof utt.onend === 'function') {
                                        utt.onend({ type: 'end', utterance: utt });
                                    }
                                }
                            } else if (event === 'onerror') {
                                if (window.speechSynthesis) {
                                    window.speechSynthesis.speaking = false;
                                    window.speechSynthesis.paused = false;
                                }
                                if (utt) {
                                    delete window.__android_tts_utterances[utteranceId];
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
                                synth.speaking = false;
                                synth.paused = false;
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.stop === 'function') {
                                    window.AndroidTtsBridge.stop();
                                }
                            } catch(e) {}
                        },
                        pause: function() {
                            try {
                                synth.paused = true;
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.pause === 'function') {
                                    window.AndroidTtsBridge.pause();
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

                    // 5. Expose convenient novel reader helper functions
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

                    // 4. Load Google Translate SDK on demand
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
                            ensureTranslateElement();
                            loadGoogleTranslateScript();

                            if (window.__chrome_translate_interval) {
                                clearInterval(window.__chrome_translate_interval);
                                window.__chrome_translate_interval = null;
                            }

                            if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                window.AndroidTtsBridge.onTranslationStatus('translating', targetLang);
                            }

                            var attempts = 0;
                            window.__chrome_translate_interval = setInterval(function() {
                                attempts++;
                                var select = document.querySelector('.goog-te-combo');
                                if (select) {
                                    clearInterval(window.__chrome_translate_interval);
                                    window.__chrome_translate_interval = null;
                                    if (select.value !== targetLang) {
                                        select.value = targetLang;
                                        select.dispatchEvent(new Event('change'));
                                    }
                                    if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                        window.AndroidTtsBridge.onTranslationStatus('translated', targetLang);
                                    }
                                } else if (attempts > 20) {
                                    clearInterval(window.__chrome_translate_interval);
                                    window.__chrome_translate_interval = null;
                                    // Fallback: Cookie method
                                    document.cookie = "googtrans=/auto/" + targetLang + "; path=/; domain=" + location.hostname;
                                    document.cookie = "googtrans=/auto/" + targetLang + "; path=/;";
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

                    // 6. Restore Original
                    window.__chrome_translate_restore = function() {
                        try {
                            if (window.__chrome_translate_interval) {
                                clearInterval(window.__chrome_translate_interval);
                                window.__chrome_translate_interval = null;
                            }
                            var select = document.querySelector('.goog-te-combo');
                            if (select) {
                                var origOption = select.querySelector('option[value=""]') || select.options[0];
                                if (origOption) {
                                    select.value = origOption.value;
                                    select.dispatchEvent(new Event('change'));
                                }
                            }
                            document.cookie = "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=" + location.hostname;
                            document.cookie = "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";

                            var iframe = document.querySelector('.goog-te-banner-frame');
                            if (iframe) {
                                try {
                                    var innerDoc = iframe.contentDocument || iframe.contentWindow.document;
                                    var restoreBtn = innerDoc.querySelector('.goog-te-button button');
                                    if (restoreBtn) restoreBtn.click();
                                } catch(e){}
                            }

                            if (window.AndroidTtsBridge && window.AndroidTtsBridge.onTranslationStatus) {
                                window.AndroidTtsBridge.onTranslationStatus('original', '');
                            }
                        } catch(e) {
                            console.error("Restore error", e);
                        }
                    };

                    // Only prepare environment, script loads when user presses Translate
                    ensureTranslateElement();
                } catch(err) {
                    console.error("Translate engine setup error", err);
                }
            })();
        """
    }
}

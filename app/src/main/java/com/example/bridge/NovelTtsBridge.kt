package com.example.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.service.TtsForegroundService
import org.json.JSONArray
import org.json.JSONObject

class NovelTtsBridge(
    private val context: Context,
    private val getService: () -> TtsForegroundService?,
    private val getWebView: () -> WebView?
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun speak(text: String?, title: String?) {
        if (text.isNullOrBlank()) return
        mainHandler.post {
            getService()?.playSingleText(text, title ?: "อ่านนิยาย")
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
                getService()?.playPlaylist(list, title ?: "อ่านนิยาย", startIndex)
            }
        } catch (e: Exception) {
            // If it was just a plain string, fallback to speak
            mainHandler.post {
                getService()?.playSingleText(paragraphsJson, title ?: "อ่านนิยาย")
            }
        }
    }

    @JavascriptInterface
    fun pause() {
        mainHandler.post { getService()?.pause() }
    }

    @JavascriptInterface
    fun resume() {
        mainHandler.post { getService()?.resume() }
    }

    @JavascriptInterface
    fun stop() {
        mainHandler.post { getService()?.stop() }
    }

    @JavascriptInterface
    fun setRate(rate: Float) {
        mainHandler.post { getService()?.setSpeechRate(rate) }
    }

    @JavascriptInterface
    fun setPitch(pitch: Float) {
        mainHandler.post { getService()?.setSpeechPitch(pitch) }
    }

    @JavascriptInterface
    fun setVoice(voiceName: String?) {
        if (voiceName.isNullOrBlank()) return
        mainHandler.post { getService()?.setVoice(voiceName) }
    }

    @JavascriptInterface
    fun getAvailableVoicesJson(): String {
        val service = getService() ?: return "[]"
        val voices = service.playbackState.value.availableVoices
        val jsonArray = JSONArray()
        for (v in voices) {
            val obj = JSONObject().apply {
                put("name", v.name)
                put("locale", v.locale)
                put("quality", v.quality)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun isAppBridgeActive(): Boolean = true

    companion object {
        const val JS_INTERFACE_NAME = "AndroidTtsBridge"

        /**
         * Comprehensive Web Speech API (SpeechSynthesis & SpeechSynthesisUtterance) Polyfill Script
         * Injected into the WebView to provide full standard getVoices(), speak(), pause(), cancel(),
         * and redirect all reading requests to the robust Android Native Foreground Service.
         */
        const val INJECTION_SCRIPT = """
            (function() {
                try {
                    console.log("[NovelAI Android Bridge] Initializing Full Web Speech & TTS Bridge");

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

                    // 2. Ensure SpeechSynthesisUtterance exists
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

                    // 3. Complete SpeechSynthesis Mock / Wrapper
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
                                const text = utterance ? (utterance.text || utterance) : '';
                                if (!text || text.trim().length === 0) return;
                                
                                if (utterance && utterance.rate) {
                                    if (window.AndroidTtsBridge && window.AndroidTtsBridge.setRate) {
                                        window.AndroidTtsBridge.setRate(utterance.rate);
                                    }
                                }
                                if (utterance && utterance.pitch) {
                                    if (window.AndroidTtsBridge && window.AndroidTtsBridge.setPitch) {
                                        window.AndroidTtsBridge.setPitch(utterance.pitch);
                                    }
                                }
                                if (utterance && utterance.voice && utterance.voice.name) {
                                    if (window.AndroidTtsBridge && window.AndroidTtsBridge.setVoice) {
                                        window.AndroidTtsBridge.setVoice(utterance.voice.name);
                                    }
                                }

                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.speak === 'function') {
                                    window.AndroidTtsBridge.speak(text, document.title || "อ่านนิยาย");
                                    if (utterance && typeof utterance.onstart === 'function') utterance.onstart({ type: 'start' });
                                }
                            } catch(err) {
                                console.error("[Bridge speak error]", err);
                                if (utterance && typeof utterance.onerror === 'function') utterance.onerror({ error: err });
                            }
                        },
                        cancel: function() {
                            try {
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.stop === 'function') {
                                    window.AndroidTtsBridge.stop();
                                }
                            } catch(e) {}
                        },
                        pause: function() {
                            try {
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.pause === 'function') {
                                    window.AndroidTtsBridge.pause();
                                }
                            } catch(e) {}
                        },
                        resume: function() {
                            try {
                                if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.resume === 'function') {
                                    window.AndroidTtsBridge.resume();
                                }
                            } catch(e) {}
                        }
                    };

                    // Overwrite or create window.speechSynthesis
                    if (!window.speechSynthesis || typeof window.speechSynthesis.getVoices !== 'function') {
                        Object.defineProperty(window, 'speechSynthesis', {
                            value: synth,
                            writable: true,
                            configurable: true
                        });
                    } else {
                        // Patch existing methods to guarantee bridge delegation
                        window.speechSynthesis.getVoices = synth.getVoices;
                        const origSpeak = window.speechSynthesis.speak.bind(window.speechSynthesis);
                        window.speechSynthesis.speak = function(utterance) {
                            if (window.AndroidTtsBridge) {
                                synth.speak(utterance);
                            } else {
                                origSpeak(utterance);
                            }
                        };
                        window.speechSynthesis.cancel = synth.cancel;
                        window.speechSynthesis.pause = synth.pause;
                        window.speechSynthesis.resume = synth.resume;
                    }

                    // Dispatch onvoiceschanged event for web novel reader scripts
                    setTimeout(function() {
                        try {
                            if (window.speechSynthesis && typeof window.speechSynthesis.onvoiceschanged === 'function') {
                                window.speechSynthesis.onvoiceschanged();
                            }
                            window.dispatchEvent(new Event('voiceschanged'));
                        } catch(e) {}
                    }, 100);

                    // 4. Expose convenient novel reader helper functions
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

                    console.log("[NovelAI Android Bridge] Web Speech Polyfill successfully installed.");
                } catch(globalErr) {
                    console.error("[NovelAI Bridge Fatal]", globalErr);
                }
            })();
        """
    }
}

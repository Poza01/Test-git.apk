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
         * Smart Polyfill Script injected into the WebView to redirect any web speech synthesis
         * or custom audio requests straight to Android Foreground Service!
         */
        const val INJECTION_SCRIPT = """
            (function() {
                if (window.__android_tts_bridge_injected__) return;
                window.__android_tts_bridge_injected__ = true;
                console.log("[NovelAI Android Bridge] Native TTS Bridge Initialized");

                // Monkey-patch window.speechSynthesis to route to Android Native Engine
                if (window.AndroidTtsBridge) {
                    const originalSpeak = window.speechSynthesis ? window.speechSynthesis.speak.bind(window.speechSynthesis) : null;
                    
                    window.speechSynthesis = window.speechSynthesis || {};
                    window.speechSynthesis.speak = function(utterance) {
                        try {
                            if (utterance && utterance.text) {
                                window.AndroidTtsBridge.speak(utterance.text, document.title || "อ่านนิยาย");
                                return;
                            }
                        } catch(e) {
                            console.error("[Bridge Error]", e);
                        }
                        if (originalSpeak) originalSpeak(utterance);
                    };

                    window.speechSynthesis.cancel = function() {
                        try {
                            window.AndroidTtsBridge.stop();
                        } catch(e) {}
                    };

                    window.speechSynthesis.pause = function() {
                        try {
                            window.AndroidTtsBridge.pause();
                        } catch(e) {}
                    };

                    window.speechSynthesis.resume = function() {
                        try {
                            window.AndroidTtsBridge.resume();
                        } catch(e) {}
                    };
                    
                    // Expose convenient global helper for web novel reader
                    window.readWithAndroidTts = function(text, title) {
                        window.AndroidTtsBridge.speak(text, title || document.title);
                    };

                    window.readNovelPlaylist = function(paragraphs, title, startIndex) {
                        if (Array.isArray(paragraphs)) {
                            window.AndroidTtsBridge.speakParagraphs(JSON.stringify(paragraphs), title || document.title, startIndex || 0);
                        } else {
                            window.AndroidTtsBridge.speak(paragraphs, title || document.title);
                        }
                    };
                }
            })();
        """
    }
}

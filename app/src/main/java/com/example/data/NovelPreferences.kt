package com.example.data

import android.content.Context
import android.content.SharedPreferences

class NovelPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("novel_ai_tts_prefs", Context.MODE_PRIVATE)

    var webUrl: String
        get() = prefs.getString("key_web_url", "https://plaaniyaythai-ai.ai.studio/") ?: "https://plaaniyaythai-ai.ai.studio/"
        set(value) = prefs.edit().putString("key_web_url", value).apply()

    var defaultRate: Float
        get() = prefs.getFloat("key_default_rate", 1.0f)
        set(value) = prefs.edit().putFloat("key_default_rate", value).apply()

    var defaultPitch: Float
        get() = prefs.getFloat("key_default_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("key_default_pitch", value).apply()

    var selectedVoice: String?
        get() = prefs.getString("key_selected_voice", null)
        set(value) = prefs.edit().putString("key_selected_voice", value).apply()

    var lastSavedText: String
        get() = prefs.getString("key_last_saved_text", "") ?: ""
        set(value) = prefs.edit().putString("key_last_saved_text", value).apply()

    var isHyperOsGuideDismissed: Boolean
        get() = prefs.getBoolean("key_hyperos_dismissed", false)
        set(value) = prefs.edit().putBoolean("key_hyperos_dismissed", value).apply()
}

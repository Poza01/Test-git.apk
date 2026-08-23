package com.example.service

data class VoiceInfo(
    val name: String,
    val locale: String,
    val isNetworkConnectionRequired: Boolean = false,
    val quality: String = "Normal"
)

data class TtsPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val chapterTitle: String = "ไม่ได้เล่น",
    val activeParagraphIndex: Int = -1,
    val totalParagraphs: Int = 0,
    val currentText: String = "",
    val paragraphs: List<String> = emptyList(),
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val selectedVoiceName: String? = null,
    val availableVoices: List<VoiceInfo> = emptyList(),
    val engineName: String = "Android Text-To-Speech",
    val isInitialized: Boolean = false
)

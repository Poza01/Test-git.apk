package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TtsForegroundService
import com.example.service.VoiceInfo
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun VoiceSettingsScreen(
    service: TtsForegroundService?,
    modifier: Modifier = Modifier
) {
    val state = service?.playbackState?.collectAsState()?.value
    var testSentence by remember {
        mutableStateOf("หลินเฟิงก้าวออกจากตำหนักโบราณ พลังปราณฟ้าดินพลันหมุนวนรอบกายอย่างรุนแรง")
    }

    val speedPresets = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp)
            .testTag("voice_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "เครื่องอ่านเสียง",
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ระบบเสียงในเครื่อง (Native TTS)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = state?.engineName ?: "Android TextToSpeech Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "แอปเชื่อมต่อกับเอนจินเสียงของ Android โดยตรง รองรับเสียงไทยทุกรูปแบบ ทำงานต่อเนื่องเบื้องหลังไม่ดับเมื่อสลับแอป",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Test Voice Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ทดสอบเสียงอ่าน",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testSentence,
                        onValueChange = { testSentence = it },
                        label = { Text("ข้อความทดสอบเสียง", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_sentence_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF121212),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AmberPrimary, AmberSecondary)
                                )
                            )
                            .clickable {
                                service?.playSingleText(testSentence, "ทดสอบเสียงอ่านภาษาไทย")
                            }
                            .padding(vertical = 12.dp)
                            .testTag("test_voice_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "ทดสอบเสียง",
                                tint = Color(0xFF0F0F0F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "กดฟังตัวอย่างเสียง",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F0F0F)
                            )
                        }
                    }
                }
            }
        }

        // Speech Rate & Pitch Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Speed slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "ความเร็ว",
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ความเร็วในการอ่าน (Speed)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${String.format("%.2f", state?.speechRate ?: 1.0f)}x",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary
                        )
                    }

                    Slider(
                        value = state?.speechRate ?: 1.0f,
                        onValueChange = { service?.setSpeechRate(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPrimary,
                            activeTrackColor = AmberPrimary,
                            inactiveTrackColor = Color(0xFF333333)
                        ),
                        modifier = Modifier.testTag("rate_slider")
                    )

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        speedPresets.forEach { speed ->
                            val isSelected = Math.abs((state?.speechRate ?: 1.0f) - speed) < 0.05f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AmberPrimary else DarkSurfaceVariant)
                                    .border(1.dp, if (isSelected) AmberPrimary else DarkBorder, RoundedCornerShape(10.dp))
                                    .clickable { service?.setSpeechRate(speed) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F0F0F) else Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pitch slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "ระดับเสียง",
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ระดับเสียงทุ้ม/แหลม (Pitch)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${String.format("%.2f", state?.speechPitch ?: 1.0f)}x",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary
                        )
                    }

                    Slider(
                        value = state?.speechPitch ?: 1.0f,
                        onValueChange = { service?.setSpeechPitch(it) },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPrimary,
                            activeTrackColor = AmberPrimary,
                            inactiveTrackColor = Color(0xFF333333)
                        ),
                        modifier = Modifier.testTag("pitch_slider")
                    )
                }
            }
        }

        // Available Device Voices Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "เสียงทั้งหมด",
                    tint = AmberPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "รายการเสียงในเครื่อง (${state?.availableVoices?.size ?: 0} เสียง)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Voices List
        if (state?.availableVoices.isNullOrEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "กำลังค้นหาเสียงในเครื่อง หรือใช้เอนจินมาตรฐานของระบบ...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(state!!.availableVoices) { voice ->
                val isSelected = voice.name == state.selectedVoiceName
                val isThai = voice.locale.contains("ไทย", ignoreCase = true) || voice.locale.contains("th", ignoreCase = true)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { service?.setVoice(voice.name) }
                        .testTag("voice_item_${voice.name}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF261D0A) else DarkSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) AmberPrimary else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = voice.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AmberPrimary else Color.White
                                )
                                if (isThai) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ภาษาไทย",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${voice.locale} • ${voice.quality}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "เลือกอยู่",
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NovelPreferences
import com.example.service.TtsForegroundService
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface

@Composable
fun DirectReaderScreen(
    service: TtsForegroundService?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { NovelPreferences(context) }

    var rawInputText by remember { mutableStateOf(prefs.lastSavedText.ifBlank { SAMPLE_WUXIA_TEXT }) }
    var chapterTitle by remember { mutableStateOf("ตอนที่ 1: การกำเนิดใหม่แห่งมหาจักรพรรดิ") }
    var parsedParagraphs by remember(rawInputText) {
        mutableStateOf(rawInputText.split("\n\n", "\n").filter { it.isNotBlank() })
    }

    val state = service?.playbackState?.collectAsState()?.value
    val listState = rememberLazyListState()

    // Auto-scroll to active paragraph
    LaunchedEffect(state?.activeParagraphIndex) {
        state?.activeParagraphIndex?.let { index ->
            if (index >= 0 && index < parsedParagraphs.size) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp)
            .testTag("direct_reader_screen")
    ) {
        // Header & Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "เครื่องอ่านนิยาย",
                                tint = AmberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "อ่านนิยายและข้อความด่วน",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Sample presets
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222))
                                .clickable {
                                    chapterTitle = "ตอนที่ 1: การกำเนิดใหม่แห่งมหาจักรพรรดิ"
                                    rawInputText = SAMPLE_WUXIA_TEXT
                                    prefs.lastSavedText = SAMPLE_WUXIA_TEXT
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("กำลังภายใน", fontSize = 10.sp, color = AmberPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222))
                                .clickable {
                                    chapterTitle = "ตอนที่ 1: ตื่นขึ้นมาในต่างโลกพร้อมสกิลระดับ SSS"
                                    rawInputText = SAMPLE_ISEKAI_TEXT
                                    prefs.lastSavedText = SAMPLE_ISEKAI_TEXT
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ต่างโลก", fontSize = 10.sp, color = Color(0xFF06B6D4))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = {
                        rawInputText = it
                        prefs.lastSavedText = it
                    },
                    label = { Text("วางเนื้อหานิยายภาษาไทยที่นี่", fontSize = 12.sp) },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("novel_text_input"),
                    shape = RoundedCornerShape(12.dp),
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

                // Start reading button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AmberPrimary, AmberSecondary)
                            )
                        )
                        .clickable {
                            if (parsedParagraphs.isNotEmpty()) {
                                service?.playPlaylist(parsedParagraphs, chapterTitle, 0)
                            }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("start_reading_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "เริ่มอ่านเสียง",
                            tint = Color(0xFF0F0F0F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "เริ่มอ่านออกเสียงทั้งหมด (${parsedParagraphs.size} ย่อหน้า)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0F0F)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Paragraphs interactive list
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ย่อหน้าเนื้อหา (แตะเพื่อเริ่มอ่านจากย่อหน้านั้น)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp
            )
            Text(
                text = "รวม ${parsedParagraphs.size} ย่อหน้า",
                style = MaterialTheme.typography.labelSmall,
                color = AmberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(parsedParagraphs) { index, paragraph ->
                val isReadingThis = state?.isPlaying == true && state.activeParagraphIndex == index
                val isSelected = state?.activeParagraphIndex == index

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            service?.playPlaylist(parsedParagraphs, chapterTitle, index)
                        }
                        .testTag("paragraph_item_$index"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isReadingThis) Color(0xFF2B2005) else DarkSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isReadingThis) AmberPrimary else if (isSelected) AmberSecondary.copy(alpha = 0.5f) else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isReadingThis) AmberPrimary else Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isReadingThis) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "กำลังอ่าน",
                                    tint = Color(0xFF0F0F0F),
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isReadingThis) Color.White else Color(0xFFD1D5DB),
                            lineHeight = 22.sp,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

private const val SAMPLE_WUXIA_TEXT = """หลินเฟิงลืมตาขึ้นช้าๆ ท่ามกลางหมอกควันสีครามที่ลอยอบอวลอยู่รอบกาย

เขาสัมผัสได้ถึงพลังปราณปฐพีอันบริสุทธิ์ที่กำลังไหลเวียนเข้าสู่จุดตันเถียนอย่างบ้าคลั่ง

หลังจากผ่านการบำเพ็ญเพียรในถ้ำมังกรโบราณมานานถึงสามร้อยปี ในที่สุดเขาก็บรรลุสู่ขอบเขตเซียนสวรรค์ขั้นสูงสุด

"ข้ากลับมาแล้ว..." หลินเฟิงพึมพำกับตนเองด้วยสายตาที่เปี่ยมไปด้วยประกายแห่งความเด็ดเดี่ยว

ทันใดนั้น เสียงคำรามของอสูรฟ้าสวรรค์ก็ดังกึกก้องไปทั่วทั้งหุบเขา ดินแดนเบื้องล่างกำลังสั่นสะเทือนเพื่อต้อนรับการกลับมาของมหาจักรพรรดิ"""

private const val SAMPLE_ISEKAI_TEXT = """เมื่อลืมตาขึ้นมาอีกครั้ง สิ่งที่เห็นเบื้องหน้ากลับไม่ใช่เพดานห้องนอนเดิม แต่เป็นท้องฟ้าสีม่วงครามที่ทอดยาวสุดสายตา

หน้าต่างสถานะโปร่งใสสีฟ้าปรากฏขึ้นตรงหน้าพร้อมเสียงแจ้งเตือนของระบบ

[ยินดีต้อนรับผู้กล้าเข้าสู่โลกแห่งแอสเทอเรีย คุณได้รับสกิลระดับ SSS: การกลืนกินมหาธาตุ]

"นี่ฉันถูกอัญเชิญมาต่างโลกจริงๆ เหรอเนี่ย?!" ชินยะอุทานด้วยความตกตะลึง

ขณะที่เขากำลังสำรวจพลังใหม่ มอนสเตอร์หมาป่าเพลิงทมิฬก็กระโจนออกมาจากพุ่มไม้เบื้องหน้าทันที"""

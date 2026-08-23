package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TtsForegroundService
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess

@Composable
fun HyperOsGuideScreen(
    service: TtsForegroundService?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val openAppSettings = {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp)
            .testTag("hyperos_guide_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1605)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "HyperOS",
                                tint = AmberPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "คู่มือตั้งค่า Xiaomi HyperOS 2",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "ป้องกันระบบสั่งปิดเสียงเมื่อสลับไปเล่น Facebook / ดับจอ",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "HyperOS 2 มีระบบจัดการแบตเตอรี่ที่ตัดแอปเบื้องหลังเร็วมาก เพียงตั้งค่าตาม 3 ขั้นตอนนี้ครั้งเดียว เสียงอ่านภาษาไทยจะเล่นต่อเนื่องไม่ดับ 100%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1D5DB),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Open App Info Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AmberPrimary, AmberSecondary)
                                )
                            )
                            .clickable { openAppSettings() }
                            .padding(vertical = 12.dp)
                            .testTag("open_app_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "เปิดการตั้งค่า",
                                tint = Color(0xFF0F0F0F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "เปิดหน้าการตั้งค่าแอปทันที",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F0F0F)
                            )
                        }
                    }
                }
            }
        }

        // Step 1: Battery Saver -> No Restrictions
        item {
            HyperOsStepCard(
                stepNumber = "1",
                title = "การประหยัดแบตเตอรี่ (Battery Saver)",
                instruction = "ในหน้าข้อมูลแอป ให้แตะ 'การประหยัดแบตเตอรี่' แล้วเปลี่ยนเป็น 'ไม่จำกัด (No restrictions)'",
                icon = Icons.Default.BatteryAlert,
                accentColor = EmeraldSuccess
            )
        }

        // Step 2: Autostart
        item {
            HyperOsStepCard(
                stepNumber = "2",
                title = "เริ่มทำงานอัตโนมัติ (Autostart)",
                instruction = "เปิดสวิตช์ 'เริ่มทำงานอัตโนมัติ' และ 'การเริ่มทำงานอัตโนมัติเบื้องหลัง' เพื่อให้ Foreground Service ทำงานไม่สะดุด",
                icon = Icons.Default.PowerSettingsNew,
                accentColor = AmberPrimary
            )
        }

        // Step 3: Lock App in Recent Apps
        item {
            HyperOsStepCard(
                stepNumber = "3",
                title = "ล็อกแอปในหน้า Recent Apps (Lock App)",
                instruction = "รูดเปิดหน้าสลับแอป (Recent Apps) แตะค้างที่ตัวแอป NovelAI TTS แล้วกดรูป 'แม่กุญแจ 🔒' เพื่อไม่ให้ระบบล้างแอปทิ้ง",
                icon = Icons.Default.Lock,
                accentColor = Color(0xFF06B6D4)
            )
        }

        // Step 4: Notification Permission
        item {
            HyperOsStepCard(
                stepNumber = "4",
                title = "การแจ้งเตือน (Notifications)",
                instruction = "อนุญาตการแจ้งเตือน เพื่อให้มีแถบเล่นเสียง (Play / Pause / Next) บนหน้าจอล็อกและแถบด้านบน",
                icon = Icons.Default.NotificationsActive,
                accentColor = Color(0xFFA855F7)
            )
        }

        // Test Background Speech
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ทดสอบการอ่านเสียงและสลับหน้าจอ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "กดปุ่มด้านล่างเพื่อเริ่มอ่านเสียงยาว 5 ย่อหน้า จากนั้นลองสลับไปเปิด Facebook หรือกดปุ่มดับหน้าจอล็อกเครื่องเพื่อทดสอบว่าเสียงยังเล่นต่อเนื่อง",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val testParagraphs = listOf(
                        "ย่อหน้าที่หนึ่ง: ระบบ Foreground Service ของ Android เริ่มทำงานอย่างสมบูรณ์แบบ",
                        "ย่อหน้าที่สอง: ตอนนี้คุณสามารถสลับไปเล่น Facebook ตอบแชทไลน์ หรือเล่นเกมอื่นได้เลย",
                        "ย่อหน้าที่สาม: แม้ว่าคุณจะกดปุ่มดับหน้าจอล็อกเครื่องใส่กระเป๋า เสียงอ่านภาษาไทยก็จะยังคงอ่านต่ออย่างราบรื่น",
                        "ย่อหน้าที่สี่: คุณสามารถใช้ปุ่มบนหูฟังบลูทูธ หรือแถบแจ้งเตือนด้านบนในการกดหยุดหรือข้ามย่อหน้าได้",
                        "ย่อหน้าที่ห้า: การทดสอบระบบเสียงเบื้องหลังบน HyperOS 2 เสร็จสมบูรณ์ ขอให้เพลิดเพลินกับการฟังนิยายครับ"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF222222))
                            .border(1.dp, AmberPrimary, RoundedCornerShape(14.dp))
                            .clickable {
                                service?.playPlaylist(testParagraphs, "ทดสอบเสียงเบื้องหลัง HyperOS 2", 0)
                            }
                            .padding(vertical = 12.dp)
                            .testTag("test_background_speech_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "ทดสอบ",
                                tint = AmberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "เริ่มทดสอบอ่านเสียงเบื้องหลัง (5 ย่อหน้า)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
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

@Composable
private fun HyperOsStepCard(
    stepNumber: String,
    title: String,
    instruction: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

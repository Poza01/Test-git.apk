package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess

data class TranslateLanguage(val code: String, val name: String, val flag: String)

val SupportedLanguages = listOf(
    TranslateLanguage("th", "ไทย (Thai)", "🇹🇭"),
    TranslateLanguage("en", "อังกฤษ (English)", "🇺🇸"),
    TranslateLanguage("zh-CN", "จีนตัวย่อ (Simplified Chinese)", "🇨🇳"),
    TranslateLanguage("zh-TW", "จีนตัวเต็ม (Traditional Chinese)", "🇹🇼"),
    TranslateLanguage("ja", "ญี่ปุ่น (Japanese)", "🇯🇵"),
    TranslateLanguage("ko", "เกาหลี (Korean)", "🇰🇷"),
    TranslateLanguage("vi", "เวียดนาม (Vietnamese)", "🇻🇳"),
    TranslateLanguage("fr", "ฝรั่งเศส (French)", "🇫🇷"),
    TranslateLanguage("de", "เยอรมัน (German)", "🇩🇪"),
    TranslateLanguage("es", "สเปน (Spanish)", "🇪🇸"),
    TranslateLanguage("ru", "รัสเซีย (Russian)", "🇷🇺")
)

@Composable
fun ChromeTranslateBar(
    isVisible: Boolean,
    isTranslated: Boolean,
    isTranslating: Boolean,
    targetLang: String,
    isAutoTranslate: Boolean,
    onTranslateTo: (String) -> Unit,
    onRestoreOriginal: () -> Unit,
    onToggleAutoTranslate: (Boolean) -> Unit,
    onCloseBar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLangPicker by remember { mutableStateOf(false) }

    val currentLang = SupportedLanguages.find { it.code == targetLang } ?: SupportedLanguages.first()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .testTag("chrome_translate_bar"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF18181B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E36))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Google Translate Brand Icon + Language Switcher Tabs
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Google Translate Brand Icon
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Google Translate",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Segmented control tabs: [ Original ] | [ Target (e.g. ไทย) ]
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF121214))
                                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "ต้นฉบับ" (Original) Tab
                            val isOriginalActive = !isTranslated && !isTranslating
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isOriginalActive) Color(0xFF27272A) else Color.Transparent
                                    )
                                    .clickable { onRestoreOriginal() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("tab_translate_original"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ต้นฉบับ",
                                    fontSize = 12.sp,
                                    fontWeight = if (isOriginalActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOriginalActive) Color.White else Color(0xFF9CA3AF)
                                )
                            }

                            // Target Language Tab (e.g. "ไทย 🇹🇭")
                            val isTargetActive = isTranslated || isTranslating
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isTargetActive) {
                                            Modifier.background(
                                                Brush.horizontalGradient(listOf(AmberPrimary, AmberSecondary))
                                            )
                                        } else {
                                            Modifier.background(Color.Transparent)
                                        }
                                    )
                                    .clickable { onTranslateTo(targetLang) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("tab_translate_target"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isTranslating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color(0xFF0F0F0F),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else if (isTranslated) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "แปลแล้ว",
                                            tint = Color(0xFF0F0F0F),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = currentLang.name.substringBefore(" "),
                                        fontSize = 12.sp,
                                        fontWeight = if (isTargetActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTargetActive) Color(0xFF0F0F0F) else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }

                    // Right: Actions (Language picker / Options menu / Close button)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Language Picker / More menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(30.dp).testTag("translate_options_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "ตัวเลือกการแปล",
                                    tint = Color(0xFFD1D5DB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoFixHigh,
                                                contentDescription = null,
                                                tint = if (isAutoTranslate) AmberPrimary else Color(0xFF9CA3AF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "แปลหน้านี้อัตโนมัติเสมอ",
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Switch(
                                                checked = isAutoTranslate,
                                                onCheckedChange = {
                                                    onToggleAutoTranslate(it)
                                                    showMenu = false
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = AmberPrimary,
                                                    checkedTrackColor = AmberPrimary.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    },
                                    onClick = {
                                        onToggleAutoTranslate(!isAutoTranslate)
                                        showMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "เลือกภาษาเป้าหมาย...",
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        showLangPicker = true
                                    }
                                )
                            }

                            // Language Selector Dialog / Submenu
                            DropdownMenu(
                                expanded = showLangPicker,
                                onDismissRequest = { showLangPicker = false },
                                modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder)
                            ) {
                                SupportedLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = lang.flag, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = lang.name,
                                                    color = if (lang.code == targetLang) AmberPrimary else Color.White,
                                                    fontWeight = if (lang.code == targetLang) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            showLangPicker = false
                                            onTranslateTo(lang.code)
                                        }
                                    )
                                }
                            }
                        }

                        // Close bar button (X)
                        IconButton(
                            onClick = onCloseBar,
                            modifier = Modifier.size(30.dp).testTag("translate_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ปิดแถบแปลภาษา",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

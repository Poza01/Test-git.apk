package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bridge.NovelTtsBridge
import com.example.data.NovelPreferences
import com.example.service.TtsForegroundService
import com.example.ui.components.ChromeTranslateBar
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebCompanionScreen(
    service: TtsForegroundService?,
    isBottomNavVisible: Boolean = false,
    onToggleBottomNav: () -> Unit = {},
    onNavigateToBackgroundSettings: () -> Unit = {},
    onNavigateToVoiceSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { NovelPreferences(context) }

    var currentUrl by remember { mutableStateOf(prefs.webUrl) }
    var inputUrl by remember { mutableStateOf(prefs.webUrl) }
    var pageTitle by remember { mutableStateOf("กำลังโหลดหน้าเว็บนิยาย...") }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var isTranslateBarVisible by remember { mutableStateOf(prefs.showTranslateBar) }
    var isTranslated by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var targetLang by remember { mutableStateOf(prefs.targetLanguage) }
    var isAutoTranslate by remember { mutableStateOf(prefs.isAutoTranslate) }

    var showChromeMenu by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val currentService by rememberUpdatedState(service)
    val bridge = remember {
        NovelTtsBridge(
            context = context,
            getService = { currentService ?: TtsForegroundService.instance },
            getWebView = { webViewInstance }
        )
    }

    // Safety timeout: Reset translating spinner after 6 seconds if not received callback
    LaunchedEffect(isTranslating) {
        if (isTranslating) {
            delay(6000)
            if (isTranslating) {
                isTranslating = false
            }
        }
    }

    LaunchedEffect(service, webViewInstance) {
        bridge.attachServiceListener()
        webViewInstance?.evaluateJavascript("""
            (function() {
                if (window.__android_tts_sync_ready) {
                    window.__android_tts_sync_ready();
                }
            })();
        """.trimIndent(), null)

        bridge.onTranslationStatusChange = { status, _ ->
            when (status) {
                "translating" -> {
                    isTranslating = true
                }
                "translated" -> {
                    isTranslating = false
                    isTranslated = true
                }
                "original" -> {
                    isTranslating = false
                    isTranslated = false
                }
                "error" -> {
                    isTranslating = false
                }
            }
        }
    }

    // Handle back button for menu dismissal or web navigation history
    BackHandler(enabled = showChromeMenu || canGoBack) {
        if (showChromeMenu) {
            showChromeMenu = false
        } else if (canGoBack) {
            webViewInstance?.goBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .testTag("web_companion_screen")
    ) {
        // Top Omnibox & Web Navigation Bar (Google Chrome Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webViewInstance?.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier.size(36.dp).testTag("web_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "ย้อนกลับ",
                        tint = if (canGoBack) Color.White else Color(0xFF555555),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val homeUrl = "https://plaaniyaythai-ai.ai.studio/"
                        inputUrl = homeUrl
                        currentUrl = homeUrl
                        prefs.webUrl = homeUrl
                        webViewInstance?.loadUrl(homeUrl)
                    },
                    modifier = Modifier.size(36.dp).testTag("web_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "หน้าแรกเว็บนิยาย",
                        tint = AmberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // URL Text Box
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color.White
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "HTTPS",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AmberPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "รีโหลด",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = Color(0xFF121212),
                        unfocusedContainerColor = Color(0xFF121212)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            var target = inputUrl.trim()
                            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                target = "https://$target"
                            }
                            inputUrl = target
                            currentUrl = target
                            prefs.webUrl = target
                            webViewInstance?.loadUrl(target)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("web_url_input")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Chrome 3-Dots Menu Button (⋮)
                Box {
                    IconButton(
                        onClick = { showChromeMenu = !showChromeMenu },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("chrome_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "เมนูเพิ่มเติมแบบ Chrome",
                            tint = if (isTranslated) Color(0xFF38BDF8) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Chrome Dropdown Submenu
                    DropdownMenu(
                        expanded = showChromeMenu,
                        onDismissRequest = { showChromeMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF202124))
                            .border(1.dp, Color(0xFF3C4043), RoundedCornerShape(14.dp))
                            .widthIn(min = 250.dp, max = 300.dp)
                            .testTag("chrome_overflow_menu")
                    ) {
                        // 1. Top Quick Action Bar (Chrome style icon row: Forward, Refresh, Home, External)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    showChromeMenu = false
                                    webViewInstance?.goForward()
                                },
                                enabled = canGoForward,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "ไปข้างหน้า",
                                    tint = if (canGoForward) Color.White else Color(0xFF555555),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    showChromeMenu = false
                                    webViewInstance?.reload()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "รีโหลด",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    showChromeMenu = false
                                    val homeUrl = "https://plaaniyaythai-ai.ai.studio/"
                                    inputUrl = homeUrl
                                    currentUrl = homeUrl
                                    prefs.webUrl = homeUrl
                                    webViewInstance?.loadUrl(homeUrl)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "หน้าแรกนิยาย",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    showChromeMenu = false
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ไม่สามารถเปิดเบราว์เซอร์ได้", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "เปิดในเบราว์เซอร์",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF3C4043), thickness = 1.dp)

                        // 2. แปลภาษา (Google Translate) - Chrome "Translate..."
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isTranslating) "กำลังแปลภาษา..." else if (isTranslated) "แสดงต้นฉบับ (คืนค่า)" else "แปลภาษา (Google Translate)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isTranslated) Color(0xFF38BDF8) else Color.White
                                    )
                                    Text(
                                        text = if (isTranslated) "หน้าเว็บแปลเป็นไทยแล้ว (คลิกเพื่อดูต้นฉบับ)" else "แปลเนื้อหาหน้าเว็บเป็นภาษาไทย",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "แปลภาษา",
                                    tint = if (isTranslated) Color(0xFF38BDF8) else AmberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = AmberPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else if (isTranslated) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF065F46))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ไทย ✓",
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showChromeMenu = false
                                if (isTranslated) {
                                    isTranslated = false
                                    isTranslating = false
                                    isAutoTranslate = false
                                    prefs.isAutoTranslate = false
                                    bridge.restoreOriginal()
                                    Toast.makeText(context, "กลับสู่เนื้อหาต้นฉบับแล้ว", Toast.LENGTH_SHORT).show()
                                } else {
                                    isTranslating = true
                                    isTranslateBarVisible = true
                                    prefs.showTranslateBar = true
                                    bridge.translatePage(targetLang)
                                }
                            },
                            modifier = Modifier.testTag("chrome_menu_translate")
                        )

                        // 3. แถบเครื่องมือสลับภาษา (Chrome Translate Bar)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isTranslateBarVisible) "ซ่อนแถบเลือกภาษา" else "แสดงแถบเลือกภาษา",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "เลือกภาษาอื่น (อังกฤษ จีน ญี่ปุ่น ฯลฯ)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "แถบเลือกภาษา",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Switch(
                                    checked = isTranslateBarVisible,
                                    onCheckedChange = { checked ->
                                        isTranslateBarVisible = checked
                                        prefs.showTranslateBar = checked
                                        showChromeMenu = false
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0284C7),
                                        uncheckedThumbColor = Color(0xFF9CA3AF),
                                        uncheckedTrackColor = Color(0xFF3C4043)
                                    ),
                                    modifier = Modifier.size(width = 36.dp, height = 24.dp)
                                )
                            },
                            onClick = {
                                isTranslateBarVisible = !isTranslateBarVisible
                                prefs.showTranslateBar = isTranslateBarVisible
                                showChromeMenu = false
                            }
                        )

                        // 4. แปลหน้านี้อัตโนมัติ (Always Translate)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "แปลหน้านี้อัตโนมัติ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "แปลเป็นไทยทันทีเมื่อเปิดบทใหม่",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "แปลอัตโนมัติ",
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Switch(
                                    checked = isAutoTranslate,
                                    onCheckedChange = { checked ->
                                        isAutoTranslate = checked
                                        prefs.isAutoTranslate = checked
                                        if (checked && !isTranslated && !isTranslating) {
                                            isTranslating = true
                                            bridge.translatePage(targetLang)
                                        }
                                        showChromeMenu = false
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AmberPrimary,
                                        uncheckedThumbColor = Color(0xFF9CA3AF),
                                        uncheckedTrackColor = Color(0xFF3C4043)
                                    ),
                                    modifier = Modifier.size(width = 36.dp, height = 24.dp)
                                )
                            },
                            onClick = {
                                isAutoTranslate = !isAutoTranslate
                                prefs.isAutoTranslate = isAutoTranslate
                                if (isAutoTranslate && !isTranslated && !isTranslating) {
                                    isTranslating = true
                                    bridge.translatePage(targetLang)
                                }
                                showChromeMenu = false
                            }
                        )

                        HorizontalDivider(color = Color(0xFF3C4043), thickness = 1.dp)

                        // 5. แสดง/ซ่อน แถบเมนูด้านล่าง 3 ปุ่ม
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isBottomNavVisible) "ซ่อนแถบ 3 เมนูล่าง" else "แสดงแถบ 3 เมนูล่าง",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "แถบเมนูนำทางด้านล่างจอ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isBottomNavVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                                    contentDescription = "แถบ 3 เมนูล่าง",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Switch(
                                    checked = isBottomNavVisible,
                                    onCheckedChange = {
                                        onToggleBottomNav()
                                        showChromeMenu = false
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AmberPrimary,
                                        uncheckedThumbColor = Color(0xFF9CA3AF),
                                        uncheckedTrackColor = Color(0xFF3C4043)
                                    ),
                                    modifier = Modifier.size(width = 36.dp, height = 24.dp)
                                )
                            },
                            onClick = {
                                onToggleBottomNav()
                                showChromeMenu = false
                            },
                            modifier = Modifier.testTag("chrome_menu_toggle_bottom_nav")
                        )

                        HorizontalDivider(color = Color(0xFF3C4043), thickness = 1.dp)

                        // 6. ตั้งค่าเสียงอ่าน (TTS Voice Settings)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "ตั้งค่าเสียงอ่าน (TTS)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "เลือกเสียง ความเร็ว และระดับเสียง",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "ตั้งค่าเสียง",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showChromeMenu = false
                                onNavigateToVoiceSettings()
                            },
                            modifier = Modifier.testTag("chrome_menu_voice_settings")
                        )

                        // 7. ล็อกแอพทำงานพื้นหลัง (HyperOS / Android Guide)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "วิธีล็อกแอพไม่ให้ดับ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "คู่มือตั้งค่า HyperOS / Xiaomi / Android",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.BatteryAlert,
                                    contentDescription = "ล็อกแอพไม่ให้ดับ",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showChromeMenu = false
                                onNavigateToBackgroundSettings()
                            },
                            modifier = Modifier.testTag("chrome_menu_background_guide")
                        )
                    }
                }
            }

            // Progress bar
            if (isLoading) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { pageProgress },
                    color = AmberPrimary,
                    trackColor = Color(0xFF222222),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }
        }

        // Chrome-style Translation Bar
        ChromeTranslateBar(
            isVisible = isTranslateBarVisible,
            isTranslated = isTranslated,
            isTranslating = isTranslating,
            targetLang = targetLang,
            isAutoTranslate = isAutoTranslate,
            onTranslateTo = { lang ->
                targetLang = lang
                prefs.targetLanguage = lang
                bridge.translatePage(lang)
            },
            onRestoreOriginal = {
                isTranslated = false
                isTranslating = false
                isAutoTranslate = false
                prefs.isAutoTranslate = false
                bridge.restoreOriginal()
                Toast.makeText(context, "กลับสู่เนื้อหาต้นฉบับแล้ว", Toast.LENGTH_SHORT).show()
            },
            onToggleAutoTranslate = { enabled ->
                isAutoTranslate = enabled
                prefs.isAutoTranslate = enabled
                if (enabled) {
                    bridge.translatePage(targetLang)
                } else {
                    isTranslated = false
                    isTranslating = false
                    bridge.restoreOriginal()
                    Toast.makeText(context, "ปิดการแปลอัตโนมัติและกลับต้นฉบับแล้ว", Toast.LENGTH_SHORT).show()
                }
            },
            onCloseBar = {
                isTranslateBarVisible = false
                prefs.showTranslateBar = false
            }
        )

        // Main In-App WebView
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Use hardware rendering for smooth 60fps scrolling and lag-free novel reading
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                        // Enable cookies and 3rd party cookies for SPA/Auth sites
                        val webViewInstanceRef = this
                        android.webkit.CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(webViewInstanceRef, true)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = false
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString NovelAI-AndroidBridge/1.0"
                            // Prevent background timer suspension
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        // Prevent background throttling of timers in WebView
                        resumeTimers()

                        // Attach the Native TTS JavaScript Bridge
                        addJavascriptInterface(bridge, NovelTtsBridge.JS_INTERFACE_NAME)

                        // Native File Download Listener (.txt, .epub, .pdf, attachments, blob/data)
                        // Directly opens the ROM / Android system browser chooser (Chrome, etc.)
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            try {
                                if (url.startsWith("blob:") || url.startsWith("data:")) {
                                    evaluateJavascript("""
                                        (function() {
                                            fetch('$url')
                                                .then(function(r) { return r.blob(); })
                                                .then(function(blob) {
                                                    var reader = new FileReader();
                                                    reader.onloadend = function() {
                                                        if (window.AndroidTtsBridge && typeof window.AndroidTtsBridge.saveBase64File === 'function') {
                                                            window.AndroidTtsBridge.saveBase64File(
                                                                reader.result,
                                                                blob.type || '${mimetype ?: "text/plain"}',
                                                                'novel_${System.currentTimeMillis()}.txt'
                                                            );
                                                        }
                                                    };
                                                    reader.readAsDataURL(blob);
                                                })
                                                .catch(function(err) {
                                                    console.error(err);
                                                });
                                        })();
                                    """.trimIndent(), null)
                                    return@setDownloadListener
                                }

                                // 1. Prompt ROM / System Chooser to open in Google Chrome or User's preferred browser
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                val chooser = Intent.createChooser(browserIntent, "เลือกเบราว์เซอร์สำหรับดาวน์โหลดไฟล์...")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(chooser)
                                Toast.makeText(ctx, "กำลังเปิดเบราว์เซอร์เพื่อดาวน์โหลด...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // Fallback to DownloadManager if no external browser found
                                try {
                                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                                        val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                        val fileName = if (guessedName.endsWith(".bin", ignoreCase = true) && (url.contains(".txt") || mimetype.contains("text"))) {
                                            guessedName.substringBeforeLast(".") + ".txt"
                                        } else {
                                            guessedName
                                        }

                                        val cookies = CookieManager.getInstance().getCookie(url)
                                        if (!cookies.isNullOrBlank()) {
                                            addRequestHeader("Cookie", cookies)
                                        }
                                        if (userAgent.isNotBlank()) {
                                            addRequestHeader("User-Agent", userAgent)
                                        }
                                        addRequestHeader("Referer", currentUrl)

                                        setTitle(fileName)
                                        setDescription("กำลังดาวน์โหลดไฟล์...")
                                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                        if (mimetype.isNotBlank()) {
                                            setMimeType(mimetype)
                                        }
                                    }

                                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                                    dm?.enqueue(request)
                                    Toast.makeText(ctx, "กำลังดาวน์โหลดไฟล์...", Toast.LENGTH_SHORT).show()
                                } catch (e2: Exception) {
                                    e2.printStackTrace()
                                    Toast.makeText(ctx, "เกิดข้อผิดพลาด: ${e2.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                isTranslated = false
                                isTranslating = false
                                if (url != null) {
                                    inputUrl = url
                                    currentUrl = url
                                    prefs.webUrl = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true

                                // Early injection for Web Speech & Chrome Translate
                                view?.evaluateJavascript(NovelTtsBridge.INJECTION_SCRIPT, null)
                                view?.evaluateJavascript(NovelTtsBridge.TRANSLATE_INJECTION_SCRIPT, null)
                                view?.evaluateJavascript("if (window.__android_tts_sync_ready) window.__android_tts_sync_ready();", null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                pageTitle = view?.title ?: "เว็บแปลนิยาย"
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true

                                // Re-inject bridge and translation hooks
                                view?.evaluateJavascript(NovelTtsBridge.INJECTION_SCRIPT, null)
                                view?.evaluateJavascript(NovelTtsBridge.TRANSLATE_INJECTION_SCRIPT, null)
                                view?.evaluateJavascript("if (window.__android_tts_sync_ready) window.__android_tts_sync_ready();", null)

                                // Restore scroll position if page was reloaded as fallback
                                view?.evaluateJavascript("""
                                    (function() {
                                        try {
                                            var saved = sessionStorage.getItem('__novel_saved_scroll');
                                            if (saved) {
                                                sessionStorage.removeItem('__novel_saved_scroll');
                                                var y = parseInt(saved, 10);
                                                if (!isNaN(y) && y > 0) {
                                                    window.scrollTo(0, y);
                                                    setTimeout(function() { window.scrollTo(0, y); }, 150);
                                                }
                                            }
                                        } catch(e){}
                                    })();
                                """.trimIndent(), null)

                                // ONLY trigger translation if auto-translate is explicitly enabled
                                if (prefs.isAutoTranslate) {
                                    view?.postDelayed({
                                        if (prefs.isAutoTranslate) {
                                            bridge.translatePage(prefs.targetLanguage)
                                        }
                                    }, 600)
                                }

                                // Auto-continue reading next/prev chapter when triggered by background or user navigation
                                // Enhanced with persistent polling interval to ensure slow or background-throttled pages never get stuck
                                val autoPlayScript = """
                                    (function() {
                                        try {
                                            if (sessionStorage.getItem('__novel_auto_play_next') !== 'true') return;

                                            function tryClickPlay() {
                                                if (sessionStorage.getItem('__novel_auto_play_next') !== 'true') return true;

                                                const playSelectors = [
                                                    '.btn-read', '.btn-play', '#btn-tts', '#read-novel', '[data-action="auto-read"]',
                                                    '.tts-play', '#play-button', '.reader-play', '.audio-play', '.play-btn', '.btn-read-play',
                                                    '.tts-btn', '#tts-play', 'button[title*="อ่าน"]', 'button[title*="Play"]',
                                                    '[aria-label*="Play"]', '[aria-label*="อ่าน"]', '[title*="เล่น"]', '[title*="Play"]',
                                                    'button:has(.fa-play)', '.fa-play', '[data-action="read"]', '.btn-start-read'
                                                ];
                                                for (let sel of playSelectors) {
                                                    let btn = document.querySelector(sel);
                                                    if (btn && typeof btn.click === 'function') {
                                                        sessionStorage.removeItem('__novel_auto_play_next');
                                                        btn.click();
                                                        return true;
                                                    }
                                                }
                                                // If reader has a global start function
                                                if (window.reader && typeof window.reader.play === 'function') {
                                                    sessionStorage.removeItem('__novel_auto_play_next');
                                                    window.reader.play();
                                                    return true;
                                                }
                                                if (typeof window.readNovel === 'function') {
                                                    sessionStorage.removeItem('__novel_auto_play_next');
                                                    window.readNovel();
                                                    return true;
                                                }
                                                if (typeof window.startTts === 'function') {
                                                    sessionStorage.removeItem('__novel_auto_play_next');
                                                    window.startTts();
                                                    return true;
                                                }
                                                return false;
                                            }

                                            // Attempt immediately
                                            if (tryClickPlay()) return;

                                            // If not found yet (e.g. dynamic rendering in background), poll every 500ms up to 20 times (10 seconds)
                                            if (window.__novel_auto_play_timer) clearInterval(window.__novel_auto_play_timer);
                                            var count = 0;
                                            window.__novel_auto_play_timer = setInterval(function() {
                                                count++;
                                                if (tryClickPlay() || count > 20) {
                                                    clearInterval(window.__novel_auto_play_timer);
                                                    window.__novel_auto_play_timer = null;
                                                }
                                            }, 500);
                                        } catch(e) {}
                                    })();
                                """.trimIndent()

                                view?.postDelayed({ view.evaluateJavascript(autoPlayScript, null) }, 300)
                                view?.postDelayed({ view.evaluateJavascript(autoPlayScript, null) }, 1000)
                                view?.postDelayed({ view.evaluateJavascript(autoPlayScript, null) }, 2500)
                                view?.postDelayed({ view.evaluateJavascript(autoPlayScript, null) }, 5000)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                // Intercept file download links that end with known extensions and prompt system chooser
                                if (reqUrl.endsWith(".txt", ignoreCase = true) ||
                                    reqUrl.endsWith(".epub", ignoreCase = true) ||
                                    reqUrl.endsWith(".pdf", ignoreCase = true) ||
                                    reqUrl.endsWith(".zip", ignoreCase = true) ||
                                    reqUrl.endsWith(".rar", ignoreCase = true)
                                ) {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        val chooser = Intent.createChooser(browserIntent, "เลือกเบราว์เซอร์สำหรับดาวน์โหลดไฟล์...")
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        ctx.startActivity(chooser)
                                        Toast.makeText(ctx, "กำลังเปิดเบราว์เซอร์เพื่อดาวน์โหลด...", Toast.LENGTH_SHORT).show()
                                        return true
                                    } catch (e: Exception) {
                                        return false
                                    }
                                }
                                return false // Keep navigation inside webview
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress / 100f
                                if (newProgress >= 100) isLoading = false
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return true
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { view ->
                    webViewInstance = view
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("novel_webview")
            )
        }
    }
}

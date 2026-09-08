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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .testTag("web_companion_screen")
    ) {
        // Top Omnibox & Web Navigation Bar
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
                    onClick = { webViewInstance?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp).testTag("web_forward_button")
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
            }

            // Quick Native Action Bar: Page status + Google Translate button + In-App Player toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLoading) AmberPrimary else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoading) "กำลังเชื่อมต่อ..." else pageTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color(0xFFD1D5DB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Toggle Bottom 3-Menu Bar (ซ่อน/เปิด 3 เมนูล่าง)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isBottomNavVisible) Color(0xFF222222) else AmberPrimary.copy(alpha = 0.2f))
                            .border(
                                width = 1.dp,
                                color = if (isBottomNavVisible) DarkBorder else AmberPrimary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onToggleBottomNav()
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("toggle_bottom_nav_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBottomNavVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                                contentDescription = if (isBottomNavVisible) "ซ่อน 3 เมนูล่าง" else "แสดง 3 เมนูล่าง",
                                tint = if (isBottomNavVisible) Color(0xFFD1D5DB) else AmberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBottomNavVisible) "ซ่อนเมนู" else "แสดงเมนู",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBottomNavVisible) Color(0xFFD1D5DB) else AmberPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Google Translate Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isTranslateBarVisible || isTranslated) {
                                    Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0EA5E9)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF222222), Color(0xFF222222)))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isTranslateBarVisible || isTranslated) Color(0xFF38BDF8) else DarkBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isTranslated && !isTranslating) {
                                    isTranslateBarVisible = true
                                    prefs.showTranslateBar = true
                                    bridge.translatePage(targetLang)
                                } else {
                                    isTranslateBarVisible = !isTranslateBarVisible
                                    prefs.showTranslateBar = isTranslateBarVisible
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("toggle_translate_bar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "แปลภาษา Google",
                                tint = if (isTranslateBarVisible || isTranslated) Color.White else Color(0xFFD1D5DB),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTranslating) "กำลังแปล..." else if (isTranslated) "แปลไทยแล้ว ✓" else "แปลภาษา",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTranslateBarVisible || isTranslated) Color.White else Color(0xFFD1D5DB),
                                fontSize = 11.sp
                            )
                        }
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

                        // WebView internally manages hardware accelerated tile rendering via Chromium.
                        // Setting LAYER_TYPE_NONE avoids unnecessary offscreen buffers that trigger Mesa DRM rendernode errors.
                        setLayerType(android.view.View.LAYER_TYPE_NONE, null)

                        // Enable cookies and 3rd party cookies for SPA/Auth sites
                        val webViewInstanceRef = this
                        android.webkit.CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(webViewInstanceRef, true)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = false // Reduce SQLite RAM usage
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
                        }

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

                                // Auto-continue reading next/prev chapter ONLY if user triggered navigation
                                view?.postDelayed({
                                    view.evaluateJavascript("""
                                        (function() {
                                            try {
                                                if (sessionStorage.getItem('__novel_auto_play_next') === 'true') {
                                                    sessionStorage.removeItem('__novel_auto_play_next');
                                                    const autoPlayBtn = document.querySelector('.btn-read, .btn-play, #btn-tts, #read-novel, [data-action="auto-read"], .tts-play, #play-button, .reader-play, .audio-play, .play-btn, .btn-read-play, [aria-label*="Play"], [title*="เล่น"], [title*="Play"]');
                                                    if (autoPlayBtn && typeof autoPlayBtn.click === 'function') {
                                                        autoPlayBtn.click();
                                                    }
                                                }
                                            } catch(e) {}
                                        })();
                                    """.trimIndent(), null)
                                }, 1200)
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

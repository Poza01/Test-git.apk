package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bridge.NovelTtsBridge
import com.example.data.NovelPreferences
import com.example.service.TtsForegroundService
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebCompanionScreen(
    service: TtsForegroundService?,
    onNavigateToHyperOsGuide: () -> Unit,
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

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val bridge = remember {
        NovelTtsBridge(
            context = context,
            getService = { service },
            getWebView = { webViewInstance }
        )
    }

    androidx.compose.runtime.LaunchedEffect(service, webViewInstance) {
        bridge.attachServiceListener()
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

            // Quick Native Action Bar: Extract & Read Webpage Paragraphs
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

                // Action button: Read aloud with Android Native Service
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AmberPrimary, AmberSecondary)
                            )
                        )
                        .clickable {
                            // Extract paragraphs from novel reader webpage
                            val jsExtract = """
                                (function() {
                                    try {
                                        let title = document.title || "นิยาย";
                                        let h1 = document.querySelector('h1, h2, .novel-title, .chapter-title');
                                        if (h1 && h1.innerText) title = h1.innerText.trim();

                                        let paragraphs = [];
                                        // 1. Look for translated / reader paragraphs in novel reader
                                        let elements = document.querySelectorAll('p, [data-para-key], .translated-text, article p, .reading-content p');
                                        elements.forEach(el => {
                                            let text = el.innerText ? el.innerText.trim() : '';
                                            if (text.length > 5 && !text.includes('javascript:') && !text.includes('Cookie')) {
                                                paragraphs.push(text);
                                            }
                                        });

                                        if (paragraphs.length === 0) {
                                            // Fallback: full body text
                                            paragraphs = (document.body.innerText || '').split('\n').filter(s => s.trim().length > 5);
                                        }

                                        if (window.AndroidTtsBridge && paragraphs.length > 0) {
                                            window.AndroidTtsBridge.speakParagraphs(JSON.stringify(paragraphs.slice(0, 200)), title, 0);
                                        } else if (window.AndroidTtsBridge) {
                                            window.AndroidTtsBridge.speak(document.body.innerText || "ไม่พบข้อความ", title);
                                        }
                                    } catch(e) {
                                        console.error("Extract error", e);
                                    }
                                })();
                            """
                            webViewInstance?.evaluateJavascript(jsExtract, null)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("extract_and_read_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "อ่านเสียงหน้านี้",
                            tint = Color(0xFF0F0F0F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "อ่านเสียงหน้านี้",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0F0F)
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
                            allowFileAccess = true
                            allowContentAccess = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString NovelAI-AndroidBridge/1.0"
                        }

                        // Attach the Native TTS JavaScript Bridge
                        addJavascriptInterface(bridge, NovelTtsBridge.JS_INTERFACE_NAME)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                if (url != null) {
                                    inputUrl = url
                                    currentUrl = url
                                    prefs.webUrl = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true

                                // Early injection
                                view?.evaluateJavascript(NovelTtsBridge.INJECTION_SCRIPT, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                pageTitle = view?.title ?: "เว็บแปลนิยาย"
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true

                                // Re-inject bridge and hooks
                                view?.evaluateJavascript(NovelTtsBridge.INJECTION_SCRIPT, null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
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

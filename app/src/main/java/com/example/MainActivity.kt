package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.service.TtsForegroundService
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppScreen
import com.example.ui.screens.HyperOsGuideScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.screens.WebCompanionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var ttsService: TtsForegroundService? by mutableStateOf(null)
    private var isBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TtsForegroundService.LocalBinder
            ttsService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ttsService = null
            isBound = false
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureWebViewCacheDirExists()
        checkNotificationPermission()
        ttsService = TtsForegroundService.instance
        bindTtsService()

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    service = ttsService
                )
            }
        }
    }

    private fun ensureWebViewCacheDirExists() {
        try {
            val codeCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            codeCacheDir.mkdirs()
            wasmCacheDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun bindTtsService() {
        val intent = Intent(this, TtsForegroundService::class.java)
        startService(intent) // Ensure service stays alive
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}

@Composable
fun MainAppContainer(
    service: TtsForegroundService?
) {
    var currentScreen by remember { mutableStateOf(AppScreen.WEB_READER) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F0F0F),
        bottomBar = {
            AppBottomNav(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F0F0F))
        ) {
            // 1. WebCompanionScreen is ALWAYS mounted in the hierarchy so the WebView DOM,
            // loaded URL, TTS audio stream, and translation state are never lost or reloaded!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (currentScreen == AppScreen.WEB_READER) {
                            Modifier.zIndex(1f)
                        } else {
                            Modifier.zIndex(0f).alpha(0f)
                        }
                    )
            ) {
                WebCompanionScreen(
                    service = service,
                    onNavigateToBackgroundSettings = { currentScreen = AppScreen.BACKGROUND_SETTINGS }
                )
            }

            // 2. Voice Settings Screen (Overlay)
            if (currentScreen == AppScreen.VOICE_SETTINGS) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .background(Color(0xFF0F0F0F))
                ) {
                    VoiceSettingsScreen(
                        service = service
                    )
                }
            }

            // 3. Background Settings Screen (Overlay)
            if (currentScreen == AppScreen.BACKGROUND_SETTINGS) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .background(Color(0xFF0F0F0F))
                ) {
                    HyperOsGuideScreen(
                        service = service
                    )
                }
            }
        }
    }
}

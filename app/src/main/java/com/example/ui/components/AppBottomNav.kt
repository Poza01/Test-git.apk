package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurface

enum class AppScreen(val title: String) {
    WEB_READER("เว็บแปลนิยาย"),
    DIRECT_READER("อ่านข้อความ"),
    VOICE_SETTINGS("เสียงเครื่อง"),
    HYPEROS_GUIDE("กันดับ HyperOS")
}

@Composable
fun AppBottomNav(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("app_bottom_nav"),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.WEB_READER,
            onClick = { onScreenSelected(AppScreen.WEB_READER) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.WEB_READER) Icons.Filled.Language else Icons.Outlined.Language,
                    contentDescription = "เว็บแปลนิยาย"
                )
            },
            label = {
                Text(
                    text = "เว็บนิยาย",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.WEB_READER) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F0F0F),
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            ),
            modifier = Modifier.testTag("nav_tab_web")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.DIRECT_READER,
            onClick = { onScreenSelected(AppScreen.DIRECT_READER) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.DIRECT_READER) Icons.Filled.Book else Icons.Outlined.Book,
                    contentDescription = "อ่านข้อความ"
                )
            },
            label = {
                Text(
                    text = "อ่านข้อความ",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.DIRECT_READER) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F0F0F),
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            ),
            modifier = Modifier.testTag("nav_tab_direct")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.VOICE_SETTINGS,
            onClick = { onScreenSelected(AppScreen.VOICE_SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.VOICE_SETTINGS) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver,
                    contentDescription = "ตั้งค่าเสียง"
                )
            },
            label = {
                Text(
                    text = "เสียงเครื่อง",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.VOICE_SETTINGS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F0F0F),
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            ),
            modifier = Modifier.testTag("nav_tab_voice")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.HYPEROS_GUIDE,
            onClick = { onScreenSelected(AppScreen.HYPEROS_GUIDE) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.HYPEROS_GUIDE) Icons.Filled.ElectricBolt else Icons.Outlined.ElectricBolt,
                    contentDescription = "HyperOS"
                )
            },
            label = {
                Text(
                    text = "HyperOS 2",
                    fontSize = 11.sp,
                    fontWeight = if (currentScreen == AppScreen.HYPEROS_GUIDE) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F0F0F),
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            ),
            modifier = Modifier.testTag("nav_tab_hyperos")
        )
    }
}

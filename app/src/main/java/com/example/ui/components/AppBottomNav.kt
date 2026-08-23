package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary

enum class AppScreen(val title: String) {
    WEB_READER("เว็บนิยาย"),
    VOICE_SETTINGS("ตั้งค่าเสียง"),
    BACKGROUND_SETTINGS("ตั้งค่าเบื้องหลัง")
}

@Composable
fun AppBottomNav(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("app_bottom_nav"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141416).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26262B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlimNavItem(
                title = "เว็บนิยาย",
                selectedIcon = Icons.Filled.Language,
                unselectedIcon = Icons.Outlined.Language,
                isSelected = currentScreen == AppScreen.WEB_READER,
                onClick = { onScreenSelected(AppScreen.WEB_READER) },
                testTag = "nav_tab_web"
            )

            SlimNavItem(
                title = "ตั้งค่าเสียง",
                selectedIcon = Icons.Filled.RecordVoiceOver,
                unselectedIcon = Icons.Outlined.RecordVoiceOver,
                isSelected = currentScreen == AppScreen.VOICE_SETTINGS,
                onClick = { onScreenSelected(AppScreen.VOICE_SETTINGS) },
                testTag = "nav_tab_voice"
            )

            SlimNavItem(
                title = "ตั้งค่าเบื้องหลัง",
                selectedIcon = Icons.Filled.ElectricBolt,
                unselectedIcon = Icons.Outlined.ElectricBolt,
                isSelected = currentScreen == AppScreen.BACKGROUND_SETTINGS,
                onClick = { onScreenSelected(AppScreen.BACKGROUND_SETTINGS) },
                testTag = "nav_tab_background"
            )
        }
    }
}

@Composable
private fun SlimNavItem(
    title: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected) {
                    Modifier.background(Brush.horizontalGradient(listOf(AmberPrimary, AmberSecondary)))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF0F0F0F) else Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF0F0F0F) else Color(0xFF9CA3AF)
            )
        }
    }
}


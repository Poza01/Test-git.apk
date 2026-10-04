package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.download.DownloadHelper
import com.example.data.download.DownloadItem
import com.example.data.download.FileCategory
import kotlinx.coroutines.launch
import java.util.Locale

enum class DownloadSortOption(val label: String) {
    NEWEST("ล่าสุดก่อน"),
    OLDEST("เก่าสุดก่อน"),
    SIZE_DESC("ใหญ่ไปเล็ก"),
    SIZE_ASC("เล็กไปใหญ่"),
    NAME_ASC("ชื่อ A-Z")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSheet(
    onDismiss: () -> Unit,
    onOpenInNovelReader: ((text: String, title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val downloads by DownloadHelper.downloadsFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }
    var currentSort by remember { mutableStateOf(DownloadSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<DownloadItem?>(null) }

    LaunchedEffect(Unit) {
        DownloadHelper.refreshDownloads(context)
    }

    // Filter & Sort list
    val filteredList = remember(downloads, searchQuery, selectedCategory, currentSort) {
        downloads.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.fileName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == FileCategory.ALL || item.category == selectedCategory
            matchesSearch && matchesCategory
        }.let { list ->
            when (currentSort) {
                DownloadSortOption.NEWEST -> list.sortedByDescending { it.lastModified }
                DownloadSortOption.OLDEST -> list.sortedBy { it.lastModified }
                DownloadSortOption.SIZE_DESC -> list.sortedByDescending { it.fileSize }
                DownloadSortOption.SIZE_ASC -> list.sortedBy { it.fileSize }
                DownloadSortOption.NAME_ASC -> list.sortedBy { it.fileName.lowercase(Locale.getDefault()) }
            }
        }
    }

    val totalSizeFormatted = remember(downloads) {
        val totalBytes = downloads.sumOf { it.fileSize }
        when {
            totalBytes < 1024 -> "$totalBytes B"
            totalBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", totalBytes / 1024.0)
            totalBytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(bottom = 12.dp)
        ) {
            // Drag Handle Bar Visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }

            // Header Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "ดาวน์โหลด",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "คลังดาวน์โหลด",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${downloads.size} รายการ • ใช้พื้นที่ $totalSizeFormatted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิด",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Storage & Summary Banner Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "จัดเก็บในโฟลเดอร์แอปหลัก",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Sort Button Dropdown
                    Box {
                        AssistChip(
                            onClick = { showSortMenu = true },
                            label = {
                                Text(
                                    text = currentSort.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "เรียงตาม",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DownloadSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal,
                                            color = if (option == currentSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        currentSort = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("ค้นหาชื่อไฟล์...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "ค้นหา",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "ล้าง", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips with Dynamic Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileCategory.entries.forEach { category ->
                    val count = remember(downloads, category) {
                        if (category == FileCategory.ALL) downloads.size
                        else downloads.count { it.category == category }
                    }

                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = "${category.label} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Downloaded Files List or Modern Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.DownloadDone,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "ไม่พบไฟล์ที่ตรงกับ \"$searchQuery\"" else "ไม่มีไฟล์ในหมวดหมู่นี้",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "ลองเปลี่ยนคำค้นหา หรือล้างช่องค้นหา" else "เมื่อคุณดาวน์โหลดไฟล์จากเว็บ จะแสดงผลที่นี่ทันที",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { searchQuery = "" },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("ล้างการค้นหา")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.file.absolutePath }) { item ->
                        DownloadItemCard(
                            item = item,
                            onClick = {
                                DownloadHelper.openFile(context, item)
                            },
                            onOpenInTts = if (item.isNovelOrText && onOpenInNovelReader != null) {
                                {
                                    coroutineScope.launch {
                                        val text = DownloadHelper.readFileContent(item.file)
                                        if (text.isNotBlank()) {
                                            sheetState.hide()
                                            onDismiss()
                                            onOpenInNovelReader(text, item.fileName.substringBeforeLast("."))
                                        }
                                    }
                                }
                            } else null,
                            onShare = { DownloadHelper.shareFile(context, item) },
                            onDelete = { fileToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "ลบไฟล์ดาวน์โหลด?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "คุณต้องการลบไฟล์ \"${target.fileName}\" ใช่หรือไม่?\n\nไฟล์นี้จะถูกลบออกจากที่จัดเก็บอย่างถาวร"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            DownloadHelper.deleteFile(context, target)
                            fileToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ลบถาวร", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("ยกเลิก")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onClick: () -> Unit,
    onOpenInTts: (() -> Unit)? = null,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, iconTint, bgTint) = getFileVisuals(item.category)

    val extensionLabel = remember(item.file) {
        val ext = item.file.extension.uppercase()
        if (ext.isNotBlank()) ext else "FILE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Visual Badge Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // File Details Name & Meta
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // File Extension Badge Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bgTint.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = extensionLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconTint,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = item.formattedSize,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // More Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "เมนูเพิ่มเติม",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("เปิดไฟล์ด้วยแอปอื่น") },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )

                        if (onOpenInTts != null) {
                            DropdownMenuItem(
                                text = { Text("อ่านด้วยเสียง (TTS)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onOpenInTts()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("แชร์ไฟล์") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("ลบไฟล์", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Quick Direct Action Button (e.g. Read TTS direct shortcut button for novel files)
            if (onOpenInTts != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenInTts,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "อ่านด้วยเสียง TTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getFileVisuals(category: FileCategory): Triple<ImageVector, Color, Color> {
    return when (category) {
        FileCategory.NOVEL -> Triple(
            Icons.Default.Description,
            Color(0xFF0288D1),
            Color(0xFFE1F5FE)
        )
        FileCategory.DOCUMENT -> Triple(
            Icons.Default.PictureAsPdf,
            Color(0xFFD32F2F),
            Color(0xFFFFEBEE)
        )
        FileCategory.AUDIO -> Triple(
            Icons.Default.Audiotrack,
            Color(0xFF7B1FA2),
            Color(0xFFF3E5F5)
        )
        FileCategory.IMAGE -> Triple(
            Icons.Default.Image,
            Color(0xFFE65100),
            Color(0xFFFFF3E0)
        )
        FileCategory.ARCHIVE -> Triple(
            Icons.Default.FolderZip,
            Color(0xFF388E3C),
            Color(0xFFE8F5E9)
        )
        else -> Triple(
            Icons.Default.InsertDriveFile,
            Color(0xFF546E7A),
            Color(0xFFECEFF1)
        )
    }
}

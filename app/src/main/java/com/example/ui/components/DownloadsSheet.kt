package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.AlertDialog
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
    var fileToDelete by remember { mutableStateOf<DownloadItem?>(null) }

    LaunchedEffect(Unit) {
        DownloadHelper.refreshDownloads(context)
    }

    val filteredList = remember(downloads, searchQuery, selectedCategory) {
        downloads.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.fileName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == FileCategory.ALL || item.category == selectedCategory
            matchesSearch && matchesCategory
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Downloads",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ดาวน์โหลด",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${downloads.size} ไฟล์ • $totalSizeFormatted",
                            style = MaterialTheme.typography.bodySmall,
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("ค้นหาไฟล์ที่ดาวน์โหลด...", fontSize = 14.sp) },
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
                            Icon(Icons.Default.Close, contentDescription = "ล้างการค้นหา", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Downloaded Files List or Empty State
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
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "ไม่พบไฟล์ที่ตรงกับการค้นหา" else "ยังไม่มีไฟล์ที่ดาวน์โหลด",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ไฟล์ที่คุณดาวน์โหลดจากเว็ปไซต์จะปรากฏที่นี่",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.file.absolutePath }) { item ->
                        DownloadItemCard(
                            item = item,
                            onClick = {
                                DownloadHelper.openFile(context, item, onOpenInNovelReader)
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
            title = { Text("ลบไฟล์ดาวน์โหลด?") },
            text = { Text("คุณต้องการลบไฟล์ \"${target.fileName}\" ใช่หรือไม่? (การกระทำนี้จะลบไฟล์ออกจากเครื่องอย่างถาวร)") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            DownloadHelper.deleteFile(context, target)
                            fileToDelete = null
                        }
                    }
                ) {
                    Text("ลบ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Type Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // File Info
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
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More Options Dropdown
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "ตัวเลือกเพิ่มเติม",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("เปิดไฟล์") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )

                    if (onOpenInTts != null) {
                        DropdownMenuItem(
                            text = { Text("อ่านด้วยเสียง (TTS)") },
                            leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                onOpenInTts()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("แชร์") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("ลบ", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
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

package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.ManagedFile
import com.example.ui.components.MayaHeader
import com.example.ui.theme.MayaBgDark
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaPink
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.MayaSurfaceBorder
import com.example.ui.theme.MayaSurfaceDark
import com.example.ui.theme.MayaSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MayaViewModel
import com.example.viewmodel.ScreenRoute

@Composable
fun FileManagerScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    BackHandler {
        onNavigate(ScreenRoute.HOME)
    }

    val files by viewModel.managedFiles.collectAsState()
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }

    val filteredFiles = if (selectedCategory == null) files else files.filter { it.category == selectedCategory }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayaBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MayaHeader(
                title = "File Manager",
                showOnlineStatus = false,
                showBackButton = true,
                onBack = { onNavigate(ScreenRoute.HOME) }
            )

            // Top Category Shortcut Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FileFolderChip(
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    count = files.count { it.category == FileCategory.DOWNLOADS },
                    isSelected = selectedCategory == FileCategory.DOWNLOADS,
                    onClick = {
                        selectedCategory = if (selectedCategory == FileCategory.DOWNLOADS) null else FileCategory.DOWNLOADS
                    }
                )
                FileFolderChip(
                    icon = Icons.Default.Description,
                    label = "Documents",
                    count = files.count { it.category == FileCategory.DOCUMENTS },
                    isSelected = selectedCategory == FileCategory.DOCUMENTS,
                    onClick = {
                        selectedCategory = if (selectedCategory == FileCategory.DOCUMENTS) null else FileCategory.DOCUMENTS
                    }
                )
                FileFolderChip(
                    icon = Icons.Default.Image,
                    label = "Images",
                    count = files.count { it.category == FileCategory.IMAGES },
                    isSelected = selectedCategory == FileCategory.IMAGES,
                    onClick = {
                        selectedCategory = if (selectedCategory == FileCategory.IMAGES) null else FileCategory.IMAGES
                    }
                )
            }

            // Voice Command Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MayaSurfaceElevated)
                    .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice: \"Maya, zip this folder\" or \"Maya, find my PDF\"",
                        color = MayaCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Files List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.id }) { file ->
                    FileItemRow(
                        file = file,
                        onOpen = { viewModel.handleVoiceInput("Open ${file.name}") },
                        onShare = { viewModel.handleVoiceInput("Share ${file.name}") },
                        onDelete = { viewModel.handleVoiceInput("Delete file") }
                    )
                }

                if (filteredFiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No files in this folder", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Bottom Quick Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayaSurfaceDark)
                        .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.handleVoiceInput("Create folder Projects") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ New Folder", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayaCyan)
                        .clickable { viewModel.handleVoiceInput("Zip this folder") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Zip Files", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FileFolderChip(
    icon: ImageVector,
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MayaCyan else MayaSurfaceDark)
            .border(1.dp, if (isSelected) MayaCyan else MayaSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else MayaCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.Black else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FileItemRow(
    file: ManagedFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayaSurfaceDark)
            .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MayaSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (file.category) {
                        FileCategory.DOCUMENTS -> Icons.Default.Description
                        FileCategory.IMAGES -> Icons.Default.Image
                        FileCategory.VIDEOS -> Icons.Default.VideoLibrary
                        FileCategory.ARCHIVES -> Icons.Default.Archive
                        FileCategory.DOWNLOADS -> Icons.Default.Download
                    },
                    contentDescription = null,
                    tint = MayaCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.size} • ${file.dateModified}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onShare)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }
    }
}

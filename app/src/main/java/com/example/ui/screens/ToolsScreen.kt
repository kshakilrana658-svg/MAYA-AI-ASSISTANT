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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.MayaBottomBar
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

private data class ToolCard(
    val title: String,
    val subtitle: String,
    val voiceExample: String,
    val category: String,
    val icon: ImageVector,
    val accentColor: Color,
    val action: (MayaViewModel, (ScreenRoute) -> Unit) -> Unit
)

@Composable
fun ToolsScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    BackHandler {
        onNavigate(ScreenRoute.HOME)
    }

    val isBn = viewModel.isBangla
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = if (isBn) listOf("সব", "ফোন", "ওয়েব", "ফাইল") else listOf("All", "Phone", "Web", "Files")

    val tools = listOf(
        ToolCard(
            title = if (isBn) "অ্যাপ খোলা" else "Open App",
            subtitle = if (isBn) "যেকোনো ইনস্টল অ্যাপ লঞ্চ করুন" else "Launch any installed app",
            voiceExample = if (isBn) "\"মায়া, গুগল ক্রোম খোলো\"" else "\"Maya, open Chrome\"",
            category = if (isBn) "ফোন" else "Phone",
            icon = Icons.Default.PlayArrow,
            accentColor = MayaCyan,
            action = { vm, _ -> vm.handleVoiceInput("Open Chrome") }
        ),
        ToolCard(
            title = if (isBn) "ওয়েব সার্চ" else "Web Search",
            subtitle = if (isBn) "ইন্টারনেটে তথ্য অনুসন্ধান" else "Search web information",
            voiceExample = if (isBn) "\"মায়া, আজকের আবহাওয়া বলো\"" else "\"Maya, what is the weather today\"",
            category = if (isBn) "ওয়েব" else "Web",
            icon = Icons.Default.Language,
            accentColor = MayaPurple,
            action = { vm, _ -> vm.handleVoiceInput("আজকের আবহাওয়া বলো") }
        ),
        ToolCard(
            title = if (isBn) "ফাইল ম্যানেজমেন্ট" else "File Manager",
            subtitle = if (isBn) "ফাইল খুঁজুন, মুভ ও জিপ করুন" else "Manage, zip & locate files",
            voiceExample = if (isBn) "\"মায়া, ফাইল খুঁজে দাও\"" else "\"Maya, find my files\"",
            category = if (isBn) "ফাইল" else "Files",
            icon = Icons.Default.Folder,
            accentColor = MayaCyan,
            action = { _, nav -> nav(ScreenRoute.FILE_MANAGER) }
        ),
        ToolCard(
            title = if (isBn) "ওয়েবসাইট ও প্রজেক্ট" else "Task Pipeline",
            subtitle = if (isBn) "মাল্টি-স্টেপ অটোমেটেড টাস্ক" else "Multi-step automated task",
            voiceExample = if (isBn) "\"মায়া, ওয়েবসাইট তৈরি করো\"" else "\"Maya, create website\"",
            category = if (isBn) "ওয়েব" else "Web",
            icon = Icons.Default.Work,
            accentColor = MayaPink,
            action = { vm, _ -> vm.handleVoiceInput("Create Website") }
        ),
        ToolCard(
            title = if (isBn) "স্ক্রিন পড়ে শোনানো" else "Read Screen",
            subtitle = if (isBn) "স্ক্রিনের লেখা পড়ে বোঝানো" else "Extract visible screen text",
            voiceExample = if (isBn) "\"মায়া, স্ক্রিন পড়ে শোনাও\"" else "\"Maya, read this screen\"",
            category = if (isBn) "ফোন" else "Phone",
            icon = Icons.Default.ScreenSearchDesktop,
            accentColor = MayaCyan,
            action = { vm, _ -> vm.handleVoiceInput("Read this screen") }
        ),
        ToolCard(
            title = if (isBn) "ক্যামেরা ও ভেরিফিকেশন" else "Camera",
            subtitle = if (isBn) "ফেস ভেরিফাই ও ক্যাপচার" else "Capture & face verify",
            voiceExample = if (isBn) "\"মায়া, ক্যামেরা খোলো\"" else "\"Maya, open camera\"",
            category = if (isBn) "ফোন" else "Phone",
            icon = Icons.Default.CameraAlt,
            accentColor = MayaPurple,
            action = { vm, _ -> vm.handleVoiceInput("Open camera") }
        ),
        ToolCard(
            title = if (isBn) "ফ্ল্যাশলাইট কন্ট্রোল" else "Flashlight",
            subtitle = if (isBn) "টর্চ অন বা অফ করুন" else "Turn torch on or off",
            voiceExample = if (isBn) "\"মায়া, ফ্ল্যাশলাইট জ্বালাও\"" else "\"Maya, turn on flashlight\"",
            category = if (isBn) "ফোন" else "Phone",
            icon = Icons.Default.PhotoLibrary,
            accentColor = MayaCyan,
            action = { vm, _ -> vm.handleVoiceInput("ফ্ল্যাশলাইট জ্বালাও") }
        ),
        ToolCard(
            title = if (isBn) "টাস্ক হিস্ট্রি" else "Task History",
            subtitle = if (isBn) "পূর্বের ভয়েস কমান্ড রেকর্ড" else "Review past voice logs",
            voiceExample = if (isBn) "\"মায়া, হিস্ট্রি দেখাও\"" else "\"Maya, check task history\"",
            category = if (isBn) "সব" else "All",
            icon = Icons.Default.History,
            accentColor = MayaPink,
            action = { _, nav -> nav(ScreenRoute.TASK_HISTORY) }
        )
    )

    val allCat = if (isBn) "সব" else "All"
    val filteredTools = if (selectedCategory == allCat || selectedCategory == "All") tools else tools.filter { it.category == selectedCategory }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayaBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MayaHeader(
                title = if (isBn) "মায়া যা যা করতে পারে" else "Maya Tools",
                showOnlineStatus = true,
                onSettingsClick = { onNavigate(ScreenRoute.SETTINGS) }
            )

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MayaCyan else MayaSurfaceDark)
                            .border(
                                1.dp,
                                if (isSelected) MayaCyan else MayaSurfaceBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("filter_cat_$cat")
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of Tools
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTools) { tool ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(MayaSurfaceDark)
                            .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(18.dp))
                            .clickable { tool.action(viewModel, onNavigate) }
                            .padding(16.dp)
                            .testTag("tool_card_${tool.title.replace(" ", "_")}")
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(tool.accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, tool.accentColor.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = tool.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = tool.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tool.subtitle,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tool.voiceExample,
                                color = MayaCyan.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            MayaBottomBar(
                currentRoute = ScreenRoute.TOOLS,
                onNavigate = onNavigate
            )
        }
    }
}

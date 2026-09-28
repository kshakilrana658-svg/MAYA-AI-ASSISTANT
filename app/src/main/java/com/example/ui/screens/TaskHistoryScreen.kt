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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.TaskHistoryItem
import com.example.ui.components.MayaHeader
import com.example.ui.theme.MayaBgDark
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaSurfaceBorder
import com.example.ui.theme.MayaSurfaceDark
import com.example.ui.theme.MayaSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MayaViewModel
import com.example.viewmodel.ScreenRoute
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskHistoryScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    BackHandler {
        onNavigate(ScreenRoute.HOME)
    }

    val history by viewModel.taskHistory.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayaBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MayaHeader(
                title = "Task History",
                showOnlineStatus = false,
                showBackButton = true,
                onBack = { onNavigate(ScreenRoute.HOME) }
            )

            // Header action: clear history
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${history.size} voice logs recorded",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                if (history.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayaSurfaceElevated)
                            .clickable { viewModel.clearHistory() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("clear_history_button")
                    ) {
                        Text(
                            text = "Clear All",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // History List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    TaskHistoryCard(
                        item = item,
                        onDelete = { viewModel.deleteHistoryItem(item) }
                    )
                }

                if (history.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history entries recorded yet", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(
    item: TaskHistoryItem,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))

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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (item.isSuccess) Color(0x2200E676) else Color(0x22FF5252)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isSuccess) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (item.isSuccess) "Success" else "Failed",
                    tint = if (item.isSuccess) Color(0xFF00E676) else Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.command,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.resultText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = TextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDelete)
            )
        }
    }
}

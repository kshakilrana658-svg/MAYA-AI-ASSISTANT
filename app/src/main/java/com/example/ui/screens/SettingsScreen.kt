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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun SettingsScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    BackHandler {
        onNavigate(ScreenRoute.HOME)
    }

    val userSettings by viewModel.userSettings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayaBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MayaHeader(
                title = "Settings",
                showOnlineStatus = false,
                onSettingsClick = {}
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // User Account Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MayaSurfaceElevated)
                            .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MayaCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userSettings.userName.take(1).uppercase(),
                                    color = Color.Black,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = userSettings.userName,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userSettings.userEmail,
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VOICE & INTERACTION",
                        color = MayaCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.Mic,
                        title = "Wake Phrase",
                        subtitle = "Current: \"${userSettings.wakePhrase}\"",
                        onClick = { onNavigate(ScreenRoute.SETTINGS_WAKE_PHRASE) }
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.StopCircle,
                        title = "Stop Command",
                        subtitle = "Current: \"${userSettings.stopCommand}\"",
                        onClick = { onNavigate(ScreenRoute.SETTINGS_STOP_COMMAND) }
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "Voice Settings",
                        subtitle = "${userSettings.voiceName} • ${userSettings.voiceStyle}",
                        onClick = { onNavigate(ScreenRoute.SETTINGS_VOICE) }
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = userSettings.language,
                        onClick = { onNavigate(ScreenRoute.SETTINGS_LANGUAGE) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SECURITY & PRIVACY",
                        color = MayaCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Security & Face Verification",
                        subtitle = if (userSettings.isFaceVerificationEnabled) "Face Verification active" else "Standard PIN / None",
                        onClick = { onNavigate(ScreenRoute.SETTINGS_SECURITY) }
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Permission Center",
                        subtitle = "Microphone, Camera, Overlay, Notifications",
                        onClick = { onNavigate(ScreenRoute.PERMISSION_SETUP) }
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.Shield,
                        title = "Privacy Dashboard",
                        subtitle = "Review real-time device access status",
                        onClick = { onNavigate(ScreenRoute.SETTINGS_PRIVACY) }
                    )
                }

                item {
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "About Maya",
                        subtitle = "v1.0.0 • Powered by Gemini AI",
                        onClick = { onNavigate(ScreenRoute.SETTINGS_ABOUT) }
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MayaSurfaceDark)
                            .border(1.dp, Color(0x33FF5252), RoundedCornerShape(16.dp))
                            .clickable { viewModel.logout() }
                            .padding(16.dp)
                            .testTag("logout_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Log Out",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Log Out",
                                color = Color(0xFFFF5252),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            MayaBottomBar(
                currentRoute = ScreenRoute.SETTINGS,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayaSurfaceDark)
            .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("settings_row_${title.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MayaSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MayaCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

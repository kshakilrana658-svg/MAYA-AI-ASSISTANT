package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MayaState
import com.example.ui.components.MayaAvatar
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    val mayaState by viewModel.mayaState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val audioRms by viewModel.rmsAudioLevel.collectAsState()
    val lastResponse by viewModel.lastMayaResponse.collectAsState()
    val lastTranscript by viewModel.lastVoiceTranscript.collectAsState()

    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MayaBgDark,
                        Color(0xFF090D24),
                        Color(0xFF050714)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            MayaHeader(
                title = "MAYA",
                showOnlineStatus = true,
                onSettingsClick = { onNavigate(ScreenRoute.SETTINGS) }
            )

            Spacer(modifier = Modifier.weight(0.15f))

            // State Indicator Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MayaSurfaceElevated.copy(alpha = 0.8f))
                    .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (mayaState) {
                                    MayaState.SLEEPING -> MayaCyan.copy(alpha = 0.5f)
                                    MayaState.LISTENING -> MayaCyan
                                    MayaState.UNDERSTANDING, MayaState.THINKING -> MayaPurple
                                    MayaState.WORKING -> MayaCyan
                                    MayaState.SUCCESS -> Color(0xFF00E676)
                                    MayaState.ERROR -> Color(0xFFFF5252)
                                    else -> Color(0xFFFFAB00)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mayaState.getLabel(viewModel.isBangla),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Centerpiece Maya Avatar with Voice Animation
            MayaAvatar(
                state = mayaState,
                audioRms = audioRms,
                size = 280.dp,
                onClick = {
                    if (mayaState == MayaState.SLEEPING || mayaState == MayaState.STOPPED) {
                        if (micPermissionState.status.isGranted) {
                            viewModel.activateListening()
                        } else {
                            micPermissionState.launchPermissionRequest()
                        }
                    } else if (mayaState == MayaState.LISTENING) {
                        viewModel.cancelListening()
                    }
                },
                modifier = Modifier.testTag("maya_avatar_orb")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Voice Guidance / Last Response Text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (mayaState) {
                        MayaState.SLEEPING -> if (viewModel.isBangla) "বলুন: \"${userSettings.wakePhrase}\"" else "Say \"${userSettings.wakePhrase}\""
                        MayaState.LISTENING -> if (lastTranscript.isNotBlank()) "\"$lastTranscript\"" else (if (viewModel.isBangla) "শুনছি, বলুন..." else "I'm listening...")
                        MayaState.UNDERSTANDING -> if (viewModel.isBangla) "আপনার অনুরোধ বুঝছি..." else "Processing your command..."
                        MayaState.THINKING -> if (viewModel.isBangla) "চিন্তা করছি..." else "Reasoning with Gemini AI..."
                        MayaState.WORKING -> if (viewModel.isBangla) "কাজটি সম্পন্ন করছি..." else "Executing on your device..."
                        MayaState.RESPONDING -> lastResponse
                        MayaState.SUCCESS -> lastResponse
                        MayaState.ERROR -> lastResponse
                        MayaState.PAUSED -> if (viewModel.isBangla) "বিরতি। বলুন: \"${userSettings.wakePhrase}\"" else "Paused. Say \"${userSettings.wakePhrase}\""
                        MayaState.STOPPED -> if (viewModel.isBangla) "শুরু করতে বলুন: \"${userSettings.wakePhrase}\"" else "Say \"${userSettings.wakePhrase}\" to start"
                        else -> mayaState.getSub(viewModel.isBangla)
                    },
                    color = if (mayaState == MayaState.LISTENING) MayaCyan else TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        !micPermissionState.status.isGranted -> if (viewModel.isBangla) "কথা বলার জন্য মাইক্রোফোন অনুমতি প্রয়োজন" else "Microphone permission required to speak to Maya"
                        mayaState == MayaState.SLEEPING -> if (viewModel.isBangla) "শুধু কথা বলুন, বাকিটা আমি সামলে নেব..." else "Just speak naturally to perform any action"
                        mayaState == MayaState.LISTENING -> if (viewModel.isBangla) "কথা শেষ হলে থামুন বা বলুন \"${userSettings.stopCommand}\"" else "Say your command or \"${userSettings.stopCommand}\""
                        else -> if (viewModel.isBangla) "মায়া সক্রিয় আছে" else "Maya is working hands-free"
                    },
                    color = if (!micPermissionState.status.isGranted) MayaCyan else TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Quick permission grant banner or rationale if permission not granted
            if (!micPermissionState.status.isGranted) {
                Spacer(modifier = Modifier.height(14.dp))
                val isRationale = micPermissionState.status.shouldShowRationale
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayaSurfaceElevated)
                        .border(1.dp, MayaCyan.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .clickable { micPermissionState.launchPermissionRequest() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("grant_mic_permission_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Allow Microphone",
                            tint = MayaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRationale) "Microphone Access Required" else "Enable Microphone for Maya",
                                color = MayaCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRationale) "Maya needs audio access to hear your wake phrase & commands" else "Tap to grant voice permission",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Quick cancel affordance when listening
            AnimatedVisibility(
                visible = mayaState == MayaState.LISTENING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MayaSurfaceElevated)
                            .border(1.dp, MayaSurfaceBorder, CircleShape)
                            .clickable { viewModel.cancelListening() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("cancel_listening_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (viewModel.isBangla) "বাতিল করুন" else "Cancel",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Bottom Navigation
            MayaBottomBar(
                currentRoute = ScreenRoute.HOME,
                onNavigate = onNavigate
            )
        }
    }
}


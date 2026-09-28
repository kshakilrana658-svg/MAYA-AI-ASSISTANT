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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

@Composable
fun SecuritySettingsScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    BackHandler {
        onNavigate(ScreenRoute.SETTINGS)
    }

    val userSettings by viewModel.userSettings.collectAsState()
    var faceVerification by remember { mutableStateOf(userSettings.isFaceVerificationEnabled) }
    var pinSecurity by remember { mutableStateOf(userSettings.isPinSecurityEnabled) }
    var biometric by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayaBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MayaHeader(
                title = "Security",
                showOnlineStatus = false,
                showBackButton = true,
                onBack = { onNavigate(ScreenRoute.SETTINGS) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enable biometric face verification and PIN protection for sensitive actions such as file deletion and account access.",
                    color = TextMuted,
                    fontSize = 14.sp
                )

                SecurityToggleRow(
                    icon = Icons.Default.Face,
                    title = "Face Verification",
                    subtitle = "Verify face using camera before executing sensitive commands",
                    checked = faceVerification,
                    onCheckedChange = {
                        faceVerification = it
                        if (it) onNavigate(ScreenRoute.FACE_REGISTRATION)
                    },
                    tag = "toggle_face_verification"
                )

                SecurityToggleRow(
                    icon = Icons.Default.Pin,
                    title = "PIN / Pattern Lock",
                    subtitle = "Set 4-digit master PIN for phone lock fallback",
                    checked = pinSecurity,
                    onCheckedChange = {
                        pinSecurity = it
                        if (it) onNavigate(ScreenRoute.PIN_SETUP)
                    },
                    tag = "toggle_pin_security"
                )

                SecurityToggleRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Fingerprint",
                    subtitle = "Use Android system fingerprint sensor",
                    checked = biometric,
                    onCheckedChange = { biometric = it },
                    tag = "toggle_biometric"
                )

                // Face registration setup trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayaSurfaceElevated)
                        .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(16.dp))
                        .clickable { onNavigate(ScreenRoute.FACE_REGISTRATION) }
                        .padding(16.dp)
                        .testTag("setup_face_scan_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MayaCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = MayaCyan)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Re-register Face Scan", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Update face template in good lighting", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateUserSettings(
                            userSettings.copy(
                                isFaceVerificationEnabled = faceVerification,
                                isPinSecurityEnabled = pinSecurity
                            )
                        )
                        onNavigate(ScreenRoute.SETTINGS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_security_settings_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MayaCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save Security Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SecurityToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayaSurfaceDark)
            .border(1.dp, MayaSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
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
                Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = MayaCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = MayaSurfaceElevated
                ),
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

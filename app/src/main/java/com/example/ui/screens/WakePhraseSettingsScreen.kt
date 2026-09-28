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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.viewmodel.MayaViewModel
import com.example.viewmodel.ScreenRoute

@Composable
fun WakePhraseSettingsScreen(
    viewModel: MayaViewModel,
    onNavigate: (ScreenRoute) -> Unit
) {
    BackHandler {
        onNavigate(ScreenRoute.SETTINGS)
    }

    val userSettings by viewModel.userSettings.collectAsState()
    var selectedOption by remember { mutableStateOf(userSettings.wakePhrase) }
    var customText by remember { mutableStateOf("") }

    val presetOptions = listOf("Hey Maya", "Maya", "মায়া")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayaBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MayaHeader(
                title = "Wake Phrase",
                showOnlineStatus = false,
                showBackButton = true,
                onBack = { onNavigate(ScreenRoute.SETTINGS) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Choose the wake phrase you speak to activate Maya hands-free.",
                    color = TextMuted,
                    fontSize = 14.sp
                )

                presetOptions.forEach { opt ->
                    val isSelected = selectedOption == opt
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MayaSurfaceElevated else MayaSurfaceDark)
                            .border(
                                1.dp,
                                if (isSelected) MayaCyan else MayaSurfaceBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedOption = opt
                            }
                            .padding(18.dp)
                            .testTag("wake_opt_$opt")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = opt,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MayaCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom wake phrase option
                val isCustomSelected = !presetOptions.contains(selectedOption)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCustomSelected) MayaSurfaceElevated else MayaSurfaceDark)
                        .border(
                            1.dp,
                            if (isCustomSelected) MayaCyan else MayaSurfaceBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (customText.isNotBlank()) selectedOption = customText else selectedOption = "Custom"
                        }
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Custom Wake Phrase",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customText,
                            onValueChange = {
                                customText = it
                                selectedOption = it
                            },
                            placeholder = { Text("e.g. Hello Maya", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MayaSurfaceDark,
                                unfocusedContainerColor = MayaSurfaceDark,
                                focusedBorderColor = MayaCyan,
                                unfocusedBorderColor = MayaSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
                    onClick = {
                        val phraseToSave = if (selectedOption == "Custom") {
                            customText.ifBlank { "Hey Maya" }
                        } else {
                            selectedOption
                        }
                        viewModel.updateUserSettings(userSettings.copy(wakePhrase = phraseToSave))
                        onNavigate(ScreenRoute.SETTINGS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_wake_phrase_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MayaCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save Wake Phrase", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

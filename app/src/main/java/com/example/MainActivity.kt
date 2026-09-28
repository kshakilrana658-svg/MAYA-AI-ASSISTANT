package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AboutMayaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FaceRegistrationScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageSelectScreen
import com.example.ui.screens.LanguageSettingsScreen
import com.example.ui.screens.PermissionSetupScreen
import com.example.ui.screens.PinSetupScreen
import com.example.ui.screens.PrivacyDashboardScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StopCommandSettingsScreen
import com.example.ui.screens.TaskExecutionScreen
import com.example.ui.screens.TaskHistoryScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.screens.WakePhraseSettingsScreen
import com.example.ui.theme.MayaBgDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MayaViewModel
import com.example.viewmodel.ScreenRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val mayaViewModel: MayaViewModel = viewModel()
                val currentScreen by mayaViewModel.currentScreen.collectAsState()

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MayaBgDark)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            ScreenRoute.SPLASH -> {
                                SplashScreen(
                                    onFinished = { route ->
                                        mayaViewModel.navigateTo(route)
                                    }
                                )
                            }
                            ScreenRoute.LANGUAGE_SELECT -> {
                                LanguageSelectScreen(
                                    viewModel = mayaViewModel,
                                    onNext = { mayaViewModel.navigateTo(ScreenRoute.LOGIN) }
                                )
                            }
                            ScreenRoute.LOGIN -> {
                                AuthScreen(
                                    viewModel = mayaViewModel,
                                    onSuccess = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.PERMISSION_SETUP -> {
                                PermissionSetupScreen(
                                    onContinue = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.FACE_REGISTRATION -> {
                                FaceRegistrationScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.PIN_SETUP -> {
                                PinSetupScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SECURITY_SETUP -> {
                                SecuritySettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.HOME -> {
                                HomeScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.CHAT -> {
                                ChatScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.TOOLS -> {
                                ToolsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.TASK_EXECUTION -> {
                                TaskExecutionScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.FILE_MANAGER -> {
                                FileManagerScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.TASK_HISTORY -> {
                                TaskHistoryScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_WAKE_PHRASE -> {
                                WakePhraseSettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_STOP_COMMAND -> {
                                StopCommandSettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_VOICE -> {
                                VoiceSettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_LANGUAGE -> {
                                LanguageSettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_SECURITY -> {
                                SecuritySettingsScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_PRIVACY -> {
                                PrivacyDashboardScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                            ScreenRoute.SETTINGS_ABOUT -> {
                                AboutMayaScreen(
                                    viewModel = mayaViewModel,
                                    onNavigate = { route -> mayaViewModel.navigateTo(route) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

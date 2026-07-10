package com.biewangle.dontforget.ui.navigation

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.biewangle.dontforget.ui.screens.memo.MemoScreen
import com.biewangle.dontforget.ui.screens.settings.SettingsScreen
import com.biewangle.dontforget.ui.screens.splash.SplashScreen
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.util.SoundEffectPlayer

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Splash : Screen("splash", "启动", Icons.Default.EditNote)
    object Memos : Screen("memos", "事项", Icons.Default.EditNote)
    object Settings : Screen("settings", "我的", Icons.Default.Insights)
}

@Composable
fun BiewangleNavHost() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // Splash 页面不显示底部导航栏
    val showBottomBar = currentRoute != Screen.Splash.route

    // 注意：返回键拦截已搬到 MainActivity.onBackPressedDispatcher 中，
    // 见 [MainActivity.kt:onCreate] 里注册的 OnBackPressedCallback。
    // 这里不放 BackHandler，避免与 Activity 级回调产生优先级竞态。

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.height(64.dp),
                    containerColor = BackgroundWarm,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(Screen.Memos, Screen.Settings).forEach { screen ->
                            val selected = currentRoute == screen.route
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        SoundEffectPlayer.playButtonClick(context)
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .background(
                                        if (selected) PrimaryOrange.copy(alpha = 0.3f) else CardWhite,
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label,
                                    modifier = Modifier.size(32.dp),
                                    tint = if (selected) WhiteText else TextWarmGray
                                )
                                Text(
                                    screen.label,
                                    fontSize = 18.sp,
                                    color = if (selected) WhiteText else TextWarmGray,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(initialAlpha = 0.3f) },
            exitTransition = { fadeOut(targetAlpha = 0.3f) }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Memos.route) {
                            // 保留 Splash 在 back stack 中，使底部栏 popUpTo 能找到目标
                            popUpTo(Screen.Splash.route)
                        }
                    }
                )
            }
            composable(Screen.Memos.route) {
                MemoScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

/** 从 ContextWrapper 链中提取 Activity，防止 cast 失败 */
private fun android.content.Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

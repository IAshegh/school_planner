package com.iashegh.schoolplanner.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iashegh.schoolplanner.MainViewModel
import com.iashegh.schoolplanner.data.Settings
import com.iashegh.schoolplanner.ui.components.CelebrationBurst
import com.iashegh.schoolplanner.ui.components.LocalFeedback
import com.iashegh.schoolplanner.ui.components.LongDate
import com.iashegh.schoolplanner.ui.components.Mascot
import com.iashegh.schoolplanner.ui.components.StarField
import com.iashegh.schoolplanner.ui.components.rememberFeedback
import com.iashegh.schoolplanner.ui.screens.BackupScreen
import com.iashegh.schoolplanner.ui.screens.BellsScreen
import com.iashegh.schoolplanner.ui.screens.CalendarScreen
import com.iashegh.schoolplanner.ui.screens.ConfirmResetDialog
import com.iashegh.schoolplanner.ui.screens.CreatePinDialog
import com.iashegh.schoolplanner.ui.screens.EnterPinDialog
import com.iashegh.schoolplanner.ui.screens.OverridesScreen
import com.iashegh.schoolplanner.ui.screens.ParentHubScreen
import com.iashegh.schoolplanner.ui.screens.PlannerScreen
import com.iashegh.schoolplanner.ui.screens.ScheduleScreen
import com.iashegh.schoolplanner.ui.screens.SettingsScreen
import com.iashegh.schoolplanner.ui.screens.SubjectsScreen
import com.iashegh.schoolplanner.ui.screens.TimetableScreen
import com.iashegh.schoolplanner.ui.screens.WeekScreen
import com.iashegh.schoolplanner.ui.screens.collectAsStateCompat
import com.iashegh.schoolplanner.ui.screens.rememberNow
import com.iashegh.schoolplanner.ui.theme.LocalSkin
import com.iashegh.schoolplanner.ui.theme.PlannerTheme
import com.iashegh.schoolplanner.ui.theme.Skin
import com.iashegh.schoolplanner.ui.theme.colors
import java.time.LocalDate

private object Routes {
    const val SCHEDULE = "schedule"
    const val PLANNER = "planner"
    const val WEEK = "week"
    const val SETTINGS = "settings"
    const val PARENT = "parent"
}

@Composable
fun PlannerApp(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateCompat()
    val s = settings
    if (s == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    PlannerTheme(s.skin) {
        val feedback = rememberFeedback(s.soundOn, s.hapticsOn)
        androidx.compose.runtime.CompositionLocalProvider(LocalFeedback provides feedback) {
            PlannerScaffold(vm, s)
        }
    }
}

@Composable
private fun PlannerScaffold(vm: MainViewModel, settings: Settings) {
    val colors = settings.skin.colors()
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: Routes.SCHEDULE
    val now by rememberNow()
    val snackbar = remember { SnackbarHostState() }
    val fb = LocalFeedback.current

    // status bar matches the skin
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        @Suppress("DEPRECATION")
        window.statusBarColor = colors.background.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = colors.background.toArgb()
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !colors.dark
        controller.isAppearanceLightNavigationBars = !colors.dark
    }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    // Leaving the parent area locks it again.
    LaunchedEffect(route) { if (!route.startsWith(Routes.PARENT)) vm.lockParent() }

    // hyperspace jump between Today and Tomorrow
    val warp = remember { Animatable(0f) }
    var warpTick by remember { mutableStateOf(0) }
    LaunchedEffect(warpTick) {
        if (warpTick > 0) {
            warp.snapTo(0f)
            warp.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            warp.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
        }
    }

    // parent PIN flow
    var pinDialog by remember { mutableStateOf<String?>(null) } // "create" | "enter" | "forgot"
    fun openParent() {
        if (vm.parentUnlocked) navController.navigate(Routes.PARENT) { launchSingleTop = true }
        else pinDialog = if (settings.hasPin) "enter" else "create"
    }

    val exams by vm.exams.collectAsStateCompat()
    val today = now.toLocalDate()
    val examSoon = exams.any { !it.date.isBefore(today) && !it.date.isAfter(today.plusDays(3)) }

    val mainRoutes = listOf(Routes.SCHEDULE, Routes.PLANNER, Routes.WEEK)
    val isMain = route in mainRoutes

    Box(Modifier.fillMaxSize().background(colors.background)) {
        if (settings.skin == Skin.GALACTIC) StarField(streak = warp.value)

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colors.onSurface,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (isMain) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Mascot(cheer = vm.cheer, alert = examSoon, onTap = { fb.bleep() })
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("School Planner", style = MaterialTheme.typography.titleMedium)
                            Text(today.format(LongDate), style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                        IconButton(onClick = { fb.tap(); navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Preferences", tint = colors.onSurface)
                        }
                        IconButton(onClick = { fb.tap(); openParent() }) {
                            Icon(if (vm.parentUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock, contentDescription = "Parent area", tint = colors.onSurface)
                        }
                    }
                }
            },
            bottomBar = {
                if (isMain) {
                    NavigationBar(containerColor = colors.surface.copy(alpha = if (colors.dark) 0.85f else 1f)) {
                        listOf(
                            Triple(Routes.SCHEDULE, "Schedule", Icons.Filled.Today),
                            Triple(Routes.PLANNER, "Planner", Icons.Filled.Checklist),
                            Triple(Routes.WEEK, "Week", Icons.Filled.CalendarMonth),
                        ).forEach { (r, label, icon) ->
                            NavigationBarItem(
                                selected = route == r,
                                onClick = {
                                    fb.tap()
                                    navController.navigate(r) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (colors.dark) Color(0xFF00102A) else Color.White,
                                    selectedTextColor = colors.onSurface,
                                    indicatorColor = colors.primary,
                                    unselectedIconColor = colors.muted,
                                    unselectedTextColor = colors.muted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(navController, startDestination = Routes.SCHEDULE, modifier = Modifier.padding(padding)) {
                composable(Routes.SCHEDULE) {
                    ScheduleScreen(vm, settings, now, onDaySwitch = { if (settings.skin == Skin.GALACTIC) warpTick++ })
                }
                composable(Routes.PLANNER) { PlannerScreen(vm, settings) }
                composable(Routes.WEEK) { WeekScreen(vm, settings) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, settings) { navController.popBackStack() } }

                composable(Routes.PARENT) {
                    if (!vm.parentUnlocked) LaunchedEffect(Unit) { navController.popBackStack() }
                    else ParentHubScreen(
                        vm, settings,
                        onNavigate = { navController.navigate(it) },
                        onBack = { navController.popBackStack() },
                        onLock = { vm.lockParent(); navController.popBackStack() },
                    )
                }
                composable("parent/subjects") { Guarded(vm, navController) { SubjectsScreen(vm) { navController.popBackStack() } } }
                composable("parent/bells") { Guarded(vm, navController) { BellsScreen(vm) { navController.popBackStack() } } }
                composable("parent/timetable") { Guarded(vm, navController) { TimetableScreen(vm, settings) { navController.popBackStack() } } }
                composable("parent/calendar") { Guarded(vm, navController) { CalendarScreen(vm, settings) { navController.popBackStack() } } }
                composable("parent/overrides") { Guarded(vm, navController) { OverridesScreen(vm) { navController.popBackStack() } } }
                composable("parent/backup") { Guarded(vm, navController) { BackupScreen(vm) { navController.popBackStack() } } }
            }
        }

        CelebrationBurst(trigger = vm.cheer)
    }

    when (pinDialog) {
        "create" -> CreatePinDialog(
            onDone = { vm.unlockParent(); vm.createPin(it); pinDialog = null; navController.navigate(Routes.PARENT) { launchSingleTop = true } },
            onDismiss = { pinDialog = null },
        )
        "enter" -> EnterPinDialog(
            vm,
            onSuccess = { vm.unlockParent(); pinDialog = null; navController.navigate(Routes.PARENT) { launchSingleTop = true } },
            onForgot = { pinDialog = "forgot" },
            onDismiss = { pinDialog = null },
        )
        "forgot" -> ConfirmResetDialog(
            "Without the PIN the only way in is to erase the whole app: subjects, timetable, homework and exams.",
            onConfirm = { vm.resetEverything(); pinDialog = null },
            onDismiss = { pinDialog = null },
        )
    }
}

/** Parent sub-screens bounce back out if the parent area has been locked in the meantime. */
@Composable
private fun Guarded(vm: MainViewModel, nav: androidx.navigation.NavController, content: @Composable () -> Unit) {
    if (vm.parentUnlocked) content() else LaunchedEffect(Unit) { nav.popBackStack(Routes.SCHEDULE, false) }
}

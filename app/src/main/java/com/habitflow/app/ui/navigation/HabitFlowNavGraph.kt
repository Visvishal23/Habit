package com.habitflow.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.habitflow.app.HabitFlowApplication
import com.habitflow.app.ui.ViewModelFactory
import com.habitflow.app.ui.calendar.CalendarScreen
import com.habitflow.app.ui.calendar.CalendarViewModel
import com.habitflow.app.ui.createhabit.CreateHabitScreen
import com.habitflow.app.ui.createhabit.CreateHabitViewModel
import com.habitflow.app.ui.habitdetail.HabitDetailScreen
import com.habitflow.app.ui.habitdetail.HabitDetailViewModel
import com.habitflow.app.ui.home.HomeScreen
import com.habitflow.app.ui.home.HomeViewModel
import com.habitflow.app.ui.journal.AddJournalEntryScreen
import com.habitflow.app.ui.journal.AddJournalEntryViewModel
import com.habitflow.app.ui.journal.JournalScreen
import com.habitflow.app.ui.journal.JournalViewModel
import com.habitflow.app.ui.onboarding.OnboardingScreen
import com.habitflow.app.ui.settings.SettingsScreen
import com.habitflow.app.ui.settings.SettingsViewModel
import com.habitflow.app.ui.statistics.StatisticsScreen
import com.habitflow.app.ui.statistics.StatisticsViewModel
import kotlinx.coroutines.launch

private val bottomNavIcons = mapOf(
    Screen.Home.route to Icons.Filled.Home,
    Screen.Calendar.route to Icons.Filled.CalendarMonth,
    Screen.Statistics.route to Icons.Filled.BarChart,
    Screen.Journal.route to Icons.Filled.MenuBook,
    Screen.Settings.route to Icons.Filled.Settings
)

private val bottomNavLabels = mapOf(
    Screen.Home.route to "Home",
    Screen.Calendar.route to "Calendar",
    Screen.Statistics.route to "Statistics",
    Screen.Journal.route to "Journal",
    Screen.Settings.route to "Settings"
)

@Composable
fun HabitFlowNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    factory: ViewModelFactory
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(bottomNavIcons[screen.route]!!, contentDescription = bottomNavLabels[screen.route]) },
                            label = { Text(bottomNavLabels[screen.route]!!) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(if (showBottomBar) padding else PaddingValues(0.dp))
        ) {
            composable(Screen.Onboarding.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                OnboardingScreen(onFinished = {
                    val app = context.applicationContext as HabitFlowApplication
                    scope.launch { app.settingsRepository.setOnboardingCompleted(true) }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Home.route) {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    onHabitClick = { id -> navController.navigate(Screen.HabitDetail.build(id)) },
                    onCreateHabit = { navController.navigate(Screen.CreateHabit.new()) }
                )
            }

            composable(Screen.Calendar.route) {
                val vm: CalendarViewModel = viewModel(factory = factory)
                CalendarScreen(vm)
            }

            composable(Screen.Statistics.route) {
                val vm: StatisticsViewModel = viewModel(factory = factory)
                StatisticsScreen(vm)
            }

            composable(Screen.Journal.route) {
                val vm: JournalViewModel = viewModel(factory = factory)
                JournalScreen(
                    viewModel = vm,
                    onAddEntry = { habitId -> navController.navigate(Screen.AddJournalEntry.build(habitId)) }
                )
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm)
            }

            composable(
                route = Screen.CreateHabit.route,
                arguments = listOf(navArgument(Screen.CreateHabit.ARG_HABIT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { entry ->
                val habitId = entry.arguments?.getLong(Screen.CreateHabit.ARG_HABIT_ID) ?: -1L
                val vm: CreateHabitViewModel = viewModel(factory = factory)
                CreateHabitScreen(
                    habitId = habitId,
                    viewModel = vm,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.HabitDetail.route,
                arguments = listOf(navArgument(Screen.HabitDetail.ARG_HABIT_ID) { type = NavType.LongType })
            ) { entry ->
                val habitId = entry.arguments?.getLong(Screen.HabitDetail.ARG_HABIT_ID) ?: -1L
                val vm: HabitDetailViewModel = viewModel(factory = factory)
                HabitDetailScreen(
                    habitId = habitId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.CreateHabit.edit(id)) },
                    onOpenJournal = { navController.navigate(Screen.Journal.route) }
                )
            }

            composable(
                route = Screen.AddJournalEntry.route,
                arguments = listOf(navArgument(Screen.AddJournalEntry.ARG_HABIT_ID) { type = NavType.LongType })
            ) { entry ->
                val habitId = entry.arguments?.getLong(Screen.AddJournalEntry.ARG_HABIT_ID) ?: -1L
                val vm: AddJournalEntryViewModel = viewModel(factory = factory)
                AddJournalEntryScreen(
                    habitId = habitId,
                    viewModel = vm,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

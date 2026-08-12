package com.healthdiary.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.healthdiary.app.ui.screens.body.BodyScreen
import com.healthdiary.app.ui.screens.diary.DiaryScreen
import com.healthdiary.app.ui.screens.diet.DietScreen
import com.healthdiary.app.ui.screens.settings.SettingsScreen
import com.healthdiary.app.ui.screens.today.TodayScreen
import com.healthdiary.app.ui.screens.tutor.TutorScreen
import com.healthdiary.app.ui.screens.workout.WorkoutDayScreen
import com.healthdiary.app.ui.screens.workout.WorkoutEditScreen
import com.healthdiary.app.ui.screens.workout.WorkoutScreen

sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Today : Destination("today", "今日", Icons.Outlined.Today)
    data object Workout : Destination("workout", "训练", Icons.Outlined.FitnessCenter)
    data object Diet : Destination("diet", "饮食", Icons.Outlined.Restaurant)
    data object Diary : Destination("diary", "日记", Icons.AutoMirrored.Outlined.MenuBook)
    data object Tutor : Destination("tutor", "家教", Icons.Outlined.School)
    data object Body : Destination("body", "身体", Icons.Outlined.MonitorWeight)
    data object Settings : Destination("settings", "设置", Icons.Outlined.Settings)

    object WorkoutEdit {
        const val route = "workout_edit?sessionId={sessionId}"
        fun create(sessionId: Long = -1L) = "workout_edit?sessionId=$sessionId"
    }

    object WorkoutDay {
        const val route = "workout_day?date={date}"
        fun create(date: String) = "workout_day?date=$date"
    }

    companion object {
        val bottomDestinations = listOf(Today, Workout, Diet, Diary, Tutor, Body)
        val bottomBarRoutes = bottomDestinations.map { it.route }
    }
}

@Composable
fun HealthDiaryNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Today.route,
        modifier = modifier
    ) {
        composable(Destination.Today.route) {
            TodayScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Destination.Workout.route) {
            WorkoutScreen(
                onStartWorkout = {
                    navController.navigate(Destination.WorkoutEdit.create())
                },
                onOpenDay = { date ->
                    navController.navigate(Destination.WorkoutDay.create(date))
                }
            )
        }
        composable(
            route = Destination.WorkoutDay.route,
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val date = entry.arguments?.getString("date") ?: ""
            WorkoutDayScreen(
                date = date,
                onBack = { navController.popBackStack() },
                onEditWorkout = { sessionId ->
                    navController.navigate(Destination.WorkoutEdit.create(sessionId))
                }
            )
        }
        composable(Destination.Diet.route) {
            DietScreen()
        }
        composable(Destination.Diary.route) {
            DiaryScreen()
        }
        composable(Destination.Tutor.route) {
            TutorScreen()
        }
        composable(Destination.Body.route) {
            BodyScreen()
        }
        composable(Destination.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Destination.WorkoutEdit.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            WorkoutEditScreen(
                sessionId = entry.arguments?.getLong("sessionId") ?: -1L,
                onDone = { navController.popBackStack() }
            )
        }
    }
}

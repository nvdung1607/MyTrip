package com.example.mytrip.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mytrip.ui.screens.expense.ExpenseScreen
import com.example.mytrip.ui.screens.home.HomeScreen
import com.example.mytrip.ui.screens.itinerary.ItineraryScreen
import com.example.mytrip.ui.screens.notes.AddNoteScreen
import com.example.mytrip.ui.screens.summary.SummaryScreen
import com.example.mytrip.ui.screens.today.TodayScreen
import com.example.mytrip.ui.screens.trip.CreateEditTripScreen
import com.example.mytrip.ui.screens.trip.TripDetailScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateTrip : Screen("create_trip")
    object EditTrip : Screen("edit_trip/{tripId}") {
        fun createRoute(tripId: Long) = "edit_trip/$tripId"
    }
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }
    object Itinerary : Screen("itinerary/{tripId}") {
        fun createRoute(tripId: Long) = "itinerary/$tripId"
    }
    object Today : Screen("today/{tripId}") {
        fun createRoute(tripId: Long) = "today/$tripId"
    }
    object AddNote : Screen("add_note/{tripId}?dayId={dayId}") {
        fun createRoute(tripId: Long, dayId: Long? = null) =
            if (dayId != null) "add_note/$tripId?dayId=$dayId"
            else "add_note/$tripId?dayId=-1"
    }
    object Expense : Screen("expense/{tripId}") {
        fun createRoute(tripId: Long) = "expense/$tripId"
    }
    object Summary : Screen("summary/{tripId}") {
        fun createRoute(tripId: Long) = "summary/$tripId"
    }
}

@Composable
fun MyTripNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.CreateTrip.route) {
            CreateEditTripScreen(navController = navController, tripId = null)
        }

        composable(
            route = Screen.EditTrip.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            CreateEditTripScreen(navController = navController, tripId = tripId)
        }

        composable(
            route = Screen.TripDetail.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            TripDetailScreen(navController = navController, tripId = tripId)
        }

        composable(
            route = Screen.Itinerary.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            ItineraryScreen(navController = navController, tripId = tripId)
        }

        composable(
            route = Screen.Today.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            TodayScreen(navController = navController, tripId = tripId)
        }

        composable(
            route = Screen.AddNote.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType },
                navArgument("dayId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            val dayId = backStack.arguments?.getLong("dayId").let { if (it == -1L) null else it }
            AddNoteScreen(navController = navController, tripId = tripId, dayId = dayId)
        }

        composable(
            route = Screen.Expense.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            ExpenseScreen(navController = navController, tripId = tripId)
        }

        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStack ->
            val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
            SummaryScreen(navController = navController, tripId = tripId)
        }
    }
}

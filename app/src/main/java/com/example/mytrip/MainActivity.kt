package com.example.mytrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.navigation.MyTripNavGraph
import com.example.mytrip.navigation.Screen
import com.example.mytrip.ui.theme.MyTripTheme

class MainActivity : ComponentActivity() {

    // Whether this is a fresh cold start (not rotation/config change)
    private var isColdStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // savedInstanceState is non-null on config changes (rotation etc.)
        val coldStart = savedInstanceState == null
        enableEdgeToEdge()
        setContent {
            MyTripTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val app = applicationContext as MyTripApplication
                    val allTrips by app.repository.getAllTrips()
                        .collectAsState(initial = null)

                    // Only auto-navigate once on cold start, after trips are loaded
                    var navigated by remember { mutableStateOf(false) }
                    LaunchedEffect(allTrips) {
                        if (!navigated && coldStart && allTrips != null) {
                            navigated = true
                            val ongoingTrip = allTrips!!.firstOrNull { it.status == TripStatus.ONGOING }
                            if (ongoingTrip != null) {
                                navController.navigate(Screen.Today.createRoute(ongoingTrip.id)) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                }
                            }
                        }
                    }

                    MyTripNavGraph(navController = navController)
                }
            }
        }
    }
}
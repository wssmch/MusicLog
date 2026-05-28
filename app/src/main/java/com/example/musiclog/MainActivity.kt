package com.example.musiclog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musiclog.navigation.NavRoute
import com.example.musiclog.ui.dashboard.DashboardScreen
import com.example.musiclog.ui.ranking.RankScreen
import com.example.musiclog.ui.search.SearchScreen
import com.example.musiclog.viewmodel.DashboardViewModel
import com.example.musiclog.viewmodel.RankViewModel
import com.example.musiclog.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val rankViewModel: RankViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavRoute.Dashboard.route
                    ) {
                        composable(NavRoute.Dashboard.route) {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToSearch = {
                                    navController.navigate(NavRoute.Search.route)
                                },
                                onNavigateToRanking = {
                                    navController.navigate(NavRoute.Ranking.route)
                                }
                            )
                        }

                        composable(NavRoute.Search.route) {
                            SearchScreen(
                                viewModel = searchViewModel
                            )
                        }

                        composable(NavRoute.Ranking.route) {
                            RankScreen(
                                viewModel = rankViewModel,
                                onNavigateToDashboard = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
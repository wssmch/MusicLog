package com.example.musiclog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musiclog.navigation.NavRoute
import com.example.musiclog.ui.dashboard.DashboardScreen
import com.example.musiclog.ui.ranking.RankScreen
import com.example.musiclog.ui.playlist.PlaylistScreen
import com.example.musiclog.ui.playlist.PlaylistDetailScreen
import com.example.musiclog.ui.search.SearchScreen
import com.example.musiclog.viewmodel.DashboardViewModel
import com.example.musiclog.viewmodel.RankViewModel
import com.example.musiclog.viewmodel.SearchViewModel
import com.example.musiclog.viewmodel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels() // 💡 해결: 유실되었던 서치 뷰모델 변수 복구
    private val rankViewModel: RankViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MusicLogAppNavigation(
                        dashboardViewModel = dashboardViewModel,
                        searchViewModel = searchViewModel, // 💡 해결: 네비게이션 트리거 전송 구조에 결합
                        rankViewModel = rankViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MusicLogAppNavigation(
    dashboardViewModel: DashboardViewModel,
    rankViewModel: RankViewModel,
    searchViewModel: SearchViewModel,
    navController: NavHostController = rememberNavController()
) {
    val playlistDetailRouteBase = "PlaylistDetail"

    NavHost(
        navController = navController,
        startDestination = NavRoute.Dashboard.route
    ) {
        composable(NavRoute.Dashboard.route) {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                onNavigateToSearch = { navController.navigate(NavRoute.Search.route)},
                onNavigateToRanking = { navController.navigate(NavRoute.Ranking.route) },
                onNavigateToPlayCount = { },
                onNavigateToPlaylist = { navController.navigate(NavRoute.Playlist.route) }
            )
        }
        composable(NavRoute.Search.route) {
            SearchScreen(viewModel = searchViewModel) // 💡 의존성 주입 연결 완공
        }
        composable(NavRoute.Ranking.route) {
            RankScreen(viewModel = rankViewModel, onNavigateToDashboard = { navController.popBackStack() })
        }

        composable(NavRoute.Playlist.route) {
            val playlistViewModel: PlaylistViewModel = hiltViewModel()
            PlaylistScreen(
                viewModel = playlistViewModel,
                onNavigateToDashboard = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("$playlistDetailRouteBase/$id") } // 💡 정상 매핑 가동
            )
        }

        composable(
            route = "$playlistDetailRouteBase/{playlistId}",
            arguments = listOf(
                androidx.navigation.navArgument("playlistId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId").orEmpty()
            val playlistViewModel: PlaylistViewModel = hiltViewModel()
            PlaylistDetailScreen(
                playlistId = playlistId,
                viewModel = playlistViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
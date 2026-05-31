package com.example.musiclog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.musiclog.ui.auth.AuthScreen // 💡 신규 화면 컴포저블 임포트
import com.example.musiclog.ui.dashboard.DashboardScreen
import com.example.musiclog.ui.playcount.PlayCountScreen
import com.example.musiclog.ui.ranking.RankScreen
import com.example.musiclog.ui.playlist.PlaylistScreen
import com.example.musiclog.ui.playlist.PlaylistDetailScreen
import com.example.musiclog.ui.search.SearchScreen
import com.example.musiclog.viewmodel.DashboardViewModel
import com.example.musiclog.viewmodel.PlayCountViewModel
import com.example.musiclog.viewmodel.RankViewModel
import com.example.musiclog.viewmodel.SearchViewModel
import com.example.musiclog.viewmodel.PlaylistViewModel
import com.google.firebase.auth.FirebaseAuth // 💡 세션 런타임 조회를 위한 인증 엔진 참조 추가
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val rankViewModel: RankViewModel by viewModels()

    private val playCountViewModel : PlayCountViewModel by viewModels()

    private val playlistViewModel : PlaylistViewModel by viewModels()

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
                        searchViewModel = searchViewModel,
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

    // 💡 해결: 세션 검증 후 토큰 유효 유무에 따라 최초 목적지(startDestination)를 동적 분기하는 라우팅 메커니즘 연동
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestinationRoute = if (currentUser != null) NavRoute.Dashboard.route else NavRoute.Auth.route

    NavHost(
        navController = navController,
        startDestination = startDestinationRoute
    ) {
        // 💡 인증 요구 스크린 셋 노드 추가
        composable(NavRoute.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Auth.route) { inclusive = true } // 백스택에서 인증화면 삭제처리 크래시 방지
                    }
                }
            )
        }

        composable(NavRoute.Dashboard.route) {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                onNavigateToSearch = { navController.navigate(NavRoute.Search.route)},
                onNavigateToRanking = { navController.navigate(NavRoute.Ranking.route) },
                onNavigateToPlayCount = { navController.navigate(NavRoute.PlayCount.route)},
                onNavigateToPlaylist = { navController.navigate(NavRoute.Playlist.route) }
            )
        }
        composable(NavRoute.Search.route) {
            SearchScreen(viewModel = searchViewModel)
        }
        composable(NavRoute.Ranking.route) {
            RankScreen(viewModel = rankViewModel, onNavigateToDashboard = { navController.popBackStack() })
        }

        composable(NavRoute.Playlist.route) {
            val playlistViewModel: PlaylistViewModel = hiltViewModel()
            PlaylistScreen(
                viewModel = playlistViewModel,
                onNavigateToDashboard = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("$playlistDetailRouteBase/$id") }
            )
        }
        composable(NavRoute.PlayCount.route) {
            val playCountViewModel: PlayCountViewModel = hiltViewModel()
            PlayCountScreen(
                viewModel = playCountViewModel,
                onNavigateBack = { navController.popBackStack() }
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
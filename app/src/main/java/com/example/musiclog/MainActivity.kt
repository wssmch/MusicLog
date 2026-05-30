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
import androidx.navigation.NavHostController
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

        // 시스템 하단바(네비게이션 바) 숨기기 및 Edge-to-Edge 설정
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
    searchViewModel: SearchViewModel,
    rankViewModel: RankViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Dashboard.route
    ) {
        // 1. 진입 화면 (상단 3*3 고정 및 하단 이동 버튼 체계)
        composable(NavRoute.Dashboard.route) {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                onNavigateToSearch = { navController.navigate(NavRoute.Search.route) },
                onNavigateToRanking = { navController.navigate(NavRoute.Ranking.route) },
                onNavigateToPlayCount = { navController.navigate(NavRoute.PlayCount.route) },
                onNavigateToPlaylist = { navController.navigate(NavRoute.Playlist.route) }
            )
        }

        // 2. 검색 화면
        composable(NavRoute.Search.route) {
            SearchScreen(viewModel = searchViewModel) // search스크린에 뷰모델을 넘겨줌
        }

        // 3. 랭킹 독립 화면
        composable(NavRoute.Ranking.route) {
            RankScreen(
                viewModel = rankViewModel,
                onNavigateToDashboard = { navController.popBackStack() }
            )
        }

        // 4. 재생횟수 통계 독립 화면
        composable(NavRoute.PlayCount.route) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("재생횟수 분석 및 통계 리포트 화면")
                }
            }
        }

        // 5. 재생목록 독립 화면
        composable(NavRoute.Playlist.route) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("내 전체 재생목록 목록 뷰 화면")
                }
            }
        }
    }
}
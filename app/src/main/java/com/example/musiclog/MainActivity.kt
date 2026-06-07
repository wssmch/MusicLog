package com.example.musiclog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.musiclog.navigation.NavRoute
import com.example.musiclog.ui.auth.AuthScreen
import com.example.musiclog.ui.components.MiniPlayer
import com.example.musiclog.ui.dashboard.DashboardScreen
import com.example.musiclog.ui.playcount.PlayCountScreen
import com.example.musiclog.ui.ranking.RankScreen
import com.example.musiclog.ui.playlist.PlaylistScreen
import com.example.musiclog.ui.playlist.PlaylistDetailScreen
import com.example.musiclog.ui.search.SearchScreen
import com.example.musiclog.viewmodel.DashboardViewModel
import com.example.musiclog.viewmodel.PlayCountViewModel
import com.example.musiclog.viewmodel.PlayerViewModel
import com.example.musiclog.viewmodel.RankViewModel
import com.example.musiclog.viewmodel.SearchViewModel
import com.example.musiclog.viewmodel.PlaylistViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val rankViewModel: RankViewModel by viewModels()
    // 💡 해결: 하위 라우팅에서 hiltViewModel()로 개별 주입 중이므로, 미사용 전역 뷰모델 선언 삭제

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLogAppNavigation(
    dashboardViewModel: DashboardViewModel,
    rankViewModel: RankViewModel,
    searchViewModel: SearchViewModel,
    navController: NavHostController = rememberNavController()
) {
    val playlistDetailRouteBase = "PlaylistDetail"
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestinationRoute =
        if (currentUser != null) NavRoute.Dashboard.route else NavRoute.Auth.route

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != NavRoute.Auth.route

    var showQueueDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val uriHandler = LocalUriHandler.current

        val playAndLogMusic = { music: com.example.musiclog.domain.model.Music, queue: List<com.example.musiclog.domain.model.Music> ->
            playerViewModel.playMusic(music, queue)
            searchViewModel.insertMusicToLog(music)
            uriHandler.openUri("https://www.youtube.com/watch?v=${music.id}")
        }

        if (showQueueDialog && showBottomBar) {
            AlertDialog(
                onDismissRequest = { showQueueDialog = false },
                title = { Text("현재 재생 대기열", fontWeight = FontWeight.Bold) },
                text = {
                    if (playerState.queue.isEmpty()) {
                        Text("대기열이 비어 있습니다.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playerState.queue) { music ->
                                val isCurrent = music.id == playerState.currentMusic?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // 💡 해결: 존재하지 않는 RowDefaults.shape 대신 명시적 둥근 모서리 적용
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showQueueDialog = false
                                            playAndLogMusic(music, playerState.queue)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = music.albumArtUrl,
                                        contentDescription = null,
                                        // 💡 해결: 동일하게 RoundedCornerShape 적용
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = music.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = music.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurrent) {
                                        Text(
                                            text = "재생 중",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQueueDialog = false }) {
                        Text("닫기")
                    }
                }
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    MiniPlayer(
                        music = playerState.currentMusic,
                        onPreviousClick = { playerViewModel.skipToPrevious() },
                        onNextClick = { playerViewModel.skipToNext() },
                        onPlayerClick = { showQueueDialog = true }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestinationRoute,
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(NavRoute.Auth.route) {
                    AuthScreen(
                        onAuthSuccess = {
                            navController.navigate(NavRoute.Dashboard.route) {
                                popUpTo(NavRoute.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(NavRoute.Dashboard.route) {
                    DashboardScreen(
                        dashboardViewModel = dashboardViewModel,
                        onNavigateToSearch = { navController.navigate(NavRoute.Search.route) },
                        onNavigateToRanking = { navController.navigate(NavRoute.Ranking.route) },
                        onNavigateToPlayCount = { navController.navigate(NavRoute.PlayCount.route) },
                        onNavigateToPlaylist = { navController.navigate(NavRoute.Playlist.route) },
                        onPlayMusic = { music, queue -> playAndLogMusic(music, queue) }
                    )
                }

                composable(NavRoute.Search.route) {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onItemClick = { music ->
                            playAndLogMusic(music, searchViewModel.searchResults.value)
                        }
                    )
                }

                composable(NavRoute.Ranking.route) {
                    RankScreen(
                        viewModel = rankViewModel,
                        onNavigateToDashboard = { navController.popBackStack() })
                }

                composable(NavRoute.Playlist.route) {
                    val scopedPlaylistViewModel: PlaylistViewModel = hiltViewModel()
                    PlaylistScreen(
                        viewModel = scopedPlaylistViewModel,
                        onNavigateToDashboard = { navController.popBackStack() },
                        onNavigateToDetail = { id -> navController.navigate("$playlistDetailRouteBase/$id") },
                        onPlayMusic = { music, queue -> playAndLogMusic(music, queue) }
                    )
                }

                composable(NavRoute.PlayCount.route) {
                    val scopedPlayCountViewModel: PlayCountViewModel = hiltViewModel()
                    PlayCountScreen(
                        viewModel = scopedPlayCountViewModel,
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
                    val scopedPlaylistViewModel: PlaylistViewModel = hiltViewModel()
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        viewModel = scopedPlaylistViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPlayMusic = { music, queue -> playAndLogMusic(music, queue) }
                    )
                }
            }
        }
    }
}
package com.example.musiclog.navigation

//화면 전환 로직


    sealed class NavRoute(val route: String) {
        object Dashboard : NavRoute("dashboard")
        object Search : NavRoute("search")
        object Ranking : NavRoute("ranking")
        object PlayCount : NavRoute("play_count") // 재생횟수(리캡) 화면 경로 추가
        object Playlist : NavRoute("playlist")   // 재생목록 전체 화면 경로 추가
    }

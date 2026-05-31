package com.example.musiclog.navigation


sealed class NavRoute(val route: String) {

    object Auth : NavRoute("auth") // 💡 인증 및 회원가입 화면 경로 신규 세그먼트 추가
    object Dashboard : NavRoute("dashboard")
    object Search : NavRoute("search")
    object Ranking : NavRoute("ranking")

    object PlayCount : NavRoute("play_count") // 재생횟수(리캡) 화면 경로 추가
    object Playlist : NavRoute("playlist") // 재생목록 전체 화면 경로 추가

}

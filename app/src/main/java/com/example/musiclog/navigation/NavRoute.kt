package com.example.musiclog.navigation

//화면 전환 로직

sealed class NavRoute(val route: String) {
    object Dashboard : NavRoute("dashboard")
    object Search : NavRoute("search")
    object Ranking : NavRoute("ranking")
}
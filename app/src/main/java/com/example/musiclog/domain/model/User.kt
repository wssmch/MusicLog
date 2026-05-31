package com.example.musiclog.domain.model

data class User(
    val email: String = "",
    val nickname: String = "",
    val createdAt: Long = 0L
)
package com.example.musiclog.utils

import android.content.Context

object AccountPrefsManager {
    private const val PREFS_NAME = "musiclog_test_accounts"

    // 로그인 성공 시 계정 정보 저장
    fun saveAccount(context: Context, email: String, pass: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(email, pass).apply()
    }

    // 저장된 모든 계정 정보 불러오기 (Map 형태로 반환: Key=Email, Value=Password)
    fun getSavedAccounts(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // SharedPreferences에 저장된 모든 Key-Value를 Map으로 변환
        return prefs.all.entries.associate { it.key to it.value.toString() }
    }
}
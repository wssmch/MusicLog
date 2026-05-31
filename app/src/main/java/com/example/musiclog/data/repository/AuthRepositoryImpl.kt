package com.example.musiclog.data.repository

import com.example.musiclog.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(emailId: String, password: String, nickname: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Firebase Authentication 계정 생성
                val authResult = auth.createUserWithEmailAndPassword(emailId, password).await()
                val uid = authResult.user?.uid ?: return@withContext false

                // 2. 가입 유저 메타데이터 생성
                val newUser = User(
                    email = emailId,
                    nickname = nickname,
                    createdAt = System.currentTimeMillis()
                )

                // 3. Firestore 직접 ID 방식(UID)으로 저장
                firestore.collection("users")
                    .document(uid)
                    .set(newUser)
                    .await()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
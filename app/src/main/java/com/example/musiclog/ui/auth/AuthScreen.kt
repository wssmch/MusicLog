package com.example.musiclog.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.musiclog.data.repository.AuthRepositoryImpl
import com.example.musiclog.utils.AccountPrefsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authRepository = remember { AuthRepositoryImpl() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    var isSignUpMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showAccountDialog by remember { mutableStateOf(false) }
    val savedAccounts = remember(showAccountDialog) {
        AccountPrefsManager.getSavedAccounts(context)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isSignUpMode) "MusicLog 회원가입" else "MusicLog 로그인",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("아이디 (이메일)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUpMode) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("닉네임") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (isSignUpMode && nickname.isBlank())) {
                        val blankMessage = "모든 필드를 기입하십시오."
                        errorMessage = blankMessage
                        Toast.makeText(context, blankMessage, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        if (isSignUpMode) {
                            try {
                                val success = authRepository.registerUser(email.trim(), password, nickname.trim())
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                                    // 💡 위치 1: 회원가입 성공 시 기기에 계정 정보 저장
                                    AccountPrefsManager.saveAccount(context, email.trim(), password)
                                    onAuthSuccess()
                                } else {
                                    val dbFailMessage = "인증 성공 후 프로필 적재에 실패했습니다. Firebase 콘솔 설정을 확인하십시오."
                                    errorMessage = dbFailMessage
                                    Toast.makeText(context, dbFailMessage, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: FirebaseAuthWeakPasswordException) {
                                isLoading = false
                                val weakMsg = "회원가입 실패: 비밀번호가 너무 취약합니다. (최소 6자리 이상)"
                                errorMessage = weakMsg
                                Toast.makeText(context, weakMsg, Toast.LENGTH_LONG).show()
                            } catch (e: FirebaseAuthInvalidCredentialsException) {
                                isLoading = false
                                val invalidMsg = "회원가입 실패: 올바른 이메일 형식이 아닙니다."
                                errorMessage = invalidMsg
                                Toast.makeText(context, invalidMsg, Toast.LENGTH_LONG).show()
                            } catch (e: FirebaseAuthUserCollisionException) {
                                isLoading = false
                                val collisionMsg = "회원가입 실패: 이미 가입된 이메일 계정입니다."
                                errorMessage = collisionMsg
                                Toast.makeText(context, collisionMsg, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                isLoading = false
                                val rawError = e.localizedMessage ?: "알 수 없는 인증 오류 발생"
                                val finalErrorMsg = if (rawError.contains("API key not allowed", ignoreCase = true)) {
                                    "Firebase 콘솔에서 이메일/비밀번호 로그인 제공업체를 활성화하십시오."
                                } else {
                                    "회원가입 오류: $rawError"
                                }
                                errorMessage = finalErrorMsg
                                Toast.makeText(context, finalErrorMsg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            try {
                                FirebaseAuth.getInstance()
                                    .signInWithEmailAndPassword(email.trim(), password)
                                    .await()
                                isLoading = false
                                Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                // 💡 위치 2: 기존 계정 로그인 성공 시 기기에 계정 정보 저장
                                AccountPrefsManager.saveAccount(context, email.trim(), password)
                                onAuthSuccess()
                            } catch (e: FirebaseAuthInvalidUserException) {
                                isLoading = false
                                val noUserMsg = "가입되지 않은 계정입니다. 먼저 회원가입을 진행해 주십시오."
                                errorMessage = noUserMsg
                                Toast.makeText(context, noUserMsg, Toast.LENGTH_LONG).show()
                            } catch (e: FirebaseAuthInvalidCredentialsException) {
                                isLoading = false
                                val invalidMsg = "이메일 또는 비밀번호가 일치하지 않습니다."
                                errorMessage = invalidMsg
                                Toast.makeText(context, invalidMsg, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                isLoading = false
                                val rawError = e.localizedMessage ?: "로그인 정보 불일치"
                                val finalErrorMsg = if (rawError.contains("API key not allowed", ignoreCase = true)) {
                                    "Firebase 콘솔에서 이메일/비밀번호 로그인 제공업체를 활성화하십시오."
                                } else {
                                    "로그인 실패: $rawError"
                                }
                                errorMessage = finalErrorMsg
                                Toast.makeText(context, finalErrorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isSignUpMode) "가입 완료" else "로그인")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { isSignUpMode = !isSignUpMode; errorMessage = null }) {
                Text(text = if (isSignUpMode) "기존 계정으로 로그인하기" else "처음이신가요? 회원가입하기")
            }

            if (savedAccounts.isNotEmpty()) {
                TextButton(
                    onClick = { showAccountDialog = true },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("저장된 계정으로 전환하여 로그인 (테스트용)", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // 💡 다이얼로그 (계정 선택 창) UI 및 원클릭 자동 로그인 로직 추가
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("계정 전환", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(savedAccounts.entries.toList()) { entry ->
                        val savedEmail = entry.key
                        val savedPass = entry.value

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                showAccountDialog = false
                                isLoading = true
                                // 저장된 계정으로 자동 로그인 시도
                                coroutineScope.launch {
                                    try {
                                        FirebaseAuth.getInstance()
                                            .signInWithEmailAndPassword(savedEmail, savedPass)
                                            .await()
                                        isLoading = false
                                        Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                        AccountPrefsManager.saveAccount(context, savedEmail, savedPass)
                                        onAuthSuccess()
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(context, "자동 로그인 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = savedEmail,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }
}
package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserRole
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.StudentPortalScreen
import com.example.ui.viewmodel.SchoolViewModel
import com.example.ui.viewmodel.SessionUiState

@Composable
fun OxfordSchoolApp(
    viewModel: SchoolViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val session = uiState.session) {
        SessionUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        SessionUiState.Unauthenticated -> {
            AuthScreen(
                isBusy = uiState.isBusy,
                errorMessage = uiState.errorMessage,
                onLogin = viewModel::login,
                onSignUp = viewModel::signUp,
                onDemoLogin = viewModel::demoLogin
            )
        }
        is SessionUiState.Authenticated -> {
            when (UserRole.fromKey(session.user.role)) {
                UserRole.ADMIN -> AdminPortalScreen(
                    user = session.user,
                    uiState = uiState,
                    viewModel = viewModel
                )
                UserRole.STUDENT_PARENT -> StudentPortalScreen(
                    user = session.user,
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

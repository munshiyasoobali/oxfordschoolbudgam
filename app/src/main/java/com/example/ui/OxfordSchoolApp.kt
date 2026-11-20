package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PortalIntent
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LinkStudentProfileScreen
import com.example.ui.screens.StudentPortalScreen
import com.example.ui.theme.OxfordNavy
import com.example.ui.viewmodel.PortalDestination
import com.example.ui.viewmodel.SchoolViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Composable
fun OxfordSchoolApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var activePortalIntent by remember { mutableStateOf(PortalIntent.PARENT_STUDENT) }
    var authBannerError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            externalError = authBannerError,
            onClearExternalError = { authBannerError = null },
            onSignInSuccess = { intent ->
                activePortalIntent = intent
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        val context = LocalContext.current
        val credentialManager = remember(context) { CredentialManager.create(context) }

        key(uid, activePortalIntent) {
            val viewModel: SchoolViewModel = viewModel(
                key = "${uid}_${activePortalIntent.name}",
                factory = SchoolViewModel.Factory(
                    appContext = context.applicationContext,
                    currentUserId = uid,
                    portalIntent = activePortalIntent
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val handleSignOut: () -> Unit = {
                viewModel.signOut(credentialManager) { reason ->
                    if (!reason.isNullOrBlank()) {
                        authBannerError = reason
                    }
                    activePortalIntent = PortalIntent.PARENT_STUDENT
                }
            }

            when (val dest = uiState.destination) {
                PortalDestination.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = OxfordNavy)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Verifying Oxford School Budgam access...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is PortalDestination.AccessDenied -> {
                    LaunchedEffect(dest.message) {
                        authBannerError = dest.message
                        viewModel.signOut(credentialManager) {
                            activePortalIntent = PortalIntent.PARENT_STUDENT
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OxfordNavy)
                    }
                }
                is PortalDestination.Management -> {
                    AdminPortalScreen(
                        adminDisplayName = dest.config.adminName.ifBlank { user.displayName ?: "School Management" },
                        uiState = uiState,
                        viewModel = viewModel,
                        onLogout = handleSignOut
                    )
                }
                PortalDestination.NeedsStudentLink -> {
                    LinkStudentProfileScreen(
                        googleEmail = user.email.orEmpty(),
                        googleName = user.displayName.orEmpty(),
                        isBusy = uiState.isBusy,
                        errorMessage = uiState.errorMessage,
                        onLinkStudent = viewModel::linkParentToStudent,
                        onSignOut = handleSignOut
                    )
                }
                is PortalDestination.Student -> {
                    StudentPortalScreen(
                        userDisplayName = dest.userProfile.displayName.ifBlank { user.displayName ?: "Parent / Student" },
                        uiState = uiState,
                        viewModel = viewModel,
                        onLogout = handleSignOut
                    )
                }
                is PortalDestination.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Unable to connect to School Portal",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = dest.message,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { activePortalIntent = PortalIntent.PARENT_STUDENT },
                                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy)
                            ) {
                                Text("Retry")
                            }
                            OutlinedButton(onClick = handleSignOut) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
        }
    }
}

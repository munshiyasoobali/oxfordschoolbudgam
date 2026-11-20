package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SeedData
import com.example.data.UserRole
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.HeritageGoldContainer
import com.example.ui.theme.HeritageGoldOnContainer
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.OxfordNavyContainer

@Composable
fun AuthScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onLogin: (identifier: String, password: String) -> Unit,
    onSignUp: (fullName: String, role: UserRole, rollNo: String, identifier: String, password: String) -> Unit,
    onDemoLogin: (UserRole) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Login, 1 = Sign Up

    // Login state
    var loginIdentifier by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var loginPassVisible by rememberSaveable { mutableStateOf(false) }

    // Sign-up state
    var fullName by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.STUDENT_PARENT) }
    var rollNumber by rememberSaveable { mutableStateOf("") }
    var signUpIdentifier by rememberSaveable { mutableStateOf("") }
    var signUpPassword by rememberSaveable { mutableStateOf("") }
    var signUpPassVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Campus Hero Banner with Crest
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_school_banner),
                    contentDescription = "Oxford School Education Budgam Campus",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    OxfordNavy.copy(alpha = 0.55f),
                                    OxfordNavy.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OXFORD SCHOOL EDUCATION BUDGAM",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Budgam Academic Portal • Attendance, Fees & School Diary",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HeritageGold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Overlapping Crest Emblem
            Image(
                painter = painterResource(id = R.drawable.img_launcher_crest),
                contentDescription = "School Crest",
                modifier = Modifier
                    .offset(y = (-34).dp)
                    .size(68.dp)
                    .clip(CircleShape)
                    .border(3.dp, HeritageGold, CircleShape),
                contentScale = ContentScale.Crop
            )

            Card(
                modifier = Modifier
                    .offset(y = (-18).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.testTag("auth_tab_login"),
                            text = { Text("Sign In", fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("auth_tab_signup"),
                            text = { Text("Sign Up", fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                )
                            }
                        }

                        if (selectedTab == 0) {
                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = { loginIdentifier = it },
                                label = { Text("Email or Phone Number") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_identifier_input")
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { loginPassVisible = !loginPassVisible }) {
                                        Icon(
                                            imageVector = if (loginPassVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )

                            Button(
                                onClick = { onLogin(loginIdentifier, loginPassword) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy)
                            ) {
                                Text("Sign In to Portal", fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Instant Demo Login Quick-Fill Section
                            Surface(
                                color = HeritageGoldContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Quick Demo Access",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = HeritageGoldOnContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Test either portal immediately with pre-loaded students, 30-day attendance, Rs fee invoices & diary.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HeritageGoldOnContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                loginIdentifier = SeedData.DEMO_ADMIN_EMAIL
                                                loginPassword = SeedData.DEMO_ADMIN_PASS
                                                onDemoLogin(UserRole.ADMIN)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("demo_admin_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AdminPanelSettings,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Admin Demo", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                loginIdentifier = SeedData.DEMO_STUDENT_EMAIL
                                                loginPassword = SeedData.DEMO_STUDENT_PASS
                                                onDemoLogin(UserRole.STUDENT_PARENT)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("demo_student_button"),
                                            border = BorderStroke(1.5.dp, OxfordNavy),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OxfordNavy)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.School,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Student Demo", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Sign Up Form
                            Text(
                                text = "Select Portal Role",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = selectedRole == UserRole.STUDENT_PARENT,
                                    onClick = { selectedRole = UserRole.STUDENT_PARENT },
                                    label = { Text("Student / Parent") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OxfordNavyContainer,
                                        selectedLabelColor = OxfordNavy
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_student_chip")
                                )
                                FilterChip(
                                    selected = selectedRole == UserRole.ADMIN,
                                    onClick = { selectedRole = UserRole.ADMIN },
                                    label = { Text("Management") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OxfordNavyContainer,
                                        selectedLabelColor = OxfordNavy
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_admin_chip")
                                )
                            }

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name_input")
                            )

                            AnimatedVisibility(visible = selectedRole == UserRole.STUDENT_PARENT) {
                                OutlinedTextField(
                                    value = rollNumber,
                                    onValueChange = { rollNumber = it },
                                    label = { Text("Roll Number / Admission ID (e.g. OXB-101)") },
                                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_roll_input")
                                )
                            }

                            OutlinedTextField(
                                value = signUpIdentifier,
                                onValueChange = { signUpIdentifier = it },
                                label = { Text("Email or Mobile Number") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_identifier_input")
                            )

                            OutlinedTextField(
                                value = signUpPassword,
                                onValueChange = { signUpPassword = it },
                                label = { Text("Create Password (4+ chars)") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { signUpPassVisible = !signUpPassVisible }) {
                                        Icon(
                                            imageVector = if (signUpPassVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (signUpPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_password_input")
                            )

                            Button(
                                onClick = {
                                    onSignUp(
                                        fullName,
                                        selectedRole,
                                        rollNumber,
                                        signUpIdentifier,
                                        signUpPassword
                                    )
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("signup_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy)
                            ) {
                                Text("Create Account & Enter", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

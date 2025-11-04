package com.example.neighborhoodhelper.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra("username") ?: ""

        setContent {
            ForgotPasswordFlow(
                username = username,
                onBackClick = { finish() },
                onFetchUserData = {
                    // TODO: Implement backend API call to fetch user's registered email and phone
                    // Example API: getUserRecoveryOptions(username)
                    // This should return both masked and full email/phone

                    // For now, return mock data
                    UserRecoveryData(
                        maskedEmail = "j*******o@gmail.com",
                        maskedPhone = "+880 *******89",
                        fullEmail = "john.doe@gmail.com",
                        fullPhone = "+8801234567889"
                    )
                },
                onSendOTP = { method, value ->
                    val intent = Intent(this, OTPVerificationActivity::class.java).apply {
                        putExtra("verification_method", method)
                        putExtra("verification_value", value)
                        putExtra("verification_purpose", "forgot_password")
                    }
                    startActivity(intent)
                }
            )
        }
    }
}

data class UserRecoveryData(
    val maskedEmail: String,
    val maskedPhone: String,
    val fullEmail: String,
    val fullPhone: String
)

@OptIn(ExperimentalMaterial3Api::class)
private val forgotPasswordTextFieldColors
    @Composable
    get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF6C63FF),
        unfocusedBorderColor = Color(0xFFE5E5E5),
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DuplicatedCode")
fun ForgotPasswordFlow(
    username: String = "",
    onBackClick: () -> Unit = {},
    onFetchUserData: () -> UserRecoveryData = {
        UserRecoveryData("j*****o@gmail.com", "+880 *****89", "john@gmail.com", "+8801234567889")
    },
    onSendOTP: (method: String, value: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    var userRecoveryData by remember { mutableStateOf<UserRecoveryData?>(null) }
    var selectedMethod by remember { mutableStateOf("email") }
    var isLoading by remember { mutableStateOf(true) }
    var userInput by remember { mutableStateOf("") }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            val recoveryData = onFetchUserData()
            userRecoveryData = recoveryData
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        IconButton(
            onClick = {
                if (currentStep == 2) {
                    currentStep = 1
                    userInput = ""
                } else {
                    onBackClick()
                }
            },
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator(color = Color(0xFF6C63FF))
            }
        } else if (currentStep == 1) {
            // Step 1: Select recovery method
            Text(
                text = "Forgot Password?",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Choose where you want to receive the OTP",
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            userRecoveryData?.let { data ->
                if (data.maskedEmail.isNotEmpty()) {
                    MethodSelectionCard(
                        title = "Email",
                        subtitle = data.maskedEmail,
                        icon = Icons.Default.Email,
                        isSelected = selectedMethod == "email",
                        onClick = { selectedMethod = "email" }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (data.maskedPhone.isNotEmpty()) {
                    MethodSelectionCard(
                        title = "Phone Number",
                        subtitle = data.maskedPhone,
                        icon = Icons.Default.Phone,
                        isSelected = selectedMethod == "phone",
                        onClick = { selectedMethod = "phone" }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { currentStep = 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        } else {
            // Step 2: Enter full email/phone for verification
            Text(
                text = "Verify Your ${if (selectedMethod == "email") "Email" else "Phone Number"}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Please enter your complete ${if (selectedMethod == "email") "email address" else "phone number"} to verify it's you",
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = if (selectedMethod == "email") "Email Address" else "Phone Number",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = {
                    Text(if (selectedMethod == "email") "Enter your email" else "Enter your phone number")
                },
                shape = RoundedCornerShape(12.dp),
                colors = forgotPasswordTextFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (selectedMethod == "email") KeyboardType.Email else KeyboardType.Phone
                ),
                singleLine = true
            )

            Text(
                text = "We'll send an OTP to this ${if (selectedMethod == "email") "email" else "number"} for verification",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (userInput.isBlank()) {
                        Toast.makeText(
                            context,
                            "Please enter your ${if (selectedMethod == "email") "email" else "phone number"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    // Verify the input matches the actual data
                    val isValid = if (selectedMethod == "email") {
                        userInput.equals(userRecoveryData?.fullEmail, ignoreCase = true)
                    } else {
                        userInput.replace(Regex("[^0-9+]"), "") ==
                                userRecoveryData?.fullPhone?.replace(Regex("[^0-9+]"), "")
                    }

                    if (isValid) {
                        onSendOTP(selectedMethod, userInput)
                    } else {
                        Toast.makeText(
                            context,
                            "The ${if (selectedMethod == "email") "email" else "phone number"} doesn't match our records",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Send OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MethodSelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFF6C63FF) else Color(0xFFE5E5E5),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF5F4FF) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color(0xFF6C63FF) else Color(0xFFF5F5F5)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF6C63FF),
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordFlowPreview() {
    ForgotPasswordFlow(username = "test_user")
}
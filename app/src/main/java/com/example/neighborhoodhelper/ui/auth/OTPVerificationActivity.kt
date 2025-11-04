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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class OTPVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get data from Intent
        val verificationMethod = intent.getStringExtra("verification_method") ?: "phone"
        val verificationValue = intent.getStringExtra("verification_value") ?: ""
        val verificationPurpose = intent.getStringExtra("verification_purpose") ?: "registration"

        // Legacy support for old registration flow
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER")
        val countryCode = intent.getStringExtra("COUNTRY_CODE")

        val displayValue = if (phoneNumber != null && countryCode != null) {
            "$countryCode $phoneNumber"
        } else {
            verificationValue
        }

        setContent {
            OTPVerificationScreen(
                verificationMethod = verificationMethod,
                verificationValue = displayValue,
                verificationPurpose = verificationPurpose,
                onOTPVerified = {
                    when (verificationPurpose) {
                        "forgot_password" -> {
                            // Navigate to Reset Password page
                            val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                                putExtra("verification_method", verificationMethod)
                                putExtra("verification_value", verificationValue)
                            }
                            startActivity(intent)
                            finish()
                        }
                        "registration" -> {
                            // Registration flow - go to main app or next step
                            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                },
                onBackClick = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(
    verificationMethod: String = "phone",
    verificationValue: String = "+880 1234567890",
    verificationPurpose: String = "registration",
    onOTPVerified: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    var otpValue by remember { mutableStateOf("") }
    val otpLength = 6

    val focusRequesters = remember { List(otpLength) { FocusRequester() } }

    val isOtpComplete = otpValue.length == otpLength

    // Dynamic text based on purpose and method
    val title = when (verificationPurpose) {
        "forgot_password" -> "Verify Your Identity"
        else -> "OTP Verification"
    }

    val subtitle = when {
        verificationMethod == "email" && verificationPurpose == "forgot_password" ->
            "Enter the OTP sent to your email\n$verificationValue"
        verificationMethod == "phone" && verificationPurpose == "forgot_password" ->
            "Enter the OTP sent to\n$verificationValue"
        verificationMethod == "email" ->
            "Enter the OTP sent to your email\n$verificationValue"
        else ->
            "Enter the OTP sent to $verificationValue"
    }

    val emoji = when (verificationMethod) {
        "email" -> "✉️"
        else -> "📱"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {

        IconButton(
            onClick = { onBackClick() },
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(otpLength) { index ->
                OTPDigitBox(
                    digit = otpValue.getOrNull(index)?.toString() ?: "",
                    onDigitChange = { newDigit ->
                        val newOtp = otpValue.toMutableList()

                        if (newDigit.isEmpty() && index < otpValue.length) {
                            newOtp.removeAt(index)
                            otpValue = newOtp.joinToString("")

                            if (index > 0) {
                                focusRequesters[index - 1].requestFocus()
                            }
                        } else if (newDigit.isNotEmpty() && newDigit.last().isDigit()) {
                            if (index < newOtp.size) {
                                newOtp[index] = newDigit.last()
                            } else {
                                newOtp.add(newDigit.last())
                            }
                            otpValue = newOtp.joinToString("")

                            if (index < otpLength - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    focusRequester = focusRequesters[index],
                    isActive = otpValue.length == index
                )
            }
        }

        Button(
            onClick = {
                if (isOtpComplete) {
                    onOTPVerified()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isOtpComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOtpComplete) Color(0xFF6C63FF) else Color.Gray,
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (verificationPurpose == "forgot_password") "Verify & Continue" else "Submit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Didn't receive the OTP? ",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "Resend",
                fontSize = 14.sp,
                color = Color(0xFF6C63FF),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "OTP Resent!", Toast.LENGTH_SHORT).show()
                    otpValue = ""
                    focusRequesters[0].requestFocus()
                }
            )
        }
    }
}

@Composable
fun OTPDigitBox(
    digit: String,
    onDigitChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isActive: Boolean
) {
    BasicTextField(
        value = TextFieldValue(digit, TextRange(digit.length)),
        onValueChange = { newValue ->
            if (newValue.text.length <= 1) {
                onDigitChange(newValue.text)
            }
        },
        modifier = Modifier
            .size(50.dp)
            .focusRequester(focusRequester)
            .border(
                width = 2.dp,
                color = if (isActive) Color(0xFF6C63FF) else if (digit.isNotEmpty()) Color(0xFF6C63FF) else Color(0xFFE5E5E5),
                shape = RoundedCornerShape(12.dp)
            )
            .background(Color.White, RoundedCornerShape(12.dp)),
        textStyle = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                innerTextField()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun OTPVerificationScreenPreview() {
    OTPVerificationScreen()
}
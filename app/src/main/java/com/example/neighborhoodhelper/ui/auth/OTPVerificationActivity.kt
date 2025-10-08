package com.example.neighborhoodhelper.ui.auth

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

        // Get phone number from Intent
        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: "XXXXXXXXX"
        val countryCode = intent.getStringExtra("COUNTRY_CODE") ?: "+880"

        setContent {
            OTPVerificationScreen(phoneNumber = "$countryCode $phoneNumber")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(phoneNumber: String = "+880 1234567890") {
    val context = LocalContext.current

    // OTP state - 6 digits
    var otpValue by remember { mutableStateOf("") }
    val otpLength = 6

    // Focus requesters for each OTP field
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }

    // Validation
    val isOtpComplete = otpValue.length == otpLength

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {

        // Back Button
        IconButton(
            onClick = {
                if (context is ComponentActivity) {
                    context.finish()
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

        // OTP Illustration Placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📱",
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
        }

        // Title
        Text(
            text = "OTP Verification",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Subtitle
        Text(
            text = "Enter the OTP sent to $phoneNumber",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )

        // OTP Input Fields
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
                            // Handle backspace
                            newOtp.removeAt(index)
                            otpValue = newOtp.joinToString("")

                            // Move focus to previous field
                            if (index > 0) {
                                focusRequesters[index - 1].requestFocus()
                            }
                        } else if (newDigit.isNotEmpty() && newDigit.last().isDigit()) {
                            // Handle digit input
                            if (index < newOtp.size) {
                                newOtp[index] = newDigit.last()
                            } else {
                                newOtp.add(newDigit.last())
                            }
                            otpValue = newOtp.joinToString("")

                            // Move focus to next field
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

        // Submit Button
        Button(
            onClick = {
                if (isOtpComplete) {
                    Toast.makeText(context, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
                    if (context is ComponentActivity) {
                        context.finish()
                    }
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
                text = "Submit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Resend OTP
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
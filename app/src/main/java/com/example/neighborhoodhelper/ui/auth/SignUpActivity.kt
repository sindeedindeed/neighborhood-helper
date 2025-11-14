package com.example.neighborhoodhelper.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.util.Log

// Assume your main activity is called MainActivity.kt
// You might not directly navigate here after signup, but after email verification and sign in.
// import com.example.neighborhoodhelper.MainActivity

class SignUpActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            SignUpScreen(auth, firestore)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form states
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+880") }

    // Password visibility states
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Loading state
    var isLoading by remember { mutableStateOf(false) }

    // Email validation helper
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Form validation - check if all fields are filled
    val isFormValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            username.isNotBlank() &&
            email.isNotBlank() &&
            isValidEmail(email) &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            phoneNumber.isNotBlank() &&
            nid.isNotBlank() &&
            password == confirmPassword &&
            password.length >= 6

    // Debug logging
    LaunchedEffect(firstName, lastName, username, email, password, confirmPassword, phoneNumber, nid) {
        Log.d("SignUpValidation", """
            First Name: ${firstName.isNotBlank()}
            Last Name: ${lastName.isNotBlank()}
            Username: ${username.isNotBlank()}
            Email: ${email.isNotBlank()} (Valid: ${isValidEmail(email)})
            Password: ${password.isNotBlank()} (length: ${password.length})
            Confirm Password: ${confirmPassword.isNotBlank()}
            Passwords Match: ${password == confirmPassword}
            Phone: ${phoneNumber.isNotBlank()}
            NID: ${nid.isNotBlank()}
            Form Valid: $isFormValid
        """.trimIndent())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // TOP BAR WITH BACK BUTTON AND TITLE (Fixed - Not scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Text(
                text = "Neighborhood Helper",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // MAIN CONTENT (Scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {

            // Title with emoji
            Text(
                text = "Getting Started",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 28.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Subtitle
            Text(
                text = "With Neighborhood Helper",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // First Name and Last Name Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    placeholder = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    placeholder = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Username",
                        tint = Color.Gray
                    )
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color(0xFFE5E5E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("email@example.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (!isValidEmail(email) && email.isNotBlank()) 4.dp else 12.dp),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = Color.Gray
                    )
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (!isValidEmail(email) && email.isNotBlank())
                        Color.Red else Color(0xFF6C63FF),
                    unfocusedBorderColor = if (!isValidEmail(email) && email.isNotBlank())
                        Color.Red else Color(0xFFE5E5E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = !isValidEmail(email) && email.isNotBlank()
            )

            // Show email validation error
            if (!isValidEmail(email) && email.isNotBlank()) {
                Text(
                    text = "Please enter a valid email address",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Password (min 6 characters)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color(0xFFE5E5E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                placeholder = { Text("Confirm Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (password != confirmPassword && confirmPassword.isNotBlank()) 4.dp else 12.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (password != confirmPassword && confirmPassword.isNotBlank())
                        Color.Red else Color(0xFF6C63FF),
                    unfocusedBorderColor = if (password != confirmPassword && confirmPassword.isNotBlank())
                        Color.Red else Color(0xFFE5E5E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = password != confirmPassword && confirmPassword.isNotBlank()
            )

            // Show password mismatch error
            if (password != confirmPassword && confirmPassword.isNotBlank()) {
                Text(
                    text = "Passwords don't match",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            // Phone Number Row (Country Code + Phone Number)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Selector
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { /* Currently read-only, no change implemented */ },
                    modifier = Modifier.width(100.dp),
                    readOnly = true,
                    enabled = !isLoading,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select country code",
                            tint = Color.Gray
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                // Phone Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("Phone Number") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = Color.Gray
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            // NID Field
            OutlinedTextField(
                value = nid,
                onValueChange = { nid = it },
                label = { Text("NID") },
                placeholder = { Text("National ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color(0xFFE5E5E5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Continue Button with Firebase Integration
            Button(
                onClick = {
                    if (isFormValid && !isLoading) {
                        isLoading = true // Set loading state to true
                        val startTime = System.currentTimeMillis()
                        Log.d("SignUpTiming", "Start registration process. Time: ${startTime}ms")

                        Log.d("SignUpFirebase", "Starting Firebase registration...")
                        Log.d("SignUpFirebase", "Creating user with email: $email")

                        // 1. Create user with Firebase Authentication
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { authTask ->
                                val authCompleteTime = System.currentTimeMillis()
                                Log.d("SignUpTiming", "Auth createUserWithEmailAndPassword completed. Time: ${authCompleteTime}ms. Duration: ${authCompleteTime - startTime}ms")

                                if (authTask.isSuccessful) {
                                    val firebaseUser = auth.currentUser
                                    val userId = firebaseUser?.uid ?: ""

                                    Log.d("SignUpFirebase", "User created successfully with ID: $userId")

                                    // 2. Send Email Verification (This runs in parallel and doesn't block Firestore save)
                                    // The Toast messages related to email sending are handled by the EmailVerificationSentActivity now.
                                    firebaseUser?.sendEmailVerification()
                                        ?.addOnCompleteListener { emailVerificationTask ->
                                            val emailSentTime = System.currentTimeMillis()
                                            Log.d("SignUpTiming", "Email verification send attempt completed. Time: ${emailSentTime}ms. Duration from Auth complete: ${emailSentTime - authCompleteTime}ms")

                                            if (emailVerificationTask.isSuccessful) {
                                                Log.d("SignUpFirebase", "Verification email sent.")
                                            } else {
                                                Log.e("SignUpFirebase", "Failed to send verification email: ${emailVerificationTask.exception?.message}")
                                            }
                                        }

                                    // 3. Save User Data to Firestore (Always attempt this if Auth was successful)
                                    saveUserDataToFirestore(
                                        firestore,
                                        userId,
                                        firstName, lastName, username, email,
                                        countryCode, phoneNumber, nid,
                                        onSuccess = {
                                            val firestoreCompleteTime = System.currentTimeMillis()
                                            Log.d("SignUpTiming", "Firestore data save completed. Time: ${firestoreCompleteTime}ms. Duration from Auth complete: ${firestoreCompleteTime - authCompleteTime}ms")
                                            isLoading = false // Reset loading state on success
                                            Log.d("SignUpTiming", "Registration process finished. Total Duration: ${firestoreCompleteTime - startTime}ms")
                                            Log.d("SignUpFirebase", "User data saved successfully! Navigating to EmailVerificationSentActivity.")

                                            // <--- MODIFIED NAVIGATION STARTS HERE --->
                                            // Navigate to the EmailVerificationSentActivity
                                            val intent = Intent(context, EmailVerificationSentActivity::class.java)
                                            // Clear the back stack so user can't go back to SignUpActivity with back button
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            context.startActivity(intent)
                                            // Finish the current SignUpActivity
                                            if (context is ComponentActivity) {
                                                context.finish()
                                            }
                                            // <--- MODIFIED NAVIGATION ENDS HERE --->
                                        },
                                        onFailure = { e ->
                                            val firestoreFailTime = System.currentTimeMillis()
                                            Log.e("SignUpTiming", "Firestore data save FAILED. Time: ${firestoreFailTime}ms. Duration from Auth complete: ${firestoreFailTime - authCompleteTime}ms")
                                            isLoading = false // Reset loading state on Firestore failure
                                            Log.d("SignUpTiming", "Registration process finished with Firestore error. Total Duration: ${firestoreFailTime - startTime}ms")
                                            Log.e("SignUpFirebase", "Error saving user data: ${e.message}")
                                            Toast.makeText(
                                                context,
                                                "Error saving user data: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )

                                } else {
                                    isLoading = false // Reset loading state on Auth failure
                                    Log.d("SignUpTiming", "Auth createUserWithEmailAndPassword FAILED. Total Duration: ${authCompleteTime - startTime}ms")
                                    Log.e("SignUpFirebase", "Registration failed: ${authTask.exception?.message}")
                                    Toast.makeText(
                                        context,
                                        "Registration failed: ${authTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        Log.d("SignUpFirebase", "Form is not valid or already loading")
                        // Provide more immediate feedback if form is invalid
                        if (!isFormValid) {
                            Toast.makeText(context, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid && !isLoading, // Button enabled only if form is valid and not loading
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color(0xFF6C63FF) else Color.Gray,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Already have account text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already Have an Account? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Sign In",
                    fontSize = 14.sp,
                    color = Color(0xFF6C63FF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        if (!isLoading) { // Prevent navigation while loading
                            val intent = Intent(context, SignInActivity::class.java)
                            context.startActivity(intent)
                            if (context is ComponentActivity) {
                                context.finish()
                            }
                        }
                    }
                )
            }
        }
    }
}

// Helper function to save user data to Firestore
// Add this function outside of SignUpScreen, for example, just below it or at the end of the file.
private fun saveUserDataToFirestore(
    firestore: FirebaseFirestore,
    userId: String,
    firstName: String,
    lastName: String,
    username: String,
    email: String,
    countryCode: String,
    phoneNumber: String,
    nid: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val userData = hashMapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "username" to username,
        "email" to email,
        "phoneNumber" to "$countryCode$phoneNumber",
        "countryCode" to countryCode,
        "nid" to nid,
        "createdAt" to Timestamp.now(),
        "emailVerified" to false // New field to track if email is verified
    )

    firestore.collection("users")
        .document(userId)
        .set(userData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}

package com.example.neighborhoodhelper.ui.auth

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
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
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.text.set

class SignUpActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            SignUpScreen(auth, firestore)
        }
    }
}

// Helper function to generate username suggestions (MOVED TO TOP-LEVEL)
private fun generateUsernameSuggestions(
    firstName: String,
    lastName: String,
    currentUsername: String,
    usernameSuggestionsState: MutableState<List<String>> // Accepts MutableState
) {
    val suggestions = mutableListOf<String>()
    val base = if (firstName.isNotBlank() && lastName.isNotBlank()) {
        "${firstName.lowercase()}.${lastName.lowercase()}"
    } else {
        currentUsername.lowercase() // Use the current username as base if names are blank
    }

    // Add suggestions with variations
    suggestions.add("${base}${(100..999).random()}")
    if (firstName.isNotBlank() && lastName.isNotBlank()) { // Only add if both names available
        suggestions.add("${firstName.lowercase()}_${lastName.lowercase()}")
        suggestions.add("${firstName.lowercase()}${lastName.lowercase()}${(10..99).random()}")
    }
    suggestions.add("${currentUsername.lowercase()}_${(1000..9999).random()}")


    usernameSuggestionsState.value = suggestions.take(3) // Update the state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

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
    var termsAccepted by remember { mutableStateOf(false) }

    // Username validation states
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    val usernameSuggestionsState = remember { mutableStateOf<List<String>>(emptyList()) } // Using the state object
    var showSuggestions by remember { mutableStateOf(false) }

    // Password visibility states
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Loading state
    var isLoading by remember { mutableStateOf(false) }

    // Email validation helper
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Username validation helper
    fun isValidUsername(username: String): Boolean {
        return username.length >= 3 &&
                username.matches(Regex("^[a-zA-Z0-9._]+$"))
    }

    // Check username availability
    fun checkUsernameAvailability(usernameToCheck: String) {
        if (usernameToCheck.length < 3) {
            usernameAvailable = null
            showSuggestions = false
            return
        }

        if (!isValidUsername(usernameToCheck)) {
            usernameAvailable = false
            showSuggestions = false
            return
        }

        isCheckingUsername = true
        coroutineScope.launch {
            try {
                val result = firestore.collection("usernames")
                    .document(usernameToCheck.lowercase()) // Ensure consistent casing for lookup
                    .get()
                    .await()

                usernameAvailable = !result.exists()

                if (result.exists()) {
                    // Generate suggestions if username is taken
                    // CALL THE MOVED FUNCTION, PASSING THE STATE OBJECT
                    generateUsernameSuggestions(firstName, lastName, usernameToCheck, usernameSuggestionsState)
                    showSuggestions = true
                } else {
                    showSuggestions = false
                }
            } catch (e: Exception) {
                Log.e("UsernameCheck", "Error checking username: ${e.message}")
                usernameAvailable = null
            } finally {
                isCheckingUsername = false
            }
        }
    }

    // Check username when it changes (with debounce)
    LaunchedEffect(username) {
        kotlinx.coroutines.delay(500) // Debounce for 500ms
        if (username.isNotBlank()) {
            checkUsernameAvailability(username)
        } else {
            usernameAvailable = null
            showSuggestions = false // Clear suggestions if username field is empty
        }
    }

    // Form validation
    val isFormValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            username.isNotBlank() &&
            isValidUsername(username) &&
            usernameAvailable == true && // Ensure username is available
            email.isNotBlank() &&
            isValidEmail(email) &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            phoneNumber.isNotBlank() &&
            nid.isNotBlank() &&
            password == confirmPassword &&
            password.length >= 6 &&
            termsAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // TOP BAR
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

        // MAIN CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {

            Text(
                text = "Getting Started",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 28.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

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
                        unfocusedBorderColor = Color(0xFFE5E5E5)
                    )
                )

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
                        unfocusedBorderColor = Color(0xFFE5E5E5)
                    )
                )
            }

            // Username Field with Validation
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().replace(" ", "")
                    // showSuggestions is now managed by LaunchedEffect and checkUsernameAvailability
                },
                label = { Text("Username") },
                placeholder = { Text("Choose a unique username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                trailingIcon = {
                    when {
                        isCheckingUsername -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6C63FF)
                            )
                        }
                        usernameAvailable == true && username.isNotBlank() -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Available",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        usernameAvailable == false && username.isNotBlank() -> {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Not available",
                                tint = Color.Red
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Username",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when {
                        usernameAvailable == true -> Color(0xFF4CAF50)
                        usernameAvailable == false -> Color.Red
                        else -> Color(0xFF6C63FF)
                    },
                    unfocusedBorderColor = when {
                        usernameAvailable == false -> Color.Red
                        else -> Color(0xFFE5E5E5)
                    }
                ),
                isError = usernameAvailable == false && username.isNotBlank()
            )

            // Username validation message
            if (username.isNotBlank()) {
                when {
                    !isValidUsername(username) -> {
                        Text(
                            text = "Username must be 3+ characters (letters, numbers, . and _ only)",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                    usernameAvailable == true -> {
                        Text(
                            text = "✓ Username is available",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                    usernameAvailable == false -> {
                        Text(
                            text = "✗ Username is already taken",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }
                }
            }

            // Username suggestions
            if (showSuggestions && usernameSuggestionsState.value.isNotEmpty()) { // Use .value to access list
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Suggestions:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        usernameSuggestionsState.value.forEach { suggestion -> // Use .value to access list
                            Text(
                                text = suggestion,
                                fontSize = 14.sp,
                                color = Color(0xFF6C63FF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        username = suggestion
                                        showSuggestions = false
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                        Color.Red else Color(0xFFE5E5E5)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = !isValidEmail(email) && email.isNotBlank()
            )

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
                    unfocusedBorderColor = Color(0xFFE5E5E5)
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
                        Color.Red else Color(0xFFE5E5E5)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = password != confirmPassword && confirmPassword.isNotBlank()
            )

            if (password != confirmPassword && confirmPassword.isNotBlank()) {
                Text(
                    text = "Passwords don't match",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            // Phone Number Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { },
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
                        unfocusedBorderColor = Color(0xFFE5E5E5)
                    )
                )

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
                        unfocusedBorderColor = Color(0xFFE5E5E5)
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
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color(0xFFE5E5E5)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Terms and Conditions Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable(enabled = !isLoading) {
                        termsAccepted = !termsAccepted
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF6C63FF),
                        uncheckedColor = Color.Gray
                    )
                )
                Text(
                    text = "I agree to the ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Terms and Conditions",
                    fontSize = 14.sp,
                    color = Color(0xFF6C63FF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(enabled = !isLoading) {
                        // Open Terms and Conditions screen/dialog
                        Toast.makeText(context, "Opening Terms & Conditions...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Continue Button
            Button(
                onClick = {
                    if (isFormValid && !isLoading) {
                        isLoading = true

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    val firebaseUser = auth.currentUser
                                    val userId = firebaseUser?.uid ?: ""

                                    firebaseUser?.sendEmailVerification()
                                        ?.addOnCompleteListener { verificationTask ->
                                            if (verificationTask.isSuccessful) {
                                                saveUserDataToFirestore(
                                                    firestore,
                                                    userId,
                                                    firstName, lastName, username, email,
                                                    countryCode, phoneNumber, nid,
                                                    onSuccess = {
                                                        isLoading = false
                                                        val intent = Intent(context, EmailVerificationSentActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        context.startActivity(intent)
                                                        if (context is ComponentActivity) {
                                                            context.finish()
                                                        }
                                                    },
                                                    onFailure = { e ->
                                                        isLoading = false
                                                        Toast.makeText(
                                                            context,
                                                            "Error saving user data: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                )
                                            } else {
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Failed to send verification email: ${verificationTask.exception?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Registration failed: ${authTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid && !isLoading,
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
                        if (!isLoading) {
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
    val usernameDoc = firestore.collection("usernames")
        .document(username.lowercase())

    usernameDoc.set(hashMapOf("userId" to userId))
        .addOnSuccessListener {
            val countryCodePattern = Regex("^\\+\\d{1,4}\$")
            val phoneNumberPattern = Regex("^\\d{6,15}\$")
            if (!countryCodePattern.matches(countryCode)) {
                onFailure(Exception("Invalid country code format"))
                return@addOnSuccessListener
            }
            if (!phoneNumberPattern.matches(phoneNumber)) {
                onFailure(Exception("Invalid phone number format"))
                return@addOnSuccessListener
            }

            // Get FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                val fcmToken = if (tokenTask.isSuccessful) tokenTask.result else ""

                val userData = hashMapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "username" to username,
                    "email" to email,
                    "countryCode" to countryCode,
                    "phoneNumber" to phoneNumber,
                    "nid" to nid,
                    "createdAt" to Timestamp.now(),
                    "emailVerified" to false,
                    "fcmToken" to fcmToken
                )

                firestore.collection("users")
                    .document(userId)
                    .set(userData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        usernameDoc.delete()
                        onFailure(e)
                    }
            }
        }
        .addOnFailureListener { e -> onFailure(e) }
}


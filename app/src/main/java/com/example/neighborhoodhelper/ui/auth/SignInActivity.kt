package com.example.neighborhoodhelper.ui.auth

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SignInActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    companion object {
        private const val TAG = "SignInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        setContent {
            SignInScreen(
                onGoogleSignInClick = { signInWithGoogle() },
                onEmailSignIn = { email, password -> signInWithEmail(email, password) }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let { updateUI(it) }
    }

    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(com.example.neighborhoodhelper.R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        kotlinx.coroutines.MainScope().launch {
            try {
                val result = credentialManager.getCredential(request = request, context = this@SignInActivity)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: GetCredentialException) {
                showError("Google sign in failed", e)
            } catch (e: Exception) {
                showError("An error occurred", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                processAuthResult(task, "signInWithCredential")
            }
    }

    private fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                processAuthResult(task, "signInWithEmail")
            }
    }

    private fun processAuthResult(task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>, method: String) {
        if (task.isSuccessful) {
            Log.d(TAG, "$method:success")
            updateUI(auth.currentUser)
        } else {
            Log.w(TAG, "$method:failure", task.exception)
            showToast("Authentication failed: ${task.exception?.message}")
            updateUI(null)
        }
    }

    private fun showError(message: String, exception: Exception) {
        Log.w(TAG, message, exception)
        showToast("$message: ${exception.message}")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(user: FirebaseUser?) {
        user?.let {
            // Store FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(it.uid)
                        .update("fcmToken", token)
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to store FCM token", e)
                        }
                }
            }

            showToast("Welcome ${it.displayName ?: it.email}!")
            Log.d(TAG, "User Info: Display Name: ${it.displayName}, Email: ${it.email}, UID: ${it.uid}")
            startActivity(Intent(this, com.example.neighborhoodhelper.MainActivity::class.java))
            finish()
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
private val textFieldColors
    @Composable
    get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF6C63FF),
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color(0xFF6C63FF)
    )

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A1A1A),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isPassword) 12.dp else 20.dp),
        placeholder = {
            Text(
                placeholder,
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp
            )
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            when {
                isPassword -> {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Toggle password visibility",
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                }
                keyboardType == KeyboardType.Email -> Icon(
                    Icons.Default.Email,
                    "Email",
                    tint = Color(0xFF9E9E9E)
                )
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = textFieldColors,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onGoogleSignInClick: () -> Unit = {},
    onEmailSignIn: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepMeSignedIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            "Sign In",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            "Let's sign in with your account",
            fontSize = 15.sp,
            color = Color(0xFF7D7D7D),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        FieldLabel("Email")
        AuthTextField(username, { username = it }, "Enter your email", KeyboardType.Email)

        FieldLabel("Password")
        AuthTextField(
            password,
            { password = it },
            "Enter your password",
            KeyboardType.Password,
            true,
            passwordVisible
        ) { passwordVisible = !passwordVisible }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    keepMeSignedIn,
                    { keepMeSignedIn = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF6C63FF),
                        uncheckedColor = Color(0xFFBDBDBD)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Keep me sign In",
                    fontSize = 13.sp,
                    color = Color(0xFF1A1A1A)
                )
            }

            Text(
                "Forgot password?",
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.clickable {
                    if (username.isBlank()) {
                        Toast.makeText(context, "Please enter your email first", Toast.LENGTH_SHORT).show()
                    } else {
                        context.startActivity(Intent(context as ComponentActivity, ForgotPasswordActivity::class.java).apply {
                            putExtra("username", username)
                        })
                    }
                }
            )
        }

        Button(
            onClick = { onEmailSignIn(username, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                "Sign In",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "Or, sign in with",
            fontSize = 13.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Black,
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Sign in with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    SignInScreen()
}
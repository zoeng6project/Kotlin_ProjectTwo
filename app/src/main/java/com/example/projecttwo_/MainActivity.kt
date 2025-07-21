package com.example.projecttwo_

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.*                         // Facebook SDK
import com.facebook.login.LoginManager       // For Facebook login button
import com.facebook.login.LoginResult        // Facebook login result callback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*            // Firebase authentication classes

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient  // Google sign-in client
    private lateinit var auth: FirebaseAuth                      // Firebase authentication instance
    private lateinit var statusTextView: TextView               // To show login status or errors
    private lateinit var emailEditText: EditText                // Email input
    private lateinit var passwordEditText: EditText             // Password input

    private lateinit var callbackManager: CallbackManager       // Facebook login callback manager
    private val tracking_id_google = 123                        // Google login request code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        statusTextView = findViewById(R.id.textView)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)

        // Configure Google Sign-In to request ID token and email
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Create Google Sign-In client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        // Set Google sign-in button click listener
        findViewById<Button>(R.id.button_Google).setOnClickListener {
            // Sign out first to prevent auto-login, then launch sign-in intent
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, tracking_id_google)
            }
        }

        // Set Facebook sign-in button click listener
        findViewById<Button>(R.id.button_Facebook).setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this, listOf("email", "public_profile")
            )
        }

        // Handle Facebook login results
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // If successful, handle Facebook token
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    // User canceled login
                    statusTextView.text = "Facebook login canceled."
                }

                override fun onError(error: FacebookException) {
                    // Login failed
                    statusTextView.text = "Facebook login error: ${error.message}"
                }
            })

        // Set Email/Password sign-up button listener
        findViewById<Button>(R.id.button_Email).setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Validate inputs
            if (email.isNotEmpty() && password.length >= 6) {
                // Attempt to create user
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            statusTextView.text = "Email Signed up: ${user?.email}"
                        } else {
                            statusTextView.text = "Email Signup failed: ${task.exception?.message}"
                        }
                    }
            } else {
                statusTextView.text = "Enter valid email and password (6+ chars)"
            }
        }

        // Handle insets (for devices with cutouts/status bar/etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Handle result from Google or Facebook login
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)  // For Facebook

        if (requestCode == tracking_id_google) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                val idToken = account.idToken

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { firebaseTask ->
                        if (firebaseTask.isSuccessful) {
                            val user = auth.currentUser
                            statusTextView.text = "Google Signed in: ${user?.displayName} (${user?.email})"
                        } else {
                            statusTextView.text =
                                "Firebase Google Sign-In Failed: ${firebaseTask.exception?.message}"
                        }
                    }
            } catch (e: Exception) {
                statusTextView.text = "Google Sign-In Failed: ${e.message}"
            }
        }
    }

    // Handle Facebook login token and authenticate with Firebase
    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    statusTextView.text = "Facebook Signed in: ${user?.displayName} (${user?.email})"
                } else {
                    statusTextView.text =
                        "Firebase Facebook Auth failed: ${task.exception?.message}"
                }
            }
    }
}

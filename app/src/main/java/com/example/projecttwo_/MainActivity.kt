package com.example.projecttwo_

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.*   // Facebook SDK
import com.facebook.login.LoginManager  // Facebook Login
import com.facebook.login.LoginResult   // Facebook Login result

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider  // Facebook -> Firebase credential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var statusTextView: TextView

    private lateinit var callbackManager: CallbackManager // Facebook callback manager
    private val tracking_id_google = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Firebase instance
        auth = FirebaseAuth.getInstance()
        statusTextView = findViewById(R.id.textView)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        // Google Sign-In button click
        findViewById<Button>(R.id.button_Google).setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, tracking_id_google)
            }
        }

        // Facebook Login button click
        findViewById<Button>(R.id.button_Facebook).setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }

        // Facebook Login callback
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    statusTextView.text = "Facebook login canceled."
                }

                override fun onError(error: FacebookException) {
                    statusTextView.text = "Facebook login error: ${error.message}"
                }
            })

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Handle results from both Google & Facebook
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Forward result to Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Handle Google Sign-In result
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

    // Convert Facebook token -> Firebase credential
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

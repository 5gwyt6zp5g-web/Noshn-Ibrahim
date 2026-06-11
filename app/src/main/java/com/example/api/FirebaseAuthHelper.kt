package com.example.api

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"

    // Default Web Client ID for Google Sign-in fallback
    private const val DEFAULT_WEB_CLIENT_ID = "123456789012-mockclientid12345.apps.googleusercontent.com"

    @Volatile
    private var isInitialized = false

    /**
     * Initializes Firebase App programmatically if it's not already initialized.
     * This avoids gradle-services.json crash if a project is compiled in dynamic environments.
     */
    fun initializeFirebase(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyFakeKeyPlaceholderForApplet_123")
                        .setApplicationId("1:123456789012:android:abcdef123456")
                        .setProjectId("eduhub-f8a7d")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.d(TAG, "Firebase initialized programmatically with dynamic options")
                } else {
                    Log.d(TAG, "Firebase was already initialized")
                }
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase: ${e.message}")
            }
        }
    }

    /**
     * Safely retrieves the FirebaseAuth instance, ensuring Firebase is initialized first.
     */
    fun getAuth(context: Context): FirebaseAuth? {
        initializeFirebase(context.getApplicationContext())
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth.getInstance() failed: ${e.message}")
            null
        }
    }

    /**
     * Configures and retrieves the GoogleSignInClient.
     */
    fun getGoogleSignInClient(context: Context, webClientId: String? = null): GoogleSignInClient {
        val clientId = if (!webClientId.isNullOrBlank()) webClientId else DEFAULT_WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
}

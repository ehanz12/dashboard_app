package com.reihan.dashboard.ui.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

object GoogleSignInHelper {
    private const val GOOGLE_CLIENT_ID = "350850678512-5c8579vu92dsenqaet17u7iec0k4gr2v.apps.googleusercontent.com"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent {
        val googleSignInClient = getGoogleSignInClient(context)
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(result: ActivityResult): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            null
        }
    }

    fun getIdToken(account: GoogleSignInAccount?): String? {
        return account?.idToken
    }

    fun getUserInfo(account: GoogleSignInAccount?): Pair<String?, String?>? {
        return if (account != null) {
            Pair(account.displayName, account.email)
        } else {
            null
        }
    }

    fun signOut(context: Context) {
        val googleSignInClient = getGoogleSignInClient(context)
        googleSignInClient.signOut()
    }
}

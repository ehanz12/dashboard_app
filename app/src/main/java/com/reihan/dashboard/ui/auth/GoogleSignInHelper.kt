package com.reihan.dashboard.ui.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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

    fun handleSignInResult(result: ActivityResult): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d("GOOGLE_SIGNIN", "Google Sign-In Success: email=${account.email}")
                Result.success(account)
            } else {
                Result.failure(Exception("Gagal mendapatkan akun Google."))
            }
        } catch (e: ApiException) {
            val errorMsg = when (e.statusCode) {
                10 -> "Google Sign-In Error (10): SHA-1 fingerprint belum terdaftar di Firebase Console atau Web Client ID tidak cocok."
                7 -> "Koneksi jaringan gagal saat Google Sign-In."
                12500 -> "Google Play Services Error (12500). Perbarui Google Play Services di perangkat."
                else -> "Google Sign-In gagal dengan kode status ${e.statusCode}."
            }
            Log.e("GOOGLE_SIGNIN", errorMsg, e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e("GOOGLE_SIGNIN", "Google Sign-In Exception: ${e.localizedMessage}", e)
            Result.failure(e)
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

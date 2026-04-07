package com.ivoryapp.nurseflow.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ivoryapp.nurseflow.MainActivity
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private val TAG = "LoginActivity"
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Auto-login check
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setupGoogle()
        setupClick()
    }

    private fun setupGoogle() {
        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "Web Client ID not found", e)
            ""
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClick() {
        binding.btnGoogleLogin.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnGoogleLogin.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnGoogleLogin.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuth(account.idToken!!)
            } catch (e: ApiException) {
                showLoading(false)
                Log.e(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Login Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuth(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(it)
                    }
                } else {
                    showLoading(false)
                    Log.e(TAG, "Firebase auth failed", task.exception)
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun generateUserCode(): String {
        val number = (1000..9999).random()
        val letters = ('A'..'Z').shuffled().take(2).joinToString("")
        return "NF-$number-$letters"
    }

    private fun saveUserToFirestore(user: FirebaseUser) {
        val uid = user.uid
        
        // Cek dulu apakah user sudah punya data (biar userCode tidak berubah-ubah)
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val existingCode = doc.getString("userCode")
            
            val userMap = hashMapOf(
                "uid" to uid,
                "name" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "userCode" to (existingCode ?: generateUserCode()),
                "lastLogin" to FieldValue.serverTimestamp()
            )

            firestore.collection("users")
                .document(uid)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    navigateToMain()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED SAVE USER", e)
                    navigateToMain()
                }
        }.addOnFailureListener {
            // Jika gagal fetch, tetap coba simpan dengan kode baru
            val userMap = hashMapOf(
                "uid" to uid,
                "name" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "userCode" to generateUserCode(),
                "lastLogin" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid).set(userMap, SetOptions.merge())
                .addOnSuccessListener { navigateToMain() }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

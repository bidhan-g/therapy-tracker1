package com.bidh.therapytracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.databinding.ActivityLockBinding

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!SecurePrefs.isLockEnabled(this)) {
            goToMain()
            return
        }

        binding.btnUnlock.setOnClickListener { authenticate() }
        authenticate()
    }

    private fun authenticate() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // No usable lock configured on the device; don't lock the user out of their own app.
            goToMain()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                goToMain()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.locked_message))
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

package com.bidh.therapytracker.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.data.Session
import com.bidh.therapytracker.data.SessionRepository
import com.bidh.therapytracker.databinding.ActivitySettingsBinding
import com.bidh.therapytracker.reminders.ReminderScheduler
import com.bidh.therapytracker.sync.DriveSyncScheduler
import com.bidh.therapytracker.util.DateTimeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: SessionRepository

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            task.getResult(ApiException::class.java)
            onGoogleDriveConnected()
        } catch (e: ApiException) {
            Toast.makeText(this, R.string.drive_sign_in_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        repository = SessionRepository(this)

        binding.switchAppLock.isChecked = SecurePrefs.isLockEnabled(this)
        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            SecurePrefs.setLockEnabled(this, isChecked)
        }

        updateMorningTimeText(SecurePrefs.getMorningHour(this), SecurePrefs.getMorningMinute(this))

        binding.btnMorningTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, m ->
                    SecurePrefs.setMorningTime(this, h, m)
                    updateMorningTimeText(h, m)
                    rescheduleAllUpcoming()
                },
                SecurePrefs.getMorningHour(this),
                SecurePrefs.getMorningMinute(this),
                false
            ).show()
        }

        binding.btnConnectGoogleDrive.setOnClickListener {
            if (GoogleSignIn.getLastSignedInAccount(this) != null) {
                disconnectGoogleDrive()
            } else {
                signInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        updateGoogleDriveStatusUi()
    }

    override fun onResume() {
        super.onResume()
        updateGoogleDriveStatusUi()
    }

    private fun onGoogleDriveConnected() {
        updateGoogleDriveStatusUi()
        DriveSyncScheduler.ensurePeriodicSyncScheduled(this)
        DriveSyncScheduler.triggerImmediateFullSync(this)
    }

    private fun disconnectGoogleDrive() {
        googleSignInClient.signOut().addOnCompleteListener {
            DriveSyncScheduler.cancelPeriodicSync(this)
            updateGoogleDriveStatusUi()
        }
    }

    private fun updateGoogleDriveStatusUi() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            binding.tvGoogleDriveStatus.text = getString(R.string.drive_connected, account.email ?: account.displayName ?: "")
            binding.btnConnectGoogleDrive.setText(R.string.disconnect_google_drive)
        } else {
            binding.tvGoogleDriveStatus.setText(R.string.drive_not_connected)
            binding.btnConnectGoogleDrive.setText(R.string.connect_google_drive)
        }
    }

    private fun updateMorningTimeText(hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        binding.btnMorningTime.text = DateTimeUtils.formatTime(cal.timeInMillis)
    }

    private fun rescheduleAllUpcoming() {
        lifecycleScope.launch {
            val upcoming = repository.getAllFutureScheduledWithCategory()
            upcoming.forEach { item ->
                val session = Session(
                    id = item.id,
                    dateTimeMillis = item.dateTimeMillis,
                    status = item.status,
                    createdAtMillis = item.createdAtMillis,
                    categoryId = item.categoryId
                )
                ReminderScheduler.schedule(this@SettingsActivity, session, item.categoryName)
            }
        }
    }
}

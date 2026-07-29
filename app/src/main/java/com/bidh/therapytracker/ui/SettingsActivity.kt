package com.bidh.therapytracker.ui

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.data.SessionRepository
import com.bidh.therapytracker.databinding.ActivitySettingsBinding
import com.bidh.therapytracker.reminders.ReminderScheduler
import com.bidh.therapytracker.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        repository = SessionRepository(this)

        var target = SecurePrefs.getTargetSessions(this).coerceAtLeast(1)
        binding.tvTargetValue.text = target.toString()

        binding.btnDecreaseTarget.setOnClickListener {
            if (target > 1) {
                target -= 1
                binding.tvTargetValue.text = target.toString()
                SecurePrefs.setTargetSessions(this, target)
            }
        }
        binding.btnIncreaseTarget.setOnClickListener {
            target += 1
            binding.tvTargetValue.text = target.toString()
            SecurePrefs.setTargetSessions(this, target)
        }

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

        binding.btnStartNewPlan.setOnClickListener { confirmStartNewPlan() }
    }

    private fun confirmStartNewPlan() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_new_plan_title)
            .setMessage(R.string.confirm_new_plan_message)
            .setPositiveButton(R.string.start) { _, _ -> startNewPlan() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startNewPlan() {
        lifecycleScope.launch {
            val allSessions = repository.getAll()
            allSessions.forEach { ReminderScheduler.cancel(this@SettingsActivity, it.id) }
            repository.deleteAll()
            SecurePrefs.clearTargetSessions(this@SettingsActivity)
            finish()
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
            val upcoming = repository.getFutureScheduled()
            upcoming.forEach { ReminderScheduler.schedule(this@SettingsActivity, it) }
        }
    }
}

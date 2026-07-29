package com.bidh.therapytracker.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.data.Session
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

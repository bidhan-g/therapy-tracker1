package com.bidh.therapytracker.ui

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.data.Session
import com.bidh.therapytracker.data.SessionRepository
import com.bidh.therapytracker.data.SessionStatus
import com.bidh.therapytracker.databinding.ActivityMainBinding
import com.bidh.therapytracker.reminders.ReminderScheduler
import com.bidh.therapytracker.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: SessionRepository
    private lateinit var adapter: SessionAdapter
    private var targetSetupDialog: AlertDialog? = null

    private enum class DialogMode { SCHEDULE, LOG_PAST, EDIT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repository = SessionRepository(this)
        adapter = SessionAdapter(
            onEdit = { showSessionDialog(DialogMode.EDIT, it) },
            onDelete = { confirmDelete(it) },
            onMarkCompleted = { updateStatus(it, SessionStatus.COMPLETED) },
            onMarkMissed = { updateStatus(it, SessionStatus.MISSED) },
            onMarkScheduled = { updateStatus(it, SessionStatus.SCHEDULED) }
        )
        binding.recyclerSessions.layoutManager = LinearLayoutManager(this)
        binding.recyclerSessions.adapter = adapter

        binding.btnScheduleAppointment.setOnClickListener { showSessionDialog(DialogMode.SCHEDULE, null) }
        binding.btnLogPastSession.setOnClickListener { showSessionDialog(DialogMode.LOG_PAST, null) }

        observeData()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (!SecurePrefs.isTargetSet(this) && targetSetupDialog?.isShowing != true) {
            showTargetSetupDialog()
        }
    }

    private fun showTargetSetupDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_set_target, null)
        val etTarget = view.findViewById<android.widget.EditText>(R.id.etTargetInput)
        val tvError = view.findViewById<android.widget.TextView>(R.id.tvTargetError)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(R.string.get_started, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = etTarget.text.toString().trim().toIntOrNull()
                if (value == null || value <= 0) {
                    tvError.visibility = View.VISIBLE
                } else {
                    SecurePrefs.setTargetSessions(this, value)
                    binding.tvProgressCount.text = "0 / $value"
                    dialog.dismiss()
                }
            }
        }

        targetSetupDialog = dialog
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun observeData() {
        val target = SecurePrefs.getTargetSessions(this)
        binding.tvProgressCount.text = if (target > 0) "0 / $target" else "0"

        repository.observeCompletedCount().observe(this) { count ->
            val currentTarget = SecurePrefs.getTargetSessions(this)
            binding.tvProgressCount.text = if (currentTarget > 0) "$count / $currentTarget" else "$count"
            val pct = if (currentTarget > 0) {
                ((count.toFloat() / currentTarget) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            binding.progressBar.progress = pct
        }

        repository.observeNextUpcoming().observe(this) { session ->
            if (session == null) {
                binding.tvNextAppointment.text = getString(R.string.no_upcoming_appointment)
                binding.layoutNextAppointmentActions.visibility = View.GONE
            } else {
                binding.tvNextAppointment.text = DateTimeUtils.formatDateTime(session.dateTimeMillis)
                binding.layoutNextAppointmentActions.visibility = View.VISIBLE
                binding.btnEditNext.setOnClickListener { showSessionDialog(DialogMode.EDIT, session) }
                binding.btnCancelNext.setOnClickListener { confirmDelete(session) }
            }
        }

        repository.observeAll().observe(this) { sessions ->
            adapter.submitList(sessions)
            binding.tvEmptyHistory.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showSessionDialog(mode: DialogMode, existing: Session?) {
        val view = layoutInflater.inflate(R.layout.dialog_add_session, null)
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val btnDate = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickDate)
        val btnTime = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickTime)

        val calendar = Calendar.getInstance()
        if (existing != null) {
            calendar.timeInMillis = existing.dateTimeMillis
        }

        tvTitle.text = when (mode) {
            DialogMode.SCHEDULE -> getString(R.string.schedule_appointment)
            DialogMode.LOG_PAST -> getString(R.string.log_past_session)
            DialogMode.EDIT -> getString(R.string.edit)
        }

        fun refreshButtons() {
            btnDate.text = DateTimeUtils.formatDate(calendar.timeInMillis)
            btnTime.text = DateTimeUtils.formatTime(calendar.timeInMillis)
        }
        refreshButtons()

        btnDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    refreshButtons()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    refreshButtons()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ -> saveSession(mode, existing, calendar.timeInMillis) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveSession(mode: DialogMode, existing: Session?, dateTimeMillis: Long) {
        lifecycleScope.launch {
            when (mode) {
                DialogMode.SCHEDULE -> {
                    val session = Session(dateTimeMillis = dateTimeMillis, status = SessionStatus.SCHEDULED)
                    val id = repository.insert(session)
                    ReminderScheduler.schedule(this@MainActivity, session.copy(id = id))
                }
                DialogMode.LOG_PAST -> {
                    val session = Session(dateTimeMillis = dateTimeMillis, status = SessionStatus.COMPLETED)
                    repository.insert(session)
                }
                DialogMode.EDIT -> {
                    existing?.let {
                        val updated = it.copy(dateTimeMillis = dateTimeMillis)
                        repository.update(updated)
                        if (updated.status == SessionStatus.SCHEDULED) {
                            ReminderScheduler.schedule(this@MainActivity, updated)
                        } else {
                            ReminderScheduler.cancel(this@MainActivity, updated.id)
                        }
                    }
                }
            }
        }
    }

    private fun updateStatus(session: Session, status: SessionStatus) {
        lifecycleScope.launch {
            val updated = session.copy(status = status)
            repository.update(updated)
            if (status == SessionStatus.SCHEDULED) {
                ReminderScheduler.schedule(this@MainActivity, updated)
            } else {
                ReminderScheduler.cancel(this@MainActivity, updated.id)
            }
        }
    }

    private fun confirmDelete(session: Session) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    repository.delete(session)
                    ReminderScheduler.cancel(this@MainActivity, session.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}

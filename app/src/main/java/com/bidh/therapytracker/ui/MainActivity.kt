package com.bidh.therapytracker.ui

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.Category
import com.bidh.therapytracker.data.CategoryRepository
import com.bidh.therapytracker.data.Session
import com.bidh.therapytracker.data.SessionRepository
import com.bidh.therapytracker.data.SessionStatus
import com.bidh.therapytracker.databinding.ActivityMainBinding
import com.bidh.therapytracker.reminders.ReminderScheduler
import com.bidh.therapytracker.sync.DriveSyncScheduler
import com.bidh.therapytracker.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: SessionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var adapter: SessionAdapter
    private var categoryId: Long = -1
    private var currentCategory: Category? = null
    private var lastCompletedCount = 0

    private enum class DialogMode { SCHEDULE, LOG_PAST, EDIT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1)
        if (categoryId <= 0) {
            finish()
            return
        }

        repository = SessionRepository(this)
        categoryRepository = CategoryRepository(this)
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

        loadCategory()
        observeData()
        requestNotificationPermissionIfNeeded()
    }

    private fun loadCategory() {
        lifecycleScope.launch {
            currentCategory = categoryRepository.getById(categoryId)
            binding.toolbar.title = currentCategory?.name ?: getString(R.string.app_name)
            refreshProgressDisplay()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_edit_category -> {
                showEditCategoryDialog()
                true
            }
            R.id.action_delete_category -> {
                confirmDeleteCategory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEditCategoryDialog() {
        val category = currentCategory ?: return
        val view = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etName = view.findViewById<EditText>(R.id.etCategoryName)
        val tvNameError = view.findViewById<TextView>(R.id.tvCategoryNameError)
        val etTarget = view.findViewById<EditText>(R.id.etCategoryTarget)
        etName.setText(category.name)
        category.targetCount?.let { etTarget.setText(it.toString()) }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.edit_category)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    tvNameError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                val targetText = etTarget.text.toString().trim()
                val target = if (targetText.isEmpty()) null else targetText.toIntOrNull()?.takeIf { it > 0 }
                lifecycleScope.launch {
                    val updated = category.copy(name = name, targetCount = target)
                    categoryRepository.update(updated)
                    currentCategory = updated
                    binding.toolbar.title = updated.name
                    refreshProgressDisplay()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteCategory() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_category_title)
            .setMessage(R.string.confirm_delete_category_message)
            .setPositiveButton(R.string.delete) { _, _ -> deleteCategory() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCategory() {
        val category = currentCategory ?: return
        lifecycleScope.launch {
            val sessions = repository.getAllForCategory(categoryId)
            sessions.forEach { ReminderScheduler.cancel(this@MainActivity, it.id) }
            repository.deleteAllForCategory(categoryId)
            categoryRepository.delete(category)
            DriveSyncScheduler.triggerSyncSoon(this@MainActivity)
            finish()
        }
    }

    private fun refreshProgressDisplay() {
        val target = currentCategory?.targetCount
        binding.tvProgressCount.text = if (target != null && target > 0) "$lastCompletedCount / $target" else "$lastCompletedCount"
        binding.progressBar.progress = if (target != null && target > 0) {
            ((lastCompletedCount.toFloat() / target) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    private fun observeData() {
        repository.observeCompletedCountForCategory(categoryId).observe(this) { count ->
            lastCompletedCount = count
            refreshProgressDisplay()
        }

        repository.observeNextUpcomingForCategory(categoryId).observe(this) { session ->
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

        repository.observeAllForCategory(categoryId).observe(this) { sessions ->
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
        val categoryName = currentCategory?.name ?: getString(R.string.app_name)
        lifecycleScope.launch {
            when (mode) {
                DialogMode.SCHEDULE -> {
                    val session = Session(dateTimeMillis = dateTimeMillis, status = SessionStatus.SCHEDULED, categoryId = categoryId)
                    val id = repository.insert(session)
                    ReminderScheduler.schedule(this@MainActivity, session.copy(id = id), categoryName)
                }
                DialogMode.LOG_PAST -> {
                    val session = Session(dateTimeMillis = dateTimeMillis, status = SessionStatus.COMPLETED, categoryId = categoryId)
                    repository.insert(session)
                }
                DialogMode.EDIT -> {
                    existing?.let {
                        val updated = it.copy(dateTimeMillis = dateTimeMillis)
                        repository.update(updated)
                        if (updated.status == SessionStatus.SCHEDULED) {
                            ReminderScheduler.schedule(this@MainActivity, updated, categoryName)
                        } else {
                            ReminderScheduler.cancel(this@MainActivity, updated.id)
                        }
                    }
                }
            }
            DriveSyncScheduler.triggerSyncSoon(this@MainActivity)
        }
    }

    private fun updateStatus(session: Session, status: SessionStatus) {
        val categoryName = currentCategory?.name ?: getString(R.string.app_name)
        lifecycleScope.launch {
            val updated = session.copy(status = status)
            repository.update(updated)
            if (status == SessionStatus.SCHEDULED) {
                ReminderScheduler.schedule(this@MainActivity, updated, categoryName)
            } else {
                ReminderScheduler.cancel(this@MainActivity, updated.id)
            }
            DriveSyncScheduler.triggerSyncSoon(this@MainActivity)
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
                    DriveSyncScheduler.triggerSyncSoon(this@MainActivity)
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

    companion object {
        const val EXTRA_CATEGORY_ID = "extra_category_id"
    }
}

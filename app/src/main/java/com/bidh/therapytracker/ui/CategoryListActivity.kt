package com.bidh.therapytracker.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.Category
import com.bidh.therapytracker.data.CategoryRepository
import com.bidh.therapytracker.data.CategorySummary
import com.bidh.therapytracker.data.SessionRepository
import com.bidh.therapytracker.databinding.ActivityCategoryListBinding
import com.bidh.therapytracker.reminders.ReminderScheduler
import com.bidh.therapytracker.sync.DriveSyncScheduler
import kotlinx.coroutines.launch

class CategoryListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryListBinding
    private lateinit var repository: CategoryRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityCategoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repository = CategoryRepository(this)
        sessionRepository = SessionRepository(this)
        adapter = CategoryAdapter(
            onClick = { openCategory(it) },
            onEdit = { showEditCategoryDialog(it) },
            onDelete = { confirmDeleteCategory(it) }
        )
        binding.recyclerCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerCategories.adapter = adapter

        binding.fabAddCategory.setOnClickListener { showAddCategoryDialog() }

        repository.observeSummaries().observe(this) { summaries ->
            adapter.submitList(summaries)
            binding.tvEmptyCategories.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_category_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun openCategory(summary: CategorySummary) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_CATEGORY_ID, summary.id)
        startActivity(intent)
    }

    private fun showAddCategoryDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etName = view.findViewById<EditText>(R.id.etCategoryName)
        val tvNameError = view.findViewById<TextView>(R.id.tvCategoryNameError)
        val etTarget = view.findViewById<EditText>(R.id.etCategoryTarget)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_category)
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
                    repository.insert(Category(name = name, targetCount = target))
                    DriveSyncScheduler.triggerSyncSoon(this@CategoryListActivity)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showEditCategoryDialog(summary: CategorySummary) {
        val view = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etName = view.findViewById<EditText>(R.id.etCategoryName)
        val tvNameError = view.findViewById<TextView>(R.id.tvCategoryNameError)
        val etTarget = view.findViewById<EditText>(R.id.etCategoryTarget)
        etName.setText(summary.name)
        summary.targetCount?.let { etTarget.setText(it.toString()) }

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
                    repository.update(Category(id = summary.id, name = name, targetCount = target))
                    DriveSyncScheduler.triggerSyncSoon(this@CategoryListActivity)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteCategory(summary: CategorySummary) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_category_title)
            .setMessage(R.string.confirm_delete_category_message)
            .setPositiveButton(R.string.delete) { _, _ -> deleteCategory(summary) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCategory(summary: CategorySummary) {
        lifecycleScope.launch {
            val sessions = sessionRepository.getAllForCategory(summary.id)
            sessions.forEach { ReminderScheduler.cancel(this@CategoryListActivity, it.id) }
            sessionRepository.deleteAllForCategory(summary.id)
            repository.delete(Category(id = summary.id, name = summary.name, targetCount = summary.targetCount))
            DriveSyncScheduler.triggerSyncSoon(this@CategoryListActivity)
        }
    }
}

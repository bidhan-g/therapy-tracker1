package com.bidh.therapytracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.CategorySummary
import com.bidh.therapytracker.databinding.ItemCategoryBinding
import com.bidh.therapytracker.util.DateTimeUtils

class CategoryAdapter(
    private val onClick: (CategorySummary) -> Unit,
    private val onEdit: (CategorySummary) -> Unit,
    private val onDelete: (CategorySummary) -> Unit
) : ListAdapter<CategorySummary, CategoryAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: CategorySummary) {
            val context = binding.root.context
            binding.tvCategoryName.text = summary.name

            val target = summary.targetCount
            if (target != null && target > 0) {
                binding.tvCategoryProgress.text = "${summary.completedCount} / $target"
                binding.progressBarCategory.visibility = View.VISIBLE
                binding.progressBarCategory.progress =
                    ((summary.completedCount.toFloat() / target) * 100).toInt().coerceIn(0, 100)
            } else {
                binding.tvCategoryProgress.text =
                    context.getString(R.string.category_progress_no_target, summary.completedCount)
                binding.progressBarCategory.visibility = View.GONE
            }

            binding.tvCategoryNext.text = if (summary.nextUpcomingMillis != null) {
                context.getString(
                    R.string.next_appointment_format,
                    DateTimeUtils.formatDateTime(summary.nextUpcomingMillis)
                )
            } else {
                context.getString(R.string.no_upcoming_appointment)
            }

            binding.root.setOnClickListener { onClick(summary) }
            binding.btnCategoryMore.setOnClickListener { view -> showPopupMenu(view, summary) }
        }

        private fun showPopupMenu(anchor: View, summary: CategorySummary) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, anchor.context.getString(R.string.edit_category))
            popup.menu.add(0, 2, 1, anchor.context.getString(R.string.delete_category))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onEdit(summary)
                    2 -> onDelete(summary)
                }
                true
            }
            popup.show()
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CategorySummary>() {
            override fun areItemsTheSame(oldItem: CategorySummary, newItem: CategorySummary) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: CategorySummary, newItem: CategorySummary) = oldItem == newItem
        }
    }
}

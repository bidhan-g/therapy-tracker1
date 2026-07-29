package com.bidh.therapytracker.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bidh.therapytracker.R
import com.bidh.therapytracker.data.Session
import com.bidh.therapytracker.data.SessionStatus
import com.bidh.therapytracker.databinding.ItemSessionBinding
import com.bidh.therapytracker.util.DateTimeUtils

class SessionAdapter(
    private val onEdit: (Session) -> Unit,
    private val onDelete: (Session) -> Unit,
    private val onMarkCompleted: (Session) -> Unit,
    private val onMarkMissed: (Session) -> Unit,
    private val onMarkScheduled: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.SessionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: Session) {
            val context = binding.root.context
            binding.tvSessionDate.text = DateTimeUtils.formatDate(session.dateTimeMillis)

            val statusText = when (session.status) {
                SessionStatus.COMPLETED -> context.getString(R.string.status_completed)
                SessionStatus.SCHEDULED -> context.getString(R.string.status_scheduled)
                SessionStatus.MISSED -> context.getString(R.string.status_missed)
            }
            binding.tvSessionStatus.text = "$statusText · ${DateTimeUtils.formatTime(session.dateTimeMillis)}"

            val colorRes = when (session.status) {
                SessionStatus.COMPLETED -> R.color.status_completed
                SessionStatus.SCHEDULED -> R.color.status_upcoming
                SessionStatus.MISSED -> R.color.status_missed
            }
            binding.statusIndicator.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

            binding.btnMoreOptions.setOnClickListener { view -> showPopupMenu(view, session) }
            binding.root.setOnClickListener { showPopupMenu(binding.btnMoreOptions, session) }
        }

        private fun showPopupMenu(anchor: View, session: Session) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, anchor.context.getString(R.string.edit))
            if (session.status != SessionStatus.COMPLETED) {
                popup.menu.add(0, 2, 1, anchor.context.getString(R.string.mark_completed))
            }
            if (session.status != SessionStatus.MISSED) {
                popup.menu.add(0, 3, 2, anchor.context.getString(R.string.mark_missed))
            }
            if (session.status != SessionStatus.SCHEDULED) {
                popup.menu.add(0, 4, 3, anchor.context.getString(R.string.mark_scheduled))
            }
            popup.menu.add(0, 5, 4, anchor.context.getString(R.string.delete))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onEdit(session)
                    2 -> onMarkCompleted(session)
                    3 -> onMarkMissed(session)
                    4 -> onMarkScheduled(session)
                    5 -> onDelete(session)
                }
                true
            }
            popup.show()
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(oldItem: Session, newItem: Session) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Session, newItem: Session) = oldItem == newItem
        }
    }
}

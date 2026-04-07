package com.ivoryapp.nurseflow.ui.notification

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(
    private val onAccept: (Notification) -> Unit,
    private val onReject: (Notification) -> Unit,
    private val onReadStatusChange: (Notification, Boolean) -> Unit,
    private val onClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            val context = binding.root.context
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            binding.tvTime.text = notification.timestamp?.toDate()?.let { sdf.format(it) } ?: ""

            // Unread Indicator
            binding.viewUnreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Click listener for navigation and marking as read
            binding.root.setOnClickListener { 
                if (!notification.isRead) {
                    onReadStatusChange(notification, true)
                }
                onClick(notification)
            }

            // Style based on read status
            if (notification.isRead) {
                binding.tvTitle.alpha = 0.6f
                binding.tvMessage.alpha = 0.6f
                binding.cardNotification.alpha = 0.8f
            } else {
                binding.tvTitle.alpha = 1.0f
                binding.tvMessage.alpha = 1.0f
                binding.cardNotification.alpha = 1.0f
            }

            // Set notification type text on the chip
            binding.chipType.text = notification.type.name

            when (notification.type) {
                NotificationType.SYSTEM -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_notifications)
                    binding.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_purple))
                    binding.layoutActions.visibility = View.GONE
                }
                NotificationType.APP -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_notifications)
                    binding.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_blue))
                    binding.layoutActions.visibility = View.GONE
                }
                NotificationType.FRIEND_REQUEST -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_notifications)
                    binding.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_cyan))
                    if (notification.status == "pending") {
                        binding.layoutActions.visibility = View.VISIBLE
                        binding.btnAccept.setOnClickListener { onAccept(notification) }
                        binding.btnReject.setOnClickListener { onReject(notification) }
                    } else {
                        binding.layoutActions.visibility = View.GONE
                    }
                }
                NotificationType.CLINICAL_ALERT -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_notifications)
                    val color = if (notification.title.contains("CRITICAL", ignoreCase = true)) {
                        ContextCompat.getColor(context, R.color.status_urgent)
                    } else {
                        ContextCompat.getColor(context, R.color.status_pending)
                    }
                    binding.ivIcon.imageTintList = ColorStateList.valueOf(color)
                    binding.layoutActions.visibility = View.GONE
                    
                    // Highlight clinical alert title if critical
                    if (notification.title.contains("CRITICAL", ignoreCase = true)) {
                        binding.tvTitle.setTextColor(color)
                    } else {
                        binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean = oldItem == newItem
    }
}

package com.ivoryapp.nurseflow.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Notification) {
            binding.tvTitle.text = item.title
            binding.tvMessage.text = item.message
            binding.viewUnreadIndicator.visibility = if (item.isRead) View.GONE else View.VISIBLE
            
            // Format Timestamp
            item.timestamp?.let {
                val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
                binding.tvTime.text = sdf.format(it.toDate())
            }

            // UI based on Type
            // Cek case-insensitive atau sesuaikan dengan enum string di Firestore
            val isRequest = item.type == NotificationType.FRIEND_REQUEST || 
                            item.type == NotificationType.HANDOVER_REQUEST
            
            if (isRequest) {
                binding.layoutActions.visibility = if (item.status == "pending") View.VISIBLE else View.GONE
                binding.btnAccept.setOnClickListener { onAccept(item) }
                binding.btnReject.setOnClickListener { onReject(item) }
                
                binding.ivIcon.setImageResource(
                    if (item.type == NotificationType.HANDOVER_REQUEST) R.drawable.ic_person else R.drawable.ic_group
                )
            } else {
                binding.layoutActions.visibility = View.GONE
                binding.ivIcon.setImageResource(R.drawable.ic_notifications)
            }

            binding.root.setOnClickListener {
                onReadStatusChange(item, true)
                onClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean = oldItem == newItem
    }
}

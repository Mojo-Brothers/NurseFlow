package com.ivoryapp.nurseflow.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.Task
import com.ivoryapp.nurseflow.databinding.ItemTaskBinding

class TaskAdapter(private val onTaskChecked: (Task, Boolean) -> Unit) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskDesc.text = task.description
            
            // Remove listener before setting state to avoid recursion
            binding.cbTask.setOnCheckedChangeListener(null)
            binding.cbTask.isChecked = task.isCompleted
            
            // Status Indicator Logic (Mocking urgency for UI demo)
            val indicatorColor = if (task.isCompleted) {
                R.color.status_done
            } else if (task.title.contains("Urgent", ignoreCase = true)) {
                R.color.status_urgent
            } else {
                R.color.status_pending
            }
            
            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, indicatorColor)
            )

            binding.cbTask.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}

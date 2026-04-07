package com.ivoryapp.nurseflow.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.data.model.HandoverTask
import com.ivoryapp.nurseflow.databinding.ItemHandoverTaskBinding

class HandoverTaskAdapter(private val onToggle: (HandoverTask) -> Unit) :
    ListAdapter<HandoverTask, HandoverTaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemHandoverTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemHandoverTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(task: HandoverTask) {
            binding.tvTaskTitle.text = task.title
            binding.btnCompleteTask.text = if (task.isCompleted) "Selesai" else "Selesaikan"
            
            binding.btnCompleteTask.setOnClickListener {
                onToggle(task)
            }
            
            binding.root.setOnClickListener {
                onToggle(task)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<HandoverTask>() {
        override fun areItemsTheSame(oldItem: HandoverTask, newItem: HandoverTask): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HandoverTask, newItem: HandoverTask): Boolean = oldItem == newItem
    }
}

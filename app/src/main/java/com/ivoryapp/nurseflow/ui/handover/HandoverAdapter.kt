package com.ivoryapp.nurseflow.ui.handover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.Handover
import com.ivoryapp.nurseflow.data.model.HandoverTask
import com.ivoryapp.nurseflow.databinding.ItemHandoverPatientBinding
import com.ivoryapp.nurseflow.databinding.ItemHandoverTaskBinding

class HandoverAdapter(
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val currentUserId: String,
    private val onTaskComplete: (HandoverTask, String) -> Unit, // Single action: complete & report
    private val onAcceptHandover: (String, Int) -> Unit,
    private val onRejectHandover: (String) -> Unit,
    private val onCompleteHandover: (String) -> Unit,
    private val onNavigateToTasks: (String, String) -> Unit,
    private val onShowPatientDetail: (Int) -> Unit,
    private val getTasksLiveData: (String) -> androidx.lifecycle.LiveData<List<HandoverTask>>
) : ListAdapter<Handover, HandoverAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHandoverPatientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHandoverPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Handover) {
            binding.tvPatientName.text = item.patientName
            val isSender = item.fromUid == currentUserId
            binding.tvHandoverInfo.text = if (isSender) "Penerima: ${item.toUid}" else "Pengirim: ${item.fromName}"
            
            binding.tvStatusBadge.text = item.status
            binding.tvStatusBadge.setChipBackgroundColorResource(when(item.status) {
                "ACCEPTED" -> R.color.status_done
                "PENDING" -> R.color.status_pending
                else -> R.color.status_urgent
            })

            // Setup Task Checklist Adapter
            val taskAdapter = TaskChecklistAdapter { task ->
                showCompleteConfirmation(task, item.fromUid)
            }
            binding.rvTasks.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvTasks.adapter = taskAdapter

            // Logika Visibilitas
            if (!isSender && item.status == "PENDING") {
                binding.layoutActionButtons.visibility = View.VISIBLE
                binding.rvTasks.visibility = View.GONE
                binding.labelTasks.visibility = View.GONE
                binding.tvProgress.visibility = View.GONE
                binding.btnAcceptHandover.setOnClickListener { onAcceptHandover(item.id, item.patientId) }
                binding.btnRejectHandover.setOnClickListener { onRejectHandover(item.id) }
            } else {
                binding.layoutActionButtons.visibility = View.GONE
                binding.rvTasks.visibility = View.VISIBLE
                binding.labelTasks.visibility = View.VISIBLE
                binding.tvProgress.visibility = View.VISIBLE
            }

            binding.root.setOnLongClickListener {
                showPopupMenu(it, item)
                true
            }

            getTasksLiveData(item.id).observe(lifecycleOwner) { tasks ->
                taskAdapter.submitList(tasks)
                val completedCount = tasks.count { it.isCompleted }
                binding.tvProgress.text = "$completedCount/${tasks.size} Selesai"

                if (!isSender && item.status == "ACCEPTED") {
                    binding.btnCompleteHandover.visibility = View.VISIBLE
                    binding.btnCompleteHandover.isEnabled = tasks.isNotEmpty() && tasks.all { it.isCompleted }
                    binding.btnCompleteHandover.setOnClickListener { onCompleteHandover(item.id) }
                } else {
                    binding.btnCompleteHandover.visibility = View.GONE
                }
            }
        }

        private fun showCompleteConfirmation(task: HandoverTask, fromUid: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Konfirmasi Penyelesaian")
                .setMessage("Pastikan tugas sudah benar-benar selesai sebelum dikirim.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Ya, Selesaikan & Kirim") { _, _ ->
                    onTaskComplete(task, fromUid)
                }
                .show()
        }

        private fun showPopupMenu(view: View, item: Handover) {
            val popup = PopupMenu(view.context, view)
            popup.menu.add(0, 1, 0, "Detail Pasien")
            popup.menu.add(0, 2, 1, "Daftar Handover Task")
            popup.setOnMenuItemClickListener { menu ->
                when (menu.itemId) {
                    1 -> { onShowPatientDetail(item.patientId); true }
                    2 -> { onNavigateToTasks(item.id, item.fromName); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Handover>() {
        override fun areItemsTheSame(oldItem: Handover, newItem: Handover): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Handover, newItem: Handover): Boolean = oldItem == newItem
    }
}

class TaskChecklistAdapter(private val onComplete: (HandoverTask) -> Unit) : 
    ListAdapter<HandoverTask, TaskChecklistAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHandoverTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val current = getItem(position)
        val next = if (position + 1 < itemCount) getItem(position + 1) else null
        holder.bind(current, next)
    }

    inner class ViewHolder(private val binding: ItemHandoverTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: HandoverTask, nextTask: HandoverTask?) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskCreator.text = "👤 Dibuat oleh: ${task.createdBy}"
            binding.tvTaskCreator.visibility = View.VISIBLE

            // UI Feedback untuk status Selesai
            if (task.isCompleted) {
                binding.btnCompleteTask.text = "✓ Selesai"
                binding.btnCompleteTask.isEnabled = false
                binding.btnCompleteTask.alpha = 0.6f
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskTitle.alpha = 0.6f
            } else {
                binding.btnCompleteTask.text = "Selesai"
                binding.btnCompleteTask.isEnabled = true
                binding.btnCompleteTask.alpha = 1.0f
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskTitle.alpha = 1.0f
                binding.btnCompleteTask.setOnClickListener { onComplete(task) }
            }

            // Divider Pemisah antar Pembuat Task
            if (nextTask != null && nextTask.createdBy != task.createdBy) {
                binding.lineCreatorDivider.visibility = View.VISIBLE
            } else {
                binding.lineCreatorDivider.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HandoverTask>() {
        override fun areItemsTheSame(oldItem: HandoverTask, newItem: HandoverTask): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HandoverTask, newItem: HandoverTask): Boolean = oldItem == newItem
    }
}

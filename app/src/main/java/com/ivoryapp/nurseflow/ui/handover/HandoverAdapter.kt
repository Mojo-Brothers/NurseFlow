package com.ivoryapp.nurseflow.ui.handover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.PatientHandover
import com.ivoryapp.nurseflow.databinding.ItemHandoverPatientBinding

class HandoverAdapter(
    private val onEditBriefing: (PatientHandover) -> Unit,
    private val onDiscussedChanged: (PatientHandover, Boolean) -> Unit
) : ListAdapter<PatientHandover, HandoverAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(item: PatientHandover) {
            binding.tvPatientName.text = item.patientName
            binding.tvRoomNumber.text = "Room ${item.roomNumber}"
            binding.tvLastVitals.text = item.lastVitalsSummary
            binding.tvSummary.text = item.summary.ifEmpty { "Belum ada ringkasan kondisi." }
            
            // Set NEWS2 Badge
            binding.tvNews2Score.text = "NEWS2: ${item.latestNews2Score}"
            val badgeColor = when {
                item.latestNews2Score >= 7 -> R.color.status_urgent
                item.latestNews2Score >= 5 -> R.color.status_pending
                item.latestNews2Score >= 3 -> R.color.status_warning
                else -> R.color.status_done
            }
            binding.cvNews2Badge.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, badgeColor)
            )

            // Discussed state
            binding.cbDiscussed.setOnCheckedChangeListener(null)
            binding.cbDiscussed.isChecked = item.isDiscussed
            binding.root.alpha = if (item.isDiscussed) 0.6f else 1.0f

            binding.cbDiscussed.setOnCheckedChangeListener { _, isChecked ->
                onDiscussedChanged(item, isChecked)
            }

            binding.btnEditBriefing.setOnClickListener {
                onEditBriefing(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PatientHandover>() {
        override fun areItemsTheSame(oldItem: PatientHandover, newItem: PatientHandover): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PatientHandover, newItem: PatientHandover): Boolean =
            oldItem == newItem
    }
}

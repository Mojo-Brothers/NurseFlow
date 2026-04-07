package com.ivoryapp.nurseflow.ui.patient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.databinding.ItemVitalSignBinding
import java.text.SimpleDateFormat
import java.util.*

class VitalSignAdapter : ListAdapter<VitalSign, VitalSignAdapter.VitalViewHolder>(VitalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VitalViewHolder {
        val binding = ItemVitalSignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VitalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VitalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VitalViewHolder(private val binding: ItemVitalSignBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(vital: VitalSign) {
            binding.tvTimestamp.text = dateFormat.format(Date(vital.timestamp))
            binding.tvBp.text = binding.root.context.getString(R.string.label_bp, vital.systolic, vital.diastolic)
            binding.tvPulse.text = binding.root.context.getString(R.string.label_pulse, vital.pulse)
            binding.tvTemp.text = binding.root.context.getString(R.string.label_temp, vital.temperature)
            binding.tvResp.text = binding.root.context.getString(R.string.label_resp, vital.respiration)
        }
    }

    class VitalDiffCallback : DiffUtil.ItemCallback<VitalSign>() {
        override fun areItemsTheSame(oldItem: VitalSign, newItem: VitalSign): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: VitalSign, newItem: VitalSign): Boolean = oldItem == newItem
    }
}

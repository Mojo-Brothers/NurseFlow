package com.ivoryapp.nurseflow.ui.patient

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.databinding.ItemVitalSignBinding
import com.ivoryapp.nurseflow.util.VitalSignAnalyzer
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
            val context = binding.root.context
            val score = VitalSignAnalyzer.calculateNEWS2(vital)
            val riskLevel = VitalSignAnalyzer.getRiskLevel(score)
            val suggestion = VitalSignAnalyzer.getClinicalSuggestion(vital, score)

            binding.tvTimestamp.text = dateFormat.format(Date(vital.timestamp))
            
            binding.tvBp.text = "${vital.systolic}/${vital.diastolic}"
            binding.tvPulse.text = vital.pulse.toString()
            binding.tvSpo2.text = if (vital.spo2 != null) "${vital.spo2}%" else "-"
            binding.tvTemp.text = "${vital.temperature}°C"
            binding.tvResp.text = vital.respiration.toString()
            
            binding.tvEwsScore.text = score.toString()
            binding.tvClinicalInsight.text = suggestion
            
            // Risk Badge Styling using defined colors
            val (badgeText, badgeColorRes, bgAlphaRes) = when(riskLevel) {
                VitalSignAnalyzer.RiskLevel.LOW -> Triple(R.string.risk_low, R.color.risk_low, R.color.risk_low_bg)
                VitalSignAnalyzer.RiskLevel.MEDIUM -> Triple(R.string.risk_medium, R.color.risk_medium, R.color.risk_medium_bg)
                VitalSignAnalyzer.RiskLevel.HIGH -> Triple(R.string.risk_high, R.color.risk_high, R.color.risk_high_bg)
            }

            binding.tvRiskBadge.setText(badgeText)
            val color = ContextCompat.getColor(context, badgeColorRes)
            val bgColor = ContextCompat.getColor(context, bgAlphaRes)
            
            binding.tvRiskBadge.backgroundTintList = ColorStateList.valueOf(bgColor)
            binding.tvRiskBadge.setTextColor(color)
            
            binding.tvEwsScore.setTextColor(color)
            binding.ivInsightIcon.imageTintList = ColorStateList.valueOf(color)
        }
    }

    class VitalDiffCallback : DiffUtil.ItemCallback<VitalSign>() {
        override fun areItemsTheSame(oldItem: VitalSign, newItem: VitalSign): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: VitalSign, newItem: VitalSign): Boolean = oldItem == newItem
    }
}

package com.ivoryapp.nurseflow.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.FragmentIvCalculatorBinding
import kotlin.math.roundToInt

class IVCalculatorFragment : Fragment() {

    private var _binding: FragmentIvCalculatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIvCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCalculate.setOnClickListener {
            calculateDripRate()
        }
    }

    private fun calculateDripRate() {
        val volumeStr = binding.etVolume.text.toString()
        val timeStr = binding.etTime.text.toString()
        val dropFactorStr = binding.etDropFactor.text.toString()

        if (volumeStr.isEmpty() || timeStr.isEmpty() || dropFactorStr.isEmpty()) {
            return
        }

        val volume = volumeStr.toDouble()
        val timeInHours = timeStr.toDouble()
        val dropFactor = dropFactorStr.toDouble()

        if (timeInHours <= 0) return

        val timeInMinutes = timeInHours * 60
        val dripRate = (volume * dropFactor) / timeInMinutes

        binding.tvResultRate.text = getString(R.string.result_drip_rate, dripRate.roundToInt())
        binding.cvResult.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.ivoryapp.nurseflow.ui.patient

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.databinding.DialogAddVitalSignBinding

class AddVitalSignDialog(
    private val existingVital: VitalSign? = null,
    private val onSave: (Int, Int, Int, Double, Int, Int) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddVitalSignBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddVitalSignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill data if editing
        existingVital?.let {
            binding.etSystolic.setText(it.systolic.toString())
            binding.etDiastolic.setText(it.diastolic.toString())
            binding.etPulse.setText(it.pulse.toString())
            binding.etTemp.setText(it.temperature.toString())
            binding.etResp.setText(it.respiration.toString())
            binding.etSpo2.setText(it.spo2?.toString() ?: "")
        }

        binding.btnSave.setOnClickListener {
            val systolic = binding.etSystolic.text.toString().toIntOrNull() ?: 0
            val diastolic = binding.etDiastolic.text.toString().toIntOrNull() ?: 0
            val pulse = binding.etPulse.text.toString().toIntOrNull() ?: 0
            val temp = binding.etTemp.text.toString().toDoubleOrNull() ?: 0.0
            val resp = binding.etResp.text.toString().toIntOrNull() ?: 0
            val spo2 = binding.etSpo2.text.toString().toIntOrNull() ?: 0

            onSave(systolic, diastolic, pulse, temp, resp, spo2)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddVitalSignDialog"
    }
}

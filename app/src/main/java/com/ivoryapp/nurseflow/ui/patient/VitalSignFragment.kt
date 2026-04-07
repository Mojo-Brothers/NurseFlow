package com.ivoryapp.nurseflow.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.databinding.FragmentVitalSignsBinding

class VitalSignFragment : Fragment() {

    private var _binding: FragmentVitalSignsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VitalSignViewModel by viewModels {
        VitalSignViewModelFactory((requireActivity().application as NurseFlowApplication).vitalSignRepository)
    }

    // Default patient ID for MVP (will be dynamic in v2)
    private val currentPatientId = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVitalSignsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = VitalSignAdapter()
        binding.rvVitalSigns.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVitalSigns.adapter = adapter

        viewModel.getVitalSigns(currentPatientId).observe(viewLifecycleOwner) { vitals ->
            adapter.submitList(vitals)
        }

        binding.fabAddVital.setOnClickListener {
            // For MVP, we'll insert a dummy record to show it works.
            // In the next step, we'll build the Add Vital Sign Dialog.
            viewModel.addVitalSign(
                patientId = currentPatientId,
                systolic = 120,
                diastolic = 80,
                pulse = 75,
                temperature = 36.5,
                respiration = 18,
                spo2 = 98
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

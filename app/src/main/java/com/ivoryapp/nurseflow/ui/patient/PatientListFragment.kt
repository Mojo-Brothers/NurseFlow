package com.ivoryapp.nurseflow.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.FragmentPatientsBinding

class PatientListFragment : Fragment() {

    private var _binding: FragmentPatientsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as NurseFlowApplication).patientRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        val adapter = PatientAdapter { patient ->
            val bundle = Bundle().apply {
                putInt("patientId", patient.id)
            }
            findNavController().navigate(R.id.action_patientListFragment_to_vitalSignFragment, bundle)
        }
        binding.rvPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatients.adapter = adapter

        viewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            adapter.submitList(patients)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPatient.setOnClickListener {
            val dialog = AddPatientDialog { name, age, dob, room, condition ->
                viewModel.addPatient(name, age, dob, room, condition)
            }
            dialog.show(parentFragmentManager, AddPatientDialog.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

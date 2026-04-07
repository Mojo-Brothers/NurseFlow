package com.ivoryapp.nurseflow.ui.home

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
import com.ivoryapp.nurseflow.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((requireActivity().application as NurseFlowApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        val adapter = TaskAdapter { task, isChecked ->
            viewModel.updateTask(task, isChecked)
        }
        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTask.setOnClickListener {
            viewModel.addTask("Shift Duty", "Check patient status in Room A1")
        }

        binding.btnIvCalc.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_ivCalculatorFragment)
        }
        
        binding.btnVitals.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_vitalSignFragment)
        }
        
        // Placeholders for other MVP features
        binding.btnAiNotes.setOnClickListener { /* Navigate to AI Notes */ }
        binding.btnPatients.setOnClickListener { /* Navigate to Patients List */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

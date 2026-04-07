package com.ivoryapp.nurseflow.ui.handover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.FragmentHandoverBinding

class HandoverFragment : Fragment() {

    private var _binding: FragmentHandoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HandoverViewModel by viewModels {
        val app = requireActivity().application as NurseFlowApplication
        HandoverViewModelFactory(app.handoverRepository)
    }

    private lateinit var adapter: HandoverAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHandoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnStartHandover.setOnClickListener {
            findNavController().navigate(R.id.action_handoverFragment_to_colleaguesFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = HandoverAdapter(
            lifecycleOwner = viewLifecycleOwner,
            currentUserId = currentUserId,
            onTaskComplete = { task, fromUid ->
                viewModel.toggleTask(task.id, true, task.handoverId)
                Toast.makeText(requireContext(), "Tugas selesai & laporan dikirim", Toast.LENGTH_SHORT).show()
            },
            onAcceptHandover = { handoverId, patientId ->
                viewModel.acceptHandover(handoverId, patientId)
            },
            onRejectHandover = { handoverId ->
                viewModel.rejectHandover(handoverId)
            },
            onCompleteHandover = { handoverId ->
                viewModel.completeHandover(handoverId)
                Toast.makeText(requireContext(), "Handover Selesai & Tanggung Jawab Berpindah", Toast.LENGTH_LONG).show()
            },
            onNavigateToTasks = { handoverId, creatorName ->
                // Navigasi ke halaman daftar task detail jika diperlukan
                Toast.makeText(requireContext(), "Membuka daftar tugas detail", Toast.LENGTH_SHORT).show()
            },
            onShowPatientDetail = { patientId ->
                val bundle = Bundle().apply { putInt("patientId", patientId) }
                findNavController().navigate(R.id.action_handoverFragment_to_patientListFragment, bundle)
            },
            getTasksLiveData = { handoverId ->
                viewModel.getTasksForHandover(handoverId)
            }
        )
        binding.rvHandoverItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHandoverItems.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.activeHandovers.observe(viewLifecycleOwner) { handovers ->
            if (handovers.isEmpty()) {
                binding.layoutStartHandover.visibility = View.VISIBLE
                binding.rvHandoverItems.visibility = View.GONE
                binding.tvHandoverTitleEmpty.text = "Belum ada Handover aktif"
            } else {
                binding.layoutStartHandover.visibility = View.GONE
                binding.rvHandoverItems.visibility = View.VISIBLE
                adapter.submitList(handovers)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

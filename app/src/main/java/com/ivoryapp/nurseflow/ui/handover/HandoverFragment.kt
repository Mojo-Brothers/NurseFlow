package com.ivoryapp.nurseflow.ui.handover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.PatientHandover
import com.ivoryapp.nurseflow.databinding.FragmentHandoverBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HandoverFragment : Fragment() {

    private var _binding: FragmentHandoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HandoverViewModel by viewModels {
        val app = requireActivity().application as NurseFlowApplication
        HandoverViewModelFactory(app.handoverRepository)
    }

    private lateinit var adapter: HandoverAdapter

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

        binding.btnStartHandover.setOnClickListener {
            showHandoverSourceDialog()
        }

        binding.btnFinalizeHandover.setOnClickListener {
            showFinalizeConfirmation()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = HandoverAdapter(
            onEditBriefing = { item -> showEditBriefingDialog(item) },
            onDiscussedChanged = { item, isDiscussed ->
                viewModel.updateHandoverItem(item.copy(isDiscussed = isDiscussed))
            }
        )
        binding.rvHandoverItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHandoverItems.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.currentSession.observe(viewLifecycleOwner) { session ->
            if (session != null) {
                binding.layoutStartHandover.visibility = View.GONE
                binding.layoutSessionInfo.visibility = View.VISIBLE
                binding.rvHandoverItems.visibility = View.VISIBLE
                binding.btnFinalizeHandover.visibility = View.VISIBLE

                binding.chipShiftType.text = "Shift ${session.shiftType}"
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.tvSessionTime.text = "${sdf.format(session.startTime.toDate())} - Sekarang"
            } else {
                binding.layoutStartHandover.visibility = View.VISIBLE
                binding.layoutSessionInfo.visibility = View.GONE
                binding.rvHandoverItems.visibility = View.GONE
                binding.btnFinalizeHandover.visibility = View.GONE
            }
        }

        viewModel.handoverItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
    }

    private fun showHandoverSourceDialog() {
        val options = arrayOf("Pasien Saya Sendiri", "Terima Operan dari Rekan")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Pasien")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showShiftSelectionDialog(null)
                } else {
                    fetchAndShowColleagues()
                }
            }
            .show()
    }

    private fun fetchAndShowColleagues() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val app = requireActivity().application as NurseFlowApplication
            val colleagues = app.handoverRepository.getColleagues()
            binding.progressBar.visibility = View.GONE

            if (colleagues.isEmpty()) {
                Toast.makeText(requireContext(), "Belum ada rekan kerja yang terhubung", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = colleagues.map { it.second }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Pilih Rekan Kerja")
                .setItems(names) { _, which ->
                    showShiftSelectionDialog(colleagues[which].first)
                }
                .show()
        }
    }

    private fun showShiftSelectionDialog(fromColleagueUid: String?) {
        val shifts = arrayOf("Pagi", "Siang", "Malam")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Shift Anda")
            .setItems(shifts) { _, which ->
                viewModel.startHandover(shifts[which], fromColleagueUid)
            }
            .show()
    }

    private fun showEditBriefingDialog(item: PatientHandover) {
        val editText = EditText(requireContext()).apply {
            setText(item.summary)
            setSelection(item.summary.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Briefing: ${item.patientName}")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val newSummary = editText.text.toString()
                viewModel.updateHandoverItem(item.copy(summary = newSummary))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showFinalizeConfirmation() {
        val items = viewModel.handoverItems.value ?: emptyList()
        val undiscussedCount = items.count { !it.isDiscussed }

        val message = if (undiscussedCount > 0) {
            "Masih ada $undiscussedCount pasien yang belum dibahas. Yakin ingin mengakhiri operan?"
        } else {
            "Semua pasien telah dibahas. Akhiri sesi operan dan pindahkan tanggung jawab pasien?"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selesai Operan")
            .setMessage(message)
            .setPositiveButton("Ya, Selesai") { _, _ ->
                viewModel.completeHandover()
                Toast.makeText(requireContext(), "Operan Selesai & Tanggung Jawab Dipindahkan", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Kembali", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

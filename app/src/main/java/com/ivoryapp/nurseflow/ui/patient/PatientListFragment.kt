package com.ivoryapp.nurseflow.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.Patient
import com.ivoryapp.nurseflow.databinding.FragmentPatientsBinding
import kotlinx.coroutines.launch

class PatientListFragment : Fragment() {

    private var _binding: FragmentPatientsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as NurseFlowApplication).patientRepository)
    }

    private lateinit var patientAdapter: PatientAdapter
    private var allPatientsList: List<Patient> = emptyList()
    private var isColleagueView: Boolean = false
    private var isSelectionMode: Boolean = false

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

        val targetUid = arguments?.getString("targetUid")
        val targetName = arguments?.getString("targetName")
        isSelectionMode = arguments?.getBoolean("isSelectionMode") ?: false
        isColleagueView = targetUid != null

        setupToolbar(targetName)
        setupRecyclerView()
        setupClickListeners()
        setupSearchView()

        if (isColleagueView && targetUid != null) {
            binding.fabAddPatient.visibility = View.GONE
            viewModel.loadColleaguePatients(targetUid)
            viewModel.colleaguePatients.observe(viewLifecycleOwner) { patients ->
                updatePatientList(patients)
            }
        } else {
            viewModel.allPatients.observe(viewLifecycleOwner) { patients ->
                updatePatientList(patients)
            }
        }
    }

    private fun setupToolbar(targetName: String?) {
        binding.toolbar.title = if (isSelectionMode) "Pilih Pasien" else if (targetName != null) "Pasien $targetName" else "Daftar Pasien Anda"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter { patient ->
            if (isSelectionMode) {
                // Balik ke AI Notes dengan membawa patientId
                val bundle = Bundle().apply {
                    putInt("patientId", patient.id)
                }
                findNavController().navigate(R.id.action_patientListFragment_to_aiNotesFragment, bundle)
            } else {
                if (!isColleagueView) {
                    showActionDialog(patient)
                } else {
                    navigateToVitals(patient)
                }
            }
        }
        binding.rvPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatients.adapter = patientAdapter
    }

    private fun showActionDialog(patient: Patient) {
        val options = arrayOf("Lihat Vital Signs", "Handover ke Rekan")
        AlertDialog.Builder(requireContext())
            .setTitle(patient.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToVitals(patient)
                    1 -> showColleagueSelectionForHandover(patient)
                }
            }
            .show()
    }

    private fun showColleagueSelectionForHandover(patient: Patient) {
        val app = requireActivity().application as NurseFlowApplication
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val colleagues = app.handoverRepository.getColleagues()
            binding.progressBar.visibility = View.GONE

            if (colleagues.isEmpty()) {
                Toast.makeText(requireContext(), "Belum ada rekan kerja yang terhubung", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = colleagues.map { it.second }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Handover ${patient.name} ke:")
                .setItems(names) { _, which ->
                    val selectedColleague = colleagues[which]
                    confirmHandover(patient, selectedColleague.first, selectedColleague.second)
                }
                .show()
        }
    }

    private fun confirmHandover(patient: Patient, toUid: String, toName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Handover")
            .setMessage("Kirim permintaan handover untuk ${patient.name} kepada $toName?")
            .setPositiveButton("Kirim") { _, _ ->
                performHandover(patient, toUid)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performHandover(patient: Patient, toUid: String) {
        val app = requireActivity().application as NurseFlowApplication
        val fromName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Nurse"
        
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                app.handoverRepository.sendPatientHandover(patient, toUid, fromName)
                Toast.makeText(requireContext(), "Permintaan handover dikirim", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal mengirim handover", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun navigateToVitals(patient: Patient) {
        val bundle = Bundle().apply {
            putInt("patientId", patient.id)
            putString("patientName", patient.name)
            putBoolean("isColleagueView", isColleagueView)
        }
        findNavController().navigate(R.id.action_patientListFragment_to_vitalSignFragment, bundle)
    }

    private fun updatePatientList(patients: List<Patient>) {
        allPatientsList = patients
        patientAdapter.submitList(patients)
        binding.tvEmpty.visibility = if (patients.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        binding.fabAddPatient.setOnClickListener {
            val dialog = AddPatientDialog { name, age, dob, room, condition ->
                viewModel.addPatient(name, age, dob, room, condition)
            }
            dialog.show(parentFragmentManager, AddPatientDialog.TAG)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPatients(newText)
                return true
            }
        })
    }

    private fun filterPatients(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allPatientsList
        } else {
            allPatientsList.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.roomNumber.contains(query, ignoreCase = true)
            }
        }
        patientAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

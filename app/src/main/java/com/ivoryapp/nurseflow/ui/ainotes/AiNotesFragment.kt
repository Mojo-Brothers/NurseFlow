package com.ivoryapp.nurseflow.ui.ainotes

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.databinding.FragmentAiNotesBinding
import com.ivoryapp.nurseflow.ui.patient.PatientViewModel
import com.ivoryapp.nurseflow.ui.patient.PatientViewModelFactory
import com.ivoryapp.nurseflow.ui.patient.VitalSignViewModel
import com.ivoryapp.nurseflow.ui.patient.VitalSignViewModelFactory
import com.ivoryapp.nurseflow.util.VitalSignAnalyzer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiNotesFragment : Fragment() {

    private var _binding: FragmentAiNotesBinding? = null
    private val binding get() = _binding!!

    private val patientViewModel: PatientViewModel by viewModels {
        PatientViewModelFactory((requireActivity().application as NurseFlowApplication).patientRepository)
    }

    private val vitalSignViewModel: VitalSignViewModel by viewModels {
        val app = requireActivity().application as NurseFlowApplication
        VitalSignViewModelFactory(app.vitalSignRepository, app.patientRepository)
    }

    private var currentPatientId = -1
    private var currentPatientName = ""
    private var currentAiAnalysis = ""
    private var isFromNotification = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentPatientId = arguments?.getInt("patientId") ?: -1
        val receivedSummary = arguments?.getString("aiSummary")
        isFromNotification = arguments?.getBoolean("isFromNotification") ?: false

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        if (currentPatientId != -1) {
            loadPatientData(currentPatientId)
            
            // Jika dikirim dari notifikasi, tampilkan summary yang diterima
            if (isFromNotification && !receivedSummary.isNullOrEmpty()) {
                displayReceivedSummary(receivedSummary)
            }
        }

        binding.cardPatientInfo.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isSelectionMode", true)
            }
            findNavController().navigate(R.id.action_aiNotesFragment_to_patientListFragment, bundle)
        }

        binding.btnGenerateReport.setOnClickListener {
            if (currentAiAnalysis.isNotEmpty()) {
                shareProfessionalReport()
            } else {
                Toast.makeText(requireContext(), "Pilih pasien dan tunggu analisis AI selesai", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRemindColleague.setOnClickListener {
            if (currentPatientId != -1) {
                val bundle = Bundle().apply {
                    putInt("patientId", currentPatientId)
                    putString("patientName", currentPatientName)
                    putString("aiSummary", currentAiAnalysis) // Kirim summary yang ada sekarang
                }
                findNavController().navigate(R.id.action_aiNotesFragment_to_colleagueSelectionFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Pilih pasien terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayReceivedSummary(summary: String) {
        binding.tvAiSummary.text = "📬 ANALISIS TERIMA DARI REKAN:\n\n$summary"
        currentAiAnalysis = summary
        binding.progressAi.visibility = View.GONE
        
        // Tandai bahwa ini data kiriman agar tidak di-overwrite otomatis oleh analisis lokal segera
        binding.tvAiSummary.append("\n\n(Data ini dikirim oleh rekan kerja)")
    }

    private fun loadPatientData(id: Int) {
        patientViewModel.allPatients.observe(viewLifecycleOwner) { patients ->
            val patient = patients.find { it.id == id }
            patient?.let {
                currentPatientId = it.id
                currentPatientName = it.name
                binding.tvPatientName.text = it.name
                binding.tvPatientDetails.text = "Room ${it.roomNumber} | ${it.conditionBrief}"
                binding.btnRemindColleague.visibility = View.VISIBLE
                
                // Jika bukan dari notifikasi, lakukan analisis otomatis
                if (!isFromNotification) {
                    vitalSignViewModel.getVitalSigns(id).observe(viewLifecycleOwner) { vitals ->
                        analyzeVitalsWithAi(vitals)
                    }
                }
            }
        }
    }

    private fun analyzeVitalsWithAi(vitals: List<VitalSign>) {
        if (vitals.isEmpty()) {
            binding.tvAiSummary.text = "Data vital sign tidak mencukupi untuk analisis AI."
            currentAiAnalysis = ""
            return
        }

        binding.progressAi.visibility = View.VISIBLE
        binding.tvAiSummary.text = "AI sedang menganalisis tren klinis..."

        Handler(Looper.getMainLooper()).postDelayed({
            val latestVital = vitals.maxByOrNull { it.timestamp }
            val avgScore = vitals.map { VitalSignAnalyzer.calculateNEWS2(it) }.average()
            
            val analysis = StringBuilder()
            analysis.append("Berdasarkan analisis tren NEWS2:\n\n")
            
            when {
                avgScore >= 5 -> analysis.append("🔴 PERINGATAN: Pasien menunjukkan tren risiko menengah ke tinggi. Skor NEWS2 rata-rata adalah ${String.format("%.1f", avgScore)}.\n")
                avgScore >= 3 -> analysis.append("🟡 OBSERVASI: Pasien dalam risiko rendah-menengah. Diperlukan pemantauan lebih sering.\n")
                else -> analysis.append("🟢 STABIL: Kondisi pasien cenderung stabil secara klinis.\n")
            }

            latestVital?.let {
                if (it.spo2 != null && it.spo2 < 94) {
                    analysis.append("• Deteksi Hipoksia: Saturasi oksigen rendah (${it.spo2}%).\n")
                }
                if (it.systolic > 160) {
                    analysis.append("• Hipertensi: Tekanan darah sistolik tinggi.\n")
                }
            }

            analysis.append("\nSaran: Lanjutkan protokol observasi rutin dan pastikan dokumentasi lengkap.")
            
            currentAiAnalysis = analysis.toString()
            binding.tvAiSummary.text = currentAiAnalysis
            binding.progressAi.visibility = View.GONE
        }, 1500)
    }

    private fun shareProfessionalReport() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val report = """
            📋 LAPORAN KLINIS PROFESIONAL (NURSEFLOW)
            Waktu Laporan: ${sdf.format(Date())}
            
            IDENTITAS PASIEN:
            Nama: $currentPatientName
            Detail: ${binding.tvPatientDetails.text}
            
            RINGKASAN ANALISIS AI:
            $currentAiAnalysis
            
            -----------------------------------
            Laporan ini dibuat otomatis melalui NurseFlow AI Clinical Assistant.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Klinis - $currentPatientName")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(intent, "Bagikan Laporan via:"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.ivoryapp.nurseflow.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivoryapp.nurseflow.databinding.FragmentColleagueSelectionBinding
import com.ivoryapp.nurseflow.ui.home.Colleague
import com.ivoryapp.nurseflow.ui.home.ColleagueAdapter
import com.ivoryapp.nurseflow.ui.notification.NotificationType

class ColleagueSelectionFragment : Fragment() {

    private var _binding: FragmentColleagueSelectionBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var colleagueAdapter: ColleagueAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColleagueSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val patientId = arguments?.getInt("patientId") ?: -1
        val patientName = arguments?.getString("patientName") ?: "Pasien"
        val aiSummary = arguments?.getString("aiSummary") // Summary dari AI

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        setupRecyclerView(patientId, patientName, aiSummary)
        loadColleagues()
    }

    private fun setupRecyclerView(patientId: Int, patientName: String, aiSummary: String?) {
        colleagueAdapter = ColleagueAdapter(
            onClick = { colleague ->
                sendReminder(colleague, patientId, patientName, aiSummary)
            }
        )
        binding.rvColleagues.layoutManager = LinearLayoutManager(requireContext())
        binding.rvColleagues.adapter = colleagueAdapter
    }

    private fun loadColleagues() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("connections")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val partnerIds = mutableSetOf<String>()
                for (doc in snapshot.documents) {
                    val members = doc.get("members") as? List<String>
                    val partnerId = members?.firstOrNull { it != uid }
                    if (partnerId != null) partnerIds.add(partnerId)
                }

                if (partnerIds.isNotEmpty()) {
                    fetchColleagueDetails(partnerIds.toList())
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Belum ada rekan kerja terhubung", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun fetchColleagueDetails(userIds: List<String>) {
        val colleagues = mutableListOf<Colleague>()
        var loadedCount = 0

        for (id in userIds) {
            firestore.collection("users").document(id).get()
                .addOnSuccessListener { doc ->
                    colleagues.add(Colleague(
                        uid = id,
                        name = doc.getString("name") ?: "Nurse",
                        userCode = doc.getString("userCode") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    ))
                    loadedCount++
                    if (loadedCount == userIds.size) {
                        colleagueAdapter.submitList(colleagues.sortedBy { it.name })
                        binding.progressBar.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    loadedCount++
                    if (loadedCount == userIds.size) {
                        colleagueAdapter.submitList(colleagues.sortedBy { it.name })
                        binding.progressBar.visibility = View.GONE
                    }
                }
        }
    }

    private fun sendReminder(colleague: Colleague, patientId: Int, patientName: String, aiSummary: String?) {
        val currentUser = auth.currentUser
        val senderUid = currentUser?.uid ?: return
        val senderName = currentUser.displayName ?: "Rekan Anda"
        
        binding.progressBar.visibility = View.VISIBLE

        val notificationData = hashMapOf(
            "userId" to colleague.uid,
            "senderId" to senderUid,
            "senderName" to senderName,
            "title" to "🤖 Analisis AI: $patientName",
            "message" to "$senderName mengirimkan analisis klinis terbaru untuk $patientName.",
            "aiSummary" to (aiSummary ?: "Tidak ada ringkasan tambahan."),
            "type" to NotificationType.CLINICAL_ALERT.name,
            "patientId" to patientId,
            "timestamp" to Timestamp.now(),
            "isRead" to false
        )

        firestore.collection("notifications").add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Reminder & Analisis dikirim!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal mengirim: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

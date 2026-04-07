package com.ivoryapp.nurseflow.ui.home

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.FragmentColleaguesBinding

class ColleaguesFragment : Fragment() {

    private var _binding: FragmentColleaguesBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var colleagueAdapter: ColleagueAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColleaguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadConnections()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        colleagueAdapter = ColleagueAdapter(
            onClick = { colleague ->
                val bundle = Bundle().apply {
                    putString("handoverTargetUid", colleague.uid)
                    putString("handoverTargetName", colleague.name)
                    putBoolean("isSelectionMode", true)
                }
                findNavController().navigate(R.id.action_colleaguesFragment_to_patientListFragment, bundle)
            },
            onLongClick = { colleague ->
                val index = colleagueAdapter.currentList.indexOf(colleague)
                if (index != -1) {
                    val viewHolder = binding.rvColleagues.findViewHolderForAdapterPosition(index)
                    showColleaguePopupMenu(viewHolder?.itemView ?: binding.rvColleagues, colleague)
                }
            }
        )
        binding.rvColleagues.adapter = colleagueAdapter
    }

    private fun showColleaguePopupMenu(anchor: View, colleague: Colleague) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Lihat Profil Rekan Kerja")
        popup.menu.add(0, 2, 1, "Lihat Daftar Pasien")
        
        val deleteItem = popup.menu.add(0, 3, 2, "Delete Rekan Kerja")
        val spannable = SpannableString(deleteItem.title)
        spannable.setSpan(ForegroundColorSpan(Color.RED), 0, spannable.length, 0)
        deleteItem.title = spannable

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> true
                2 -> {
                    val bundle = Bundle().apply {
                        putString("targetUid", colleague.uid)
                        putString("targetName", colleague.name)
                    }
                    findNavController().navigate(R.id.action_colleaguesFragment_to_patientListFragment, bundle)
                    true
                }
                3 -> {
                    showDeleteConfirmation(colleague)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            val code = binding.etMemberId.text.toString().trim().uppercase()
            if (code.isNotEmpty()) {
                sendConnectionRequest(code)
            } else {
                Toast.makeText(requireContext(), "Masukkan Member ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadConnections() {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)

        firestore.collection("connections")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                if (e != null || snapshot == null) {
                    setLoading(false)
                    return@addSnapshotListener
                }

                val partnerIds = mutableSetOf<String>()
                for (doc in snapshot.documents) {
                    val members = doc.get("members") as? List<String>
                    val partnerId = members?.firstOrNull { it != uid }
                    if (partnerId != null) partnerIds.add(partnerId)
                }

                if (partnerIds.isNotEmpty()) {
                    loadColleagues(partnerIds.toList())
                } else {
                    colleagueAdapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                    setLoading(false)
                }
            }
    }

    private fun loadColleagues(userIds: List<String>) {
        val colleagues = mutableListOf<Colleague>()
        var loadedCount = 0

        for (id in userIds) {
            firestore.collection("users").document(id).get()
                .addOnSuccessListener { doc ->
                    if (_binding == null) return@addOnSuccessListener
                    val colleague = Colleague(
                        uid = id,
                        name = doc.getString("name") ?: "Nurse",
                        userCode = doc.getString("userCode") ?: "",
                        photoUrl = doc.getString("photoUrl")
                    )
                    colleagues.add(colleague)
                    loadedCount++

                    if (loadedCount == userIds.size) {
                        colleagueAdapter.submitList(colleagues.sortedBy { it.name })
                        binding.tvEmpty.visibility = if (colleagues.isEmpty()) View.VISIBLE else View.GONE
                        setLoading(false)
                    }
                }
                .addOnFailureListener {
                    if (_binding == null) return@addOnFailureListener
                    loadedCount++
                    if (loadedCount == userIds.size) setLoading(false)
                }
        }
    }

    private fun sendConnectionRequest(inputCode: String) {
        val uid = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: "Rekan Perawat"
        setLoading(true)

        firestore.collection("users")
            .whereEqualTo("userCode", inputCode)
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@addOnSuccessListener
                }

                val targetUid = documents.documents[0].id
                if (targetUid == uid) {
                    Toast.makeText(requireContext(), "Ini ID Anda sendiri", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@addOnSuccessListener
                }

                // Cek apakah sudah ada permintaan PENDING
                firestore.collection("requests")
                    .whereEqualTo("fromUid", uid)
                    .whereEqualTo("toUid", targetUid)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { pendingDocs ->
                        if (!pendingDocs.isEmpty) {
                            Toast.makeText(requireContext(), "Permintaan sudah pernah dikirim dan masih pending", Toast.LENGTH_SHORT).show()
                            setLoading(false)
                            return@addOnSuccessListener
                        }

                        // Cek apakah sudah terhubung
                        firestore.collection("connections")
                            .whereArrayContains("members", uid)
                            .get()
                            .addOnSuccessListener { connDocs ->
                                val alreadyConnected = connDocs.any { 
                                    (it.get("members") as? List<*>)?.contains(targetUid) == true 
                                }
                                
                                if (alreadyConnected) {
                                    Toast.makeText(requireContext(), "Sudah terhubung", Toast.LENGTH_SHORT).show()
                                    setLoading(false)
                                    return@addOnSuccessListener
                                }

                                val requestData = hashMapOf(
                                    "fromUid" to uid,
                                    "fromName" to name,
                                    "toUid" to targetUid,
                                    "status" to "pending",
                                    "createdAt" to FieldValue.serverTimestamp()
                                )

                                firestore.collection("requests").add(requestData)
                                    .addOnSuccessListener {
                                        val notificationData = hashMapOf(
                                            "userId" to targetUid,
                                            "title" to "Permintaan Rekan Kerja",
                                            "message" to "$name ingin menambahkan Anda sebagai rekan kerja.",
                                            "type" to "FRIEND_REQUEST",
                                            "fromUid" to uid,
                                            "status" to "pending",
                                            "timestamp" to FieldValue.serverTimestamp(),
                                            "isRead" to false
                                        )
                                        firestore.collection("notifications").add(notificationData)
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Permintaan dikirim!", Toast.LENGTH_SHORT).show()
                                                binding.etMemberId.text?.clear()
                                                setLoading(false)
                                            }
                                    }
                            }
                    }
            }
            .addOnFailureListener {
                if (_binding != null) setLoading(false)
            }
    }

    private fun showDeleteConfirmation(colleague: Colleague) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Rekan Kerja")
            .setMessage("Apakah Anda yakin ingin memutuskan koneksi dengan ${colleague.name}?")
            .setPositiveButton("Hapus") { _, _ -> deleteConnection(colleague.uid) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteConnection(targetUid: String) {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)
        firestore.collection("connections")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val docToDelete = snapshot.documents.find { 
                    (it.get("members") as? List<*>)?.contains(targetUid) == true 
                }
                docToDelete?.reference?.delete()?.addOnSuccessListener {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Koneksi dihapus", Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        _binding?.let {
            it.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            it.btnConnect.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.ivoryapp.nurseflow.ui.notification

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: NotificationsAdapter
    private val TAG = "NotificationsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupSwipeToDelete()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onAccept = { notification -> acceptRequest(notification) },
            onReject = { notification -> rejectRequest(notification) },
            onReadStatusChange = { notification, isRead -> updateReadStatus(notification, isRead) },
            onClick = { notification ->
                if (notification.type == NotificationType.CLINICAL_ALERT && notification.patientId != null) {
                    val bundle = Bundle().apply {
                        putInt("patientId", notification.patientId)
                        putString("aiSummary", notification.aiSummary ?: "No summary available")
                        putBoolean("isFromNotification", true)
                    }
                    try {
                        findNavController().navigate(R.id.action_notificationsFragment_to_aiNotesFragment, bundle)
                        updateReadStatus(notification, true)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvNotifications.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val notification = adapter.currentList[position]
                    deleteNotification(notification)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvNotifications)
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("notifications")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                
                if (e != null) {
                    Log.e(TAG, "Error loading notifications: ", e)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    } catch (ex: Exception) {
                        null
                    }
                } ?: emptyList()

                adapter.submitList(notifications)
                binding.tvEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun updateReadStatus(notification: Notification, isRead: Boolean) {
        if (notification.id.isNotEmpty()) {
            firestore.collection("notifications").document(notification.id)
                .update("isRead", isRead)
        }
    }

    private fun deleteNotification(notification: Notification) {
        if (notification.id.isNotEmpty()) {
            firestore.collection("notifications").document(notification.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Notifikasi dihapus", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun acceptRequest(notification: Notification) {
        val uid = auth.currentUser?.uid ?: return
        val myName = auth.currentUser?.displayName ?: "Rekan Perawat"
        val fromUid = notification.fromUid ?: return

        Toast.makeText(requireContext(), "Menerima permintaan...", Toast.LENGTH_SHORT).show()

        // Ambil semua notifikasi dari pengirim yang sama untuk ditandai SELESAI
        firestore.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("fromUid", fromUid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { notifSnapshot ->
                
                // Cari permintaan di koleksi requests
                firestore.collection("requests")
                    .whereEqualTo("fromUid", fromUid)
                    .whereEqualTo("toUid", uid)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { reqSnapshot ->
                        
                        firestore.runBatch { batch ->
                            // 1. Update status semua notifikasi terkait
                            for (notifDoc in notifSnapshot.documents) {
                                batch.update(notifDoc.reference, 
                                    "status", "accepted", 
                                    "isRead", true)
                            }

                            // 2. Update status di koleksi requests
                            for (reqDoc in reqSnapshot.documents) {
                                batch.update(reqDoc.reference, "status", "accepted")
                            }

                            // 3. Buat koneksi baru
                            val connectionData = hashMapOf(
                                "members" to listOf(uid, fromUid),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            batch.set(firestore.collection("connections").document(), connectionData)

                            // 4. Kirim notifikasi balik ke User A (Pengirim)
                            val backNotification = hashMapOf(
                                "userId" to fromUid,
                                "title" to "Permintaan Diterima",
                                "message" to "$myName telah menerima permintaan rekan kerja Anda.",
                                "type" to "SYSTEM",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "isRead" to false
                            )
                            batch.set(firestore.collection("notifications").document(), backNotification)

                        }.addOnSuccessListener {
                            Toast.makeText(requireContext(), "Berhasil terhubung!", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun rejectRequest(notification: Notification) {
        val uid = auth.currentUser?.uid ?: return
        val fromUid = notification.fromUid ?: return

        Toast.makeText(requireContext(), "Menolak permintaan...", Toast.LENGTH_SHORT).show()

        firestore.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("fromUid", fromUid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { notifSnapshot ->
                firestore.collection("requests")
                    .whereEqualTo("fromUid", fromUid)
                    .whereEqualTo("toUid", uid)
                    .get()
                    .addOnSuccessListener { reqSnapshot ->
                        firestore.runBatch { batch ->
                            for (notifDoc in notifSnapshot.documents) {
                                batch.update(notifDoc.reference, 
                                    "status", "rejected", 
                                    "isRead", true)
                            }
                            for (reqDoc in reqSnapshot.documents) {
                                batch.update(reqDoc.reference, "status", "rejected")
                            }
                        }.addOnSuccessListener {
                            Toast.makeText(requireContext(), "Permintaan ditolak", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

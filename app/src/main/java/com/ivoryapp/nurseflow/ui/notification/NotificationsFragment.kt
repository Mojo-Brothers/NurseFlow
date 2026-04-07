package com.ivoryapp.nurseflow.ui.notification

import android.os.Bundle
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
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    val notif = doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    notif?.apply {
                        // Pastikan aiSummary terisi dari Firestore
                        if (aiSummary == null) {
                            aiSummary = doc.getString("aiSummary")
                        }
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
        val fromUid = notification.fromUid ?: return

        firestore.collection("requests").whereEqualTo("fromUid", fromUid).whereEqualTo("toUid", uid).get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.reference?.update("status", "accepted")
            }

        firestore.collection("notifications").document(notification.id)
            .update("status", "accepted", "isRead", true)
            .addOnSuccessListener {
                createConnection(uid, fromUid)
            }
    }

    private fun rejectRequest(notification: Notification) {
        val uid = auth.currentUser?.uid ?: return
        val fromUid = notification.fromUid ?: return

        firestore.collection("requests").whereEqualTo("fromUid", fromUid).whereEqualTo("toUid", uid).get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.reference?.update("status", "rejected")
            }

        firestore.collection("notifications").document(notification.id)
            .update("status", "rejected", "isRead", true)
    }

    private fun createConnection(uid1: String, uid2: String) {
        val connectionData = hashMapOf(
            "members" to listOf(uid1, uid2),
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("connections").add(connectionData)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

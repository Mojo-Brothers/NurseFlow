package com.ivoryapp.nurseflow.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.databinding.FragmentHomeBinding
import com.ivoryapp.nurseflow.ui.LoginActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var colleagueAdapter: ColleagueAdapter
    private var pendingRequestsCount = 0
    private var unreadNotificationsCount = 0
    
    // Registrations for cleanup
    private var requestsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var connectionsListener: ListenerRegistration? = null
    private var handoverListener: ListenerRegistration? = null

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

        setupUserHeader()
        setupColleagueRecyclerView()
        setupClickListeners()
        
        displayUserInfo()
        listenForTotalNotifications()
        loadConnections()
        listenForHandoverRequests()
    }

    private fun listenForHandoverRequests() {
        val uid = auth.currentUser?.uid ?: return
        
        handoverListener = firestore.collection("handover_requests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                
                val handoverDoc = snapshot?.documents?.firstOrNull()
                if (handoverDoc != null) {
                    binding.cardHandoverNotification.visibility = View.VISIBLE
                    val fromName = handoverDoc.getString("fromName") ?: "Rekan"
                    val patientName = handoverDoc.getString("patientName") ?: "Pasien"
                    binding.tvHandoverDesc.text = "$fromName ingin menyerahkan tanggung jawab pasien: $patientName"
                    
                    binding.btnAcceptHandover.setOnClickListener { 
                        acceptHandover(handoverDoc.id, handoverDoc.getLong("patientId")?.toInt() ?: 0)
                    }
                    binding.btnRejectHandover.setOnClickListener {
                        rejectHandover(handoverDoc.id)
                    }
                } else {
                    binding.cardHandoverNotification.visibility = View.GONE
                }
            }
    }

    private fun acceptHandover(requestId: String, patientId: Int) {
        val app = requireActivity().application as NurseFlowApplication
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                app.handoverRepository.acceptHandoverRequest(requestId, patientId)
                Toast.makeText(requireContext(), "Pasien berhasil diterima", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menerima pasien", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun rejectHandover(requestId: String) {
        val app = requireActivity().application as NurseFlowApplication
        viewLifecycleOwner.lifecycleScope.launch {
            app.handoverRepository.rejectHandoverRequest(requestId)
        }
    }

    private fun listenForTotalNotifications() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Listen for connection requests
        requestsListener = firestore.collection("requests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                pendingRequestsCount = snapshot?.size() ?: 0
                updateNotificationBadge()
                
                val requestDoc = snapshot?.documents?.firstOrNull()
                if (requestDoc != null) {
                    binding.cardRequest.visibility = View.VISIBLE
                    binding.tvRequestDesc.text = "${requestDoc.getString("fromName")} ingin terhubung"
                    binding.btnAcceptRequest.setOnClickListener { acceptConnectionRequest(requestDoc.id, requestDoc.getString("fromUid")!!) }
                    binding.btnRejectRequest.setOnClickListener { rejectConnectionRequest(requestDoc.id) }
                } else {
                    binding.cardRequest.visibility = View.GONE
                }
            }

        // 2. Listen for clinical reminders
        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                unreadNotificationsCount = snapshot?.size() ?: 0
                updateNotificationBadge()
                
                if (unreadNotificationsCount > 0) {
                    startBellAnimation()
                }
            }
    }

    private fun updateNotificationBadge() {
        if (_binding == null) return
        val total = pendingRequestsCount + unreadNotificationsCount
        if (total > 0) {
            binding.tvNotificationBadge.visibility = View.VISIBLE
            binding.tvNotificationBadge.text = if (total > 99) "99+" else total.toString()
        } else {
            binding.tvNotificationBadge.visibility = View.GONE
            stopBellAnimation()
        }
    }

    private fun startBellAnimation() {
        if (_binding == null) return
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.bell_shake)
        binding.ivBell.startAnimation(shake)
    }

    private fun stopBellAnimation() {
        _binding?.ivBell?.clearAnimation()
    }

    private fun setLoading(isLoading: Boolean) {
        _binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun setupColleagueRecyclerView() {
        colleagueAdapter = ColleagueAdapter(
            onClick = { colleague ->
                val bundle = Bundle().apply {
                    putString("targetUid", colleague.uid)
                    putString("targetName", colleague.name)
                }
                findNavController().navigate(R.id.action_homeFragment_to_patientListFragment, bundle)
            },
            onLongClick = { colleague ->
                val index = colleagueAdapter.currentList.indexOf(colleague)
                val viewHolder = binding.rvColleagues.findViewHolderForAdapterPosition(index)
                showColleaguePopupMenu(viewHolder?.itemView ?: binding.rvColleagues, colleague)
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
                    findNavController().navigate(R.id.action_homeFragment_to_patientListFragment, bundle)
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

    private fun showDeleteConfirmation(colleague: Colleague) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Rekan Kerja")
            .setMessage("Apakah Anda yakin ingin menghapus ${colleague.name}?")
            .setPositiveButton("Hapus") { _, _ -> deleteColleague(colleague) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteColleague(colleague: Colleague) {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)
        firestore.collection("connections")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val members = doc.get("members") as? List<String>
                    if (members?.contains(colleague.uid) == true) doc.reference.delete()
                }
                setLoading(false)
            }
            .addOnFailureListener { setLoading(false) }
    }

    private fun acceptConnectionRequest(requestId: String, fromUid: String) {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)
        val connectionData = hashMapOf("members" to listOf(uid, fromUid), "createdAt" to FieldValue.serverTimestamp())
        firestore.runBatch { batch ->
            batch.set(firestore.collection("connections").document(), connectionData)
            batch.update(firestore.collection("requests").document(requestId), "status", "accepted")
        }.addOnSuccessListener { setLoading(false) }.addOnFailureListener { setLoading(false) }
    }

    private fun rejectConnectionRequest(requestId: String) {
        firestore.collection("requests").document(requestId).update("status", "rejected")
    }

    private fun loadConnections() {
        val uid = auth.currentUser?.uid ?: return
        connectionsListener = firestore.collection("connections")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val partnerIds = mutableSetOf<String>()
                snapshot?.forEach { doc ->
                    val members = doc.get("members") as? List<String>
                    members?.firstOrNull { it != uid }?.let { partnerIds.add(it) }
                }
                if (partnerIds.isNotEmpty()) loadColleaguesDetails(partnerIds.toList()) else colleagueAdapter.submitList(emptyList())
            }
    }

    private fun loadColleaguesDetails(userIds: List<String>) {
        val colleagues = mutableListOf<Colleague>()
        var loadedCount = 0
        for (id in userIds) {
            firestore.collection("users").document(id).get()
                .addOnSuccessListener { doc ->
                    if (_binding == null) return@addOnSuccessListener
                    colleagues.add(Colleague(id, doc.getString("name") ?: "Nurse", doc.getString("userCode") ?: "", doc.getString("photoUrl")))
                    loadedCount++
                    if (loadedCount == userIds.size) colleagueAdapter.submitList(colleagues.sortedBy { it.name })
                }
                .addOnFailureListener {
                    loadedCount++
                    if (_binding != null && loadedCount == userIds.size) colleagueAdapter.submitList(colleagues.sortedBy { it.name })
                }
        }
    }

    private fun displayUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get().addOnSuccessListener { document ->
            if (_binding == null) return@addOnSuccessListener
            val userCode = document.getString("userCode") ?: generateUserCode().also { saveUserCodeToFirestore(uid, it) }
            binding.cardTeamInfo.visibility = View.VISIBLE
            binding.tvDisplayTeamId.text = userCode
            binding.btnCopyTeamId.setOnClickListener { copyToClipboard(userCode) }
        }
    }

    private fun generateUserCode(): String = "NF-${(1000..9999).random()}-${('A'..'Z').shuffled().take(2).joinToString("")}"

    private fun saveUserCodeToFirestore(uid: String, code: String) {
        firestore.collection("users").document(uid).set(hashMapOf("userCode" to code), SetOptions.merge())
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Work ID", text))
        Toast.makeText(requireContext(), "Work ID copied", Toast.LENGTH_SHORT).show()
    }

    private fun setupUserHeader() {
        auth.currentUser?.let {
            if (_binding != null) {
                binding.tvGreeting.text = "Hello, ${it.displayName?.split(" ")?.firstOrNull() ?: "Nurse"}"
                Glide.with(this).load(it.photoUrl).placeholder(android.R.drawable.ic_menu_gallery).circleCrop().into(binding.ivProfile)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnNotifications.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment) }
        binding.btnHeaderColleagues.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_colleaguesFragment) }
        binding.btnIvCalc.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_ivCalculatorFragment) }
        binding.btnVitals.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_patientListFragment) }
        binding.btnPatients.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_patientListFragment) }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnAiNotes.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_aiNotesFragment) }
        binding.btnColleagues.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_colleaguesFragment) }
        binding.tvColleaguesLabel.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_colleaguesFragment) }
        binding.btnGoToHandover.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_handoverFragment) }
    }

    private fun logout() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(requireActivity(), gso).signOut().addOnCompleteListener {
            if (isAdded) {
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            }
        }
    }

    override fun onDestroyView() {
        // Stop all listeners to prevent crashes
        requestsListener?.remove()
        notificationsListener?.remove()
        connectionsListener?.remove()
        handoverListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}

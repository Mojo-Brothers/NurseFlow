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
import com.ivoryapp.nurseflow.data.repository.LanguageRepository
import com.ivoryapp.nurseflow.databinding.FragmentHomeBinding
import com.ivoryapp.nurseflow.ui.LoginActivity
import com.ivoryapp.nurseflow.utils.LocaleManager
import com.ivoryapp.nurseflow.utils.RemoteConfigManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var colleagueAdapter: ColleagueAdapter
    private var pendingRequestsCount = 0
    private var unreadNotificationsCount = 0
    
    private var requestsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var connectionsListener: ListenerRegistration? = null

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

        RemoteConfigManager.fetchAndActivate {
            if (isAdded && _binding != null) {
                applyTranslations()
                setupUserHeader()
            }
        }

        setupUserHeader()
        setupColleagueRecyclerView()
        setupClickListeners()
        
        displayUserInfo()
        listenForTotalNotifications()
        loadConnections()
    }

    private fun applyTranslations() {
        if (_binding == null) return
        val context = requireContext()
        
        binding.tvSubtitle.text = LanguageRepository.get(context, "home_subtitle")
        binding.tvStatusMain.text = LanguageRepository.get(context, "label_briefing_and_handover")
        binding.tvLastUpdate.text = LanguageRepository.get(context, "label_start_handover_desc")
        
        binding.tvPatientsLabel.text = LanguageRepository.get(context, "tool_patients")
        binding.tvIvCalcLabel.text = LanguageRepository.get(context, "tool_iv_calc")
        binding.tvColleaguesLabelGrid.text = LanguageRepository.get(context, "tool_colleagues")
        binding.tvVitalsLabel.text = LanguageRepository.get(context, "tool_vitals")
        binding.tvAiNotesLabel.text = LanguageRepository.get(context, "tool_ai_notes")
        
        binding.tvColleaguesLabel.text = LanguageRepository.get(context, "label_my_team")
        
        binding.tvRequestTitle.text = LanguageRepository.get(context, "label_colleague_request")
        binding.btnAcceptRequest.text = LanguageRepository.get(context, "btn_accept")
        binding.btnRejectRequest.text = LanguageRepository.get(context, "btn_reject")
    }

    private fun listenForTotalNotifications() {
        val uid = auth.currentUser?.uid ?: return

        requestsListener = firestore.collection("requests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                pendingRequestsCount = snapshot?.size() ?: 0
                updateNotificationBadge()
                
                // NOTIFIKASI DI HOME DINONAKTIFKAN SESUAI REQUEST USER
                // SEMUA NOTIFIKASI SUDAH ADA DI HALAMAN NOTIFIKASI (BELL ICON)
                binding.cardRequest.visibility = View.GONE
            }

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
        popup.menu.add(0, 1, 0, LanguageRepository.get(requireContext(), "menu_view_profile"))
        popup.menu.add(0, 2, 1, LanguageRepository.get(requireContext(), "menu_view_patients"))
        
        val deleteItem = popup.menu.add(0, 3, 2, LanguageRepository.get(requireContext(), "menu_delete_colleague"))
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
            .setTitle(LanguageRepository.get(requireContext(), "dialog_delete_colleague_title"))
            .setMessage(String.format(LanguageRepository.get(requireContext(), "dialog_delete_colleague_message"), colleague.name))
            .setPositiveButton(LanguageRepository.get(requireContext(), "btn_delete")) { _, _ -> deleteColleague(colleague) }
            .setNegativeButton(LanguageRepository.get(requireContext(), "btn_cancel"), null)
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
        Toast.makeText(requireContext(), LanguageRepository.get(requireContext(), "toast_work_id_copied"), Toast.LENGTH_SHORT).show()
    }

    private fun setupUserHeader() {
        auth.currentUser?.let {
            if (_binding != null) {
                val greetingPrefix = LanguageRepository.get(requireContext(), "greeting_hello_prefix")
                binding.tvGreeting.text = "$greetingPrefix, ${it.displayName?.split(" ")?.firstOrNull() ?: "Nurse"}"
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
        binding.btnLanguage.setOnClickListener { showLanguageSelector() }
    }

    private fun showLanguageSelector() {
        val languages = arrayOf("English", "Bahasa Indonesia")
        val languageCodes = arrayOf("en", "in")
        
        AlertDialog.Builder(requireContext())
            .setTitle(LanguageRepository.get(requireContext(), "dialog_select_language_title"))
            .setItems(languages) { _, which ->
                val selectedLang = languageCodes[which]
                LocaleManager.setNewLocale(requireContext(), selectedLang)
                requireActivity().finish()
                requireActivity().startActivity(requireActivity().intent)
            }
            .show()
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
        requestsListener?.remove()
        notificationsListener?.remove()
        connectionsListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}

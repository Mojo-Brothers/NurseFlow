package com.ivoryapp.nurseflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.ivoryapp.nurseflow.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        loadUserData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        
        binding.etProfileName.setText(user.displayName)
        binding.etProfileEmail.setText(user.email)
        
        Glide.with(this)
            .load(user.photoUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .circleCrop()
            .into(binding.ivProfileEdit)
    }

    private fun setupClickListeners() {
        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }
        
        binding.btnChangePhoto.setOnClickListener {
            Toast.makeText(requireContext(), "Fitur ganti foto akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileChanges() {
        val newName = binding.etProfileName.text.toString().trim()
        val user = auth.currentUser ?: return

        if (newName.isEmpty()) {
            binding.etProfileName.error = "Nama tidak boleh kosong"
            return
        }

        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Menyimpan..."

        // 1. Update Firebase Auth Profile
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 2. Update Firestore User Document
                firestore.collection("users").document(user.uid)
                    .update("name", newName)
                    .addOnSuccessListener {
                        if (_binding != null) {
                            Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                    .addOnFailureListener { e ->
                        if (_binding != null) {
                            binding.btnSaveProfile.isEnabled = true
                            binding.btnSaveProfile.text = "Simpan Perubahan"
                            Toast.makeText(requireContext(), "Gagal update Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                if (_binding != null) {
                    binding.btnSaveProfile.isEnabled = true
                    binding.btnSaveProfile.text = "Simpan Perubahan"
                    Toast.makeText(requireContext(), "Gagal update Auth: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

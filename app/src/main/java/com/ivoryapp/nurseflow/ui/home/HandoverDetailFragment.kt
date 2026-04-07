package com.ivoryapp.nurseflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.ivoryapp.nurseflow.data.model.Handover
import com.ivoryapp.nurseflow.data.model.HandoverTask
import com.ivoryapp.nurseflow.databinding.FragmentHandoverDetailBinding

class HandoverDetailFragment : Fragment() {

    private var _binding: FragmentHandoverDetailBinding? = null
    private val binding get() = _binding!!
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var taskAdapter: HandoverTaskAdapter
    private var tasksListener: ListenerRegistration? = null
    private var handoverListener: ListenerRegistration? = null

    // Assuming you use SafeArgs, otherwise get from bundle
    // private val args: HandoverDetailFragmentArgs by navArgs()
    private var handoverId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHandoverDetailBinding.inflate(inflater, container, false)
        handoverId = arguments?.getString("handoverId") ?: ""
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        listenToHandoverDetails()
        listenToTasks()
        
        binding.btnSendTask.setOnClickListener {
            val title = binding.etNewTask.text.toString()
            if (title.isNotEmpty()) {
                addNewTask(title)
                binding.etNewTask.text.clear()
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = HandoverTaskAdapter { task ->
            toggleTaskStatus(task)
        }
        binding.rvHandoverTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun listenToHandoverDetails() {
        handoverListener = firestore.collection("handovers").document(handoverId)
            .addSnapshotListener { snapshot, _ ->
                val handover = snapshot?.toObject(Handover::class.java) ?: return@addSnapshotListener
                binding.tvPatientName.text = handover.patientName
                binding.tvFromTo.text = "Dari: ${handover.fromName} → Untuk: ${auth.currentUser?.displayName}"
            }
    }

    private fun listenToTasks() {
        tasksListener = firestore.collection("handover_tasks")
            .whereEqualTo("handoverId", handoverId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val tasks = snapshot.documents.mapNotNull { 
                    it.toObject(HandoverTask::class.java)?.copy(id = it.id)
                }
                taskAdapter.submitList(tasks)
            }
    }

    private fun addNewTask(title: String) {
        val task = HandoverTask(
            handoverId = handoverId,
            title = title,
            createdBy = auth.currentUser?.uid ?: "",
            isCompleted = false
        )
        firestore.collection("handover_tasks").add(task)
    }

    private fun toggleTaskStatus(task: HandoverTask) {
        firestore.collection("handover_tasks").document(task.id)
            .update("isCompleted", !task.isCompleted)
    }

    override fun onDestroyView() {
        tasksListener?.remove()
        handoverListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}

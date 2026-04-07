package com.ivoryapp.nurseflow.ui.patient

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ivoryapp.nurseflow.databinding.DialogAddPatientBinding

class AddPatientDialog(private val onSave: (name: String, age: Int, dob: String, room: String, condition: String) -> Unit) : DialogFragment() {

    private var _binding: DialogAddPatientBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddPatientBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding.root)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val dob = binding.etDob.text.toString()
            val age = binding.etAge.text.toString().toIntOrNull() ?: 0
            val room = binding.etRoom.text.toString()
            val condition = binding.etCondition.text.toString()

            if (name.isNotEmpty() && room.isNotEmpty()) {
                onSave(name, age, dob, room, condition)
                dismiss()
            } else {
                if (name.isEmpty()) binding.etName.error = "Required"
                if (room.isEmpty()) binding.etRoom.error = "Required"
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddPatientDialog"
    }
}

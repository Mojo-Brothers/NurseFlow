package com.ivoryapp.nurseflow.ui.patient

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivoryapp.nurseflow.NurseFlowApplication
import com.ivoryapp.nurseflow.R
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.databinding.FragmentVitalSignsBinding
import com.google.android.material.snackbar.Snackbar

class VitalSignFragment : Fragment() {

    private var _binding: FragmentVitalSignsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VitalSignViewModel by viewModels {
        val app = requireActivity().application as NurseFlowApplication
        VitalSignViewModelFactory(app.vitalSignRepository, app.patientRepository)
    }

    private var patientId: Int = -1
    private lateinit var adapter: VitalSignAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVitalSignsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patientId = arguments?.getInt("patientId") ?: -1

        adapter = VitalSignAdapter()
        binding.rvVitalSigns.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVitalSigns.adapter = adapter

        if (patientId != -1) {
            viewModel.getPatient(patientId).observe(viewLifecycleOwner) { patient ->
                patient?.let {
                    binding.tvPatientNameDetail.text = it.name
                    binding.tvPatientIdDetail.text = "DOB: ${it.dateOfBirth} | Age: ${it.age} | Room: ${it.roomNumber}"
                }
            }

            viewModel.getVitalSigns(patientId).observe(viewLifecycleOwner) { vitals ->
                adapter.submitList(vitals)
            }
        }

        binding.fabAddVital.setOnClickListener {
            showAddVitalSignDialog()
        }

        setupSwipeActions()
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            
            private val paint = Paint()
            private val deleteColor = ContextCompat.getColor(requireContext(), R.color.status_urgent)
            private val editColor = ContextCompat.getColor(requireContext(), R.color.accent_cyan)
            private val deleteIcon: Drawable? = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val editIcon: Drawable? = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val vital = adapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.deleteVitalSign(vital)
                    Snackbar.make(binding.root, "Vital Sign dihapus", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            viewModel.addVitalSign(
                                vital.patientId, vital.systolic, vital.diastolic,
                                vital.pulse, vital.temperature, vital.respiration, vital.spo2
                            )
                        }.show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    adapter.notifyItemChanged(position)
                    showAddVitalSignDialog(vital)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom.toFloat() - itemView.top.toFloat()

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (dX < 0) { // Swipe Left (Delete)
                        paint.color = deleteColor
                        val background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                        c.drawRect(background, paint)

                        deleteIcon?.let {
                            val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                            val iconTop = itemView.top + iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight
                            val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                            val iconRight = itemView.right - iconMargin
                            it.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
                            it.setTint(Color.WHITE)
                            it.draw(c)
                        }

                    } else if (dX > 0) { // Swipe Right (Edit)
                        paint.color = editColor
                        val background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                        c.drawRect(background, paint)

                        editIcon?.let {
                            val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                            val iconTop = itemView.top + iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                            it.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
                            it.setTint(Color.WHITE)
                            it.draw(c)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvVitalSigns)
    }

    private fun showAddVitalSignDialog(vital: VitalSign? = null) {
        val dialog = AddVitalSignDialog(vital) { systolic, diastolic, pulse, temp, resp, spo2 ->
            if (vital == null) {
                viewModel.addVitalSign(patientId, systolic, diastolic, pulse, temp, resp, spo2)
            } else {
                val updatedVital = vital.copy(
                    systolic = systolic,
                    diastolic = diastolic,
                    pulse = pulse,
                    temperature = temp,
                    respiration = resp,
                    spo2 = spo2,
                    timestamp = System.currentTimeMillis()
                )
                viewModel.updateVitalSign(updatedVital)
            }
        }
        dialog.show(childFragmentManager, AddVitalSignDialog.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

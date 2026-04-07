package com.ivoryapp.nurseflow.ui.patient

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.databinding.DialogAddVitalSignBinding
import java.util.*

class AddVitalSignDialog(
    private val existingVital: VitalSign? = null,
    private val onSave: (Int, Int, Int, Double, Int, Int) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddVitalSignBinding? = null
    private val binding get() = _binding!!

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = data?.get(0) ?: ""
            parseInputText(spokenText)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { processImageWithOCR(it) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddVitalSignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingVital?.let {
            binding.etSystolic.setText(it.systolic.toString())
            binding.etDiastolic.setText(it.diastolic.toString())
            binding.etPulse.setText(it.pulse.toString())
            binding.etTemp.setText(it.temperature.toString())
            binding.etResp.setText(it.respiration.toString())
            binding.etSpo2.setText(it.spo2?.toString() ?: "")
        }

        binding.btnVoiceInput.setOnClickListener { startVoiceInput() }
        binding.btnCameraInput.setOnClickListener { startCameraInput() }
        binding.btnSave.setOnClickListener {
            val systolic = binding.etSystolic.text.toString().toIntOrNull() ?: 0
            val diastolic = binding.etDiastolic.text.toString().toIntOrNull() ?: 0
            val pulse = binding.etPulse.text.toString().toIntOrNull() ?: 0
            val temp = binding.etTemp.text.toString().toDoubleOrNull() ?: 0.0
            val resp = binding.etResp.text.toString().toIntOrNull() ?: 0
            val spo2 = binding.etSpo2.text.toString().toIntOrNull() ?: 0

            onSave(systolic, diastolic, pulse, temp, resp, spo2)
            dismiss()
        }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan Vital Sign")
        }
        try { voiceLauncher.launch(intent) } catch (e: Exception) {
            Toast.makeText(requireContext(), "Voice input tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCameraInput() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try { cameraLauncher.launch(takePictureIntent) } catch (e: Exception) {
            Toast.makeText(requireContext(), "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImageWithOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isBlank()) {
                    Toast.makeText(requireContext(), "Gambar tidak terbaca", Toast.LENGTH_LONG).show()
                } else {
                    analyzeTextAndColor(bitmap, visionText)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "OCR Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun analyzeTextAndColor(bitmap: Bitmap, visionText: Text) {
        var foundCount = 0
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val text = element.text
                    val box = element.boundingBox ?: continue
                    
                    // Deteksi warna teks di area kotak (bounding box)
                    val color = getDominantColor(bitmap, box)
                    
                    if (applyColorHeuristic(text, color)) {
                        foundCount++
                    }
                }
            }
        }
        
        // Tetap jalankan parsing teks biasa sebagai fallback
        parseInputText(visionText.text)
        
        if (foundCount > 0) {
            Toast.makeText(requireContext(), "Mendeteksi $foundCount data berdasarkan warna", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDominantColor(bitmap: Bitmap, rect: Rect): Int {
        // Ambil pixel di tengah kotak untuk performa cepat
        if (rect.left < 0 || rect.top < 0 || rect.right > bitmap.width || rect.bottom > bitmap.height) return Color.WHITE
        
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        return bitmap.getPixel(centerX, centerY)
    }

    private fun applyColorHeuristic(text: String, color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        
        val value = text.replace(Regex("\\D"), "").toIntOrNull() ?: return false
        
        return when {
            // Hijau -> Pulse / HR
            g > r + 20 && g > b + 20 && value in 40..200 -> {
                binding.etPulse.setText(value.toString())
                true
            }
            // Biru / Cyan -> SpO2
            b > r + 20 && value in 70..100 -> {
                binding.etSpo2.setText(value.toString())
                true
            }
            // Merah / Kuning -> BP (Systolic biasanya angka besar di atas 80)
            r > b + 20 && value in 80..240 && binding.etSystolic.text.isNullOrBlank() -> {
                binding.etSystolic.setText(value.toString())
                true
            }
            else -> false
        }
    }

    private fun parseInputText(text: String) {
        val lowerText = text.lowercase(Locale.ROOT).replace("\n", " ").replace(":", " ")

        // Pattern 120/80
        val bpPattern = Regex("(\\d{2,3})\\s*/\\s*(\\d{2,3})")
        bpPattern.find(lowerText)?.let {
            binding.etSystolic.setText(it.groupValues[1])
            binding.etDiastolic.setText(it.groupValues[2])
        }

        // Keywords
        val patterns = listOf(
            Triple(binding.etPulse, Regex("(pulse|nadi|hr|pr|bpm)\\D*(\\d{2,3})"), "Pulse"),
            Triple(binding.etTemp, Regex("(temp|suhu|t)\\D*(\\d{2}[.]\\d?)"), "Temp"),
            Triple(binding.etResp, Regex("(resp|nafas|rr|br)\\D*(\\d{1,2})"), "Resp"),
            Triple(binding.etSpo2, Regex("(spo2|sat|o2|%)\\D*(\\d{2,3})"), "SpO2")
        )

        for ((editText, regex, _) in patterns) {
            if (editText.text.isNullOrBlank()) {
                regex.find(lowerText)?.groupValues?.get(2)?.let { editText.setText(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddVitalSignDialog"
    }
}

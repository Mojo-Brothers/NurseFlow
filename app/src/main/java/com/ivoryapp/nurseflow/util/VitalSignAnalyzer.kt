package com.ivoryapp.nurseflow.util

import com.ivoryapp.nurseflow.data.model.VitalSign

object VitalSignAnalyzer {

    enum class RiskLevel(val label: String, val color: String) {
        LOW("Low Risk", "#4CAF50"),
        MEDIUM("Medium Risk", "#FF9800"),
        HIGH("High Risk", "#F44336")
    }

    /**
     * Calculates Early Warning Score (EWS) based on NEWS2 protocol.
     * This is a simplified version.
     */
    fun calculateNEWS2(vitalSign: VitalSign): Int {
        var score = 0
        
        // Respiratory Rate
        score += when (vitalSign.respiration) {
            in 0..8 -> 3
            in 9..11 -> 1
            in 12..20 -> 0
            in 21..24 -> 2
            else -> 3
        }

        // SpO2 (Scale 1)
        vitalSign.spo2?.let {
            score += when (it) {
                in 0..91 -> 3
                92, 93 -> 2
                94, 95 -> 1
                else -> 0
            }
        }

        // Temperature
        score += when {
            vitalSign.temperature <= 35.0 -> 3
            vitalSign.temperature <= 36.0 -> 1
            vitalSign.temperature <= 38.0 -> 0
            vitalSign.temperature <= 39.0 -> 1
            else -> 2
        }

        // Systolic BP
        score += when (vitalSign.systolic) {
            in 0..90 -> 3
            in 91..100 -> 2
            in 101..110 -> 1
            in 111..219 -> 0
            else -> 3
        }

        // Pulse (Heart Rate)
        score += when (vitalSign.pulse) {
            in 0..40 -> 3
            in 41..50 -> 1
            in 51..90 -> 0
            in 91..110 -> 1
            in 111..130 -> 2
            else -> 3
        }

        // Consciousness (AVPU)
        score += if (vitalSign.consciousness != "Alert") 3 else 0

        return score
    }

    fun getRiskLevel(score: Int): RiskLevel {
        return when {
            score == 0 -> RiskLevel.LOW
            score <= 4 -> RiskLevel.LOW
            score <= 6 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }
    }

    fun getClinicalSuggestion(vitalSign: VitalSign, score: Int): String {
        return when {
            score >= 7 -> "CRITICAL: Urgent assessment by a response team (RRT) is required. Consider intensive care."
            score >= 5 -> "WARNING: Urgent review by a clinician or senior nurse required. Increase frequency of monitoring."
            vitalSign.spo2 != null && vitalSign.spo2 < 92 -> "Patient shows signs of hypoxia. Consider oxygen therapy and monitoring."
            vitalSign.systolic > 180 -> "High blood pressure detected. Monitor for symptoms of hypertensive emergency."
            else -> "Patient condition is stable. Continue routine monitoring."
        }
    }
}

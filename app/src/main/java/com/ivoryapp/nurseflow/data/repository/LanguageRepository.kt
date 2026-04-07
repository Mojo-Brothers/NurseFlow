package com.ivoryapp.nurseflow.data.repository

import android.content.Context
import com.ivoryapp.nurseflow.utils.RemoteConfigManager
import java.util.Locale

object LanguageRepository {

    fun get(context: Context, key: String): String {
        val prefs = context.getSharedPreferences("nurseflow_prefs", Context.MODE_PRIVATE)
        // Handle both 'id' and 'in' for Indonesian locale
        val lang = prefs.getString("selected_language", Locale.getDefault().language) ?: "en"
        val normalizedLang = if (lang == "id" || lang == "in") "in" else "en"
        
        val remoteValue = RemoteConfigManager.getString(key, normalizedLang)
        
        return if (remoteValue.isNotEmpty()) {
            remoteValue
        } else {
            // Fallback to local strings
            val resId = context.resources.getIdentifier(key, "string", context.packageName)
            if (resId != 0) context.getString(resId) else key
        }
    }
}

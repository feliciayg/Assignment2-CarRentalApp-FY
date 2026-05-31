package com.example.assignment2_carrentalapp_fy.util

import android.app.Activity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.assignment2_carrentalapp_fy.R
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    fun applyDarkMode(enabled: Boolean) {
        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applySystemBars(activity: Activity, isDarkMode: Boolean) {
        activity.window.statusBarColor = ContextCompat.getColor(
            activity,
            if (isDarkMode) R.color.sr_surface else R.color.sr_header_bg
        )
        activity.window.navigationBarColor = ContextCompat.getColor(
            activity,
            if (isDarkMode) R.color.sr_background else R.color.sr_background
        )
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }
}

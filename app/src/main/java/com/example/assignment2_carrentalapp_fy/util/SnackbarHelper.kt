package com.example.assignment2_carrentalapp_fy.util

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.example.assignment2_carrentalapp_fy.R
import com.google.android.material.snackbar.Snackbar

object SnackbarHelper {
    fun showShort(anchor: View, message: String) {
        createSnackbar(anchor, message, Snackbar.LENGTH_SHORT).show()
    }

    fun showLong(anchor: View, message: String) {
        createSnackbar(anchor, message, Snackbar.LENGTH_LONG).show()
    }

    fun showWithAction(
        anchor: View,
        message: String,
        actionText: String,
        onAction: () -> Unit
    ) {
        createSnackbar(anchor, message, Snackbar.LENGTH_LONG)
            .setAction(actionText) { onAction() }
            .show()
    }

    private fun createSnackbar(anchor: View, message: String, duration: Int): Snackbar {
        val snackbar = Snackbar.make(anchor, message, duration)
        val context = anchor.context
        val backgroundColor = ContextCompat.getColor(context, R.color.sr_surface_muted)
        val textColor = ContextCompat.getColor(context, R.color.sr_text_primary)
        val actionColor = ContextCompat.getColor(context, R.color.sr_primary)

        ViewCompat.setBackgroundTintList(snackbar.view, ColorStateList.valueOf(backgroundColor))
        snackbar.setTextColor(textColor)
        snackbar.setActionTextColor(actionColor)
        snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines = 3
        return snackbar
    }
}

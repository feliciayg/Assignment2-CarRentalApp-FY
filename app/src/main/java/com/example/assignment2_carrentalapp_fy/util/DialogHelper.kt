package com.example.assignment2_carrentalapp_fy.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.assignment2_carrentalapp_fy.R
import com.example.assignment2_carrentalapp_fy.model.Booking
import com.example.assignment2_carrentalapp_fy.model.FilterOption
import com.example.assignment2_carrentalapp_fy.model.SortOption

object DialogHelper {

    fun showCurrentBookingDialog(
        context: Context,
        booking: Booking,
        onEndRental: () -> Unit,
        onClose: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.current_booking_title))
            .setMessage(
                context.getString(
                    R.string.current_booking_message,
                    booking.carName,
                    booking.carModel,
                    booking.rentalDays,
                    booking.totalCost
                )
            )
            .setPositiveButton(context.getString(R.string.end_rental)) { dialog, _ ->
                dialog.dismiss()
                onEndRental()
            }
            .setNegativeButton(context.getString(R.string.close)) { dialog, _ ->
                dialog.dismiss()
                onClose()
            }
            .show()
    }

    fun showRatingDialog(
        context: Context,
        currentRating: Int,
        onSubmit: (Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<androidx.appcompat.widget.AppCompatRatingBar>(R.id.ratingBarRental)
        val ratingHint = dialogView.findViewById<TextView>(R.id.textRatingHint)
        ratingBar.rating = 0f

        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.rate_your_rental))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.submit), null)
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedRating = ratingBar.rating.toInt()
            if (selectedRating == 0) {
                ratingHint.text = context.getString(R.string.rating_select_error)
                return@setOnClickListener
            }
            dialog.dismiss()
            onSubmit(selectedRating.coerceIn(1, 5))
        }
    }

    fun showMessageDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.ok), null)
            .show()
    }

    fun showBookingBlockedDialog(
        context: Context,
        onEndRental: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.booking_blocked_title))
            .setMessage(context.getString(R.string.booking_blocked_message))
            .setPositiveButton(context.getString(R.string.end_rental)) { dialog, _ ->
                dialog.dismiss()
                onEndRental()
            }
            .setNegativeButton(context.getString(R.string.ok), null)
            .show()
    }

    fun showSortDialog(
        context: Context,
        selectedOption: SortOption,
        onSelected: (SortOption) -> Unit
    ) {
        val options = arrayOf(
            context.getString(R.string.sort_option_rating),
            context.getString(R.string.sort_option_year),
            context.getString(R.string.sort_option_cost)
        )
        var selectedIndex = when (selectedOption) {
            SortOption.RATING_DESC -> 0
            SortOption.YEAR_DESC -> 1
            SortOption.COST_ASC -> 2
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.sort_cars_title))
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(context.getString(R.string.apply)) { dialog, _ ->
                dialog.dismiss()
                onSelected(
                    when (selectedIndex) {
                        1 -> SortOption.YEAR_DESC
                        2 -> SortOption.COST_ASC
                        else -> SortOption.RATING_DESC
                    }
                )
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    fun showFilterDialog(
        context: Context,
        selectedOption: FilterOption,
        onSelected: (FilterOption) -> Unit
    ) {
        val options = arrayOf(
            context.getString(R.string.filter_option_all),
            context.getString(R.string.filter_option_available),
            context.getString(R.string.filter_option_rented),
            context.getString(R.string.filter_option_favourites)
        )
        var selectedIndex = when (selectedOption) {
            FilterOption.ALL -> 0
            FilterOption.AVAILABLE -> 1
            FilterOption.RENTED -> 2
            FilterOption.FAVOURITES -> 3
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.filter_cars_title))
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(context.getString(R.string.apply)) { dialog, _ ->
                dialog.dismiss()
                onSelected(
                    when (selectedIndex) {
                        1 -> FilterOption.AVAILABLE
                        2 -> FilterOption.RENTED
                        3 -> FilterOption.FAVOURITES
                        else -> FilterOption.ALL
                    }
                )
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    fun showBookingSuccess(context: Context, message: String, onOk: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.booking_successful_title))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                onOk()
            }
            .show()
    }

    fun showError(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.ok), null)
            .show()
    }
}

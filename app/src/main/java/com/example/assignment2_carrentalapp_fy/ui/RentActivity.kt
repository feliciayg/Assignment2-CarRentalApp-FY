package com.example.assignment2_carrentalapp_fy.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.assignment2_carrentalapp_fy.R
import com.example.assignment2_carrentalapp_fy.databinding.ActivityRentBinding
import com.example.assignment2_carrentalapp_fy.model.Car
import com.example.assignment2_carrentalapp_fy.model.formattedKilometres
import com.example.assignment2_carrentalapp_fy.model.formattedRating
import com.example.assignment2_carrentalapp_fy.model.nameWithModel
import com.example.assignment2_carrentalapp_fy.repository.CarRepository
import com.example.assignment2_carrentalapp_fy.state.BookingValidationState
import com.example.assignment2_carrentalapp_fy.util.DialogHelper
import com.example.assignment2_carrentalapp_fy.util.SnackbarHelper
import com.example.assignment2_carrentalapp_fy.util.ThemeHelper
import com.example.assignment2_carrentalapp_fy.viewmodel.RentViewModel

// Booking details screen. It receives a Parcelable Car, renders RentUiState,
// and returns a result to MainActivity after save or cancellation.
class RentActivity : AppCompatActivity() {
    private var selectedCar: Car? = null
    private lateinit var binding: ActivityRentBinding
    private val rentViewModel = RentViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeHelper.applySystemBars(
            this,
            androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        )

        setSupportActionBar(binding.toolbarRent)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // Toolbar back and system back both mean "cancel this unsaved booking".
        binding.toolbarRent.setNavigationOnClickListener { finishWithCancel() }
        onBackPressedDispatcher.addCallback(this) { finishWithCancel() }

        val selectedCar = intent.getParcelableExtra(EXTRA_SELECTED_CAR, Car::class.java)
        this.selectedCar = selectedCar
        // Aim: confirm RentActivity received the selected car from MainActivity correctly.
        Log.d(TAG, "onCreate: selectedCar=${selectedCar?.nameWithModel()}")
        if (selectedCar == null) {
            finish()
            return
        }

        rentViewModel.setCar(selectedCar)
        val initialRentalDays = intent.getIntExtra(EXTRA_SELECTED_DAYS, 1)
        // Keep this extra so the activity can still support preselected days if needed.
        if (initialRentalDays in 1..7 && initialRentalDays != 1) {
            rentViewModel.setRentalDays(initialRentalDays)
        }
        bindActions()
        render()
    }

    private fun bindActions() {
        binding.sliderRentalDays.addOnChangeListener { _, value, _ ->
            rentViewModel.setRentalDays(value.toInt())
            // Aim: confirm slider changes update rental days and recalculate total cost.
            Log.d(TAG, "slider changed: rentalDays=${value.toInt()}, totalCost=${rentViewModel.rentUiState.totalCost}")
            render()
        }

        binding.buttonSelectedFavourite.setOnClickListener {
            val selectedCar = rentViewModel.rentUiState.selectedCar ?: return@setOnClickListener
            val wasFavourite = selectedCar.isFavourite
            // Aim: confirm favourite toggle on booking screen changes the selected car state.
            Log.d(TAG, "booking favourite click: carId=${selectedCar.id}, wasFavourite=$wasFavourite")
            rentViewModel.toggleFavourite()
            render()
            // Favourite changes are lightweight and reversible, so snackbar Undo is safe here.
            if (!wasFavourite) {
                SnackbarHelper.showWithAction(
                    binding.rentRoot,
                    getString(R.string.car_added_to_favourites_message),
                    getString(R.string.undo)
                ) {
                    rentViewModel.toggleFavourite()
                    render()
                }
            } else {
                SnackbarHelper.showWithAction(
                    binding.rentRoot,
                    getString(R.string.car_removed_from_favourites_message),
                    getString(R.string.undo)
                ) {
                    rentViewModel.toggleFavourite()
                    render()
                }
            }
        }

        binding.buttonSelectedShare.setOnClickListener {
            val selectedCar = rentViewModel.rentUiState.selectedCar ?: return@setOnClickListener
            // Share uses an implicit intent because the destination app should be
            // chosen by Android, unlike the explicit MainActivity -> RentActivity flow.
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("feliciayongys@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_car))
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.share_car_message, selectedCar.nameWithModel()))
                    },
                    getString(R.string.share_car)
                )
            )
        }

        binding.buttonCancelBooking.setOnClickListener { finishWithCancel() }

        binding.buttonSaveBooking.setOnClickListener {
            // Aim: confirm booking save state before validation feedback or dialogs are shown.
            Log.d(
                TAG,
                "saveBooking click: totalCost=${rentViewModel.rentUiState.totalCost}, balance=${rentViewModel.rentUiState.balance}, remaining=${rentViewModel.rentUiState.remainingBalance}, validationError=${rentViewModel.rentUiState.validationError}"
            )
            val result = rentViewModel.saveBooking()
            result.onSuccess { booking ->
                val remainingCreditAfterSave = rentViewModel.rentUiState.remainingBalance
                setResult(
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(EXTRA_BOOKING_ID, booking.bookingId)
                        .putExtra(EXTRA_BOOKING_CAR_ID, booking.carId)
                )
                DialogHelper.showBookingSuccess(
                    this,
                    getString(
                        R.string.booking_success_message,
                        booking.carName,
                        booking.carModel,
                        booking.rentalDays,
                        booking.totalCost,
                        remainingCreditAfterSave
                    )
                ) {
                    finish()
                }
            }.onFailure {
                val validationState = rentViewModel.rentUiState.bookingValidationState
                if (validationState != BookingValidationState.VALID) {
                    showBookingValidationDialog(validationState)
                } else {
                    DialogHelper.showError(
                        this,
                        getString(R.string.booking_details),
                        it.message ?: getString(R.string.generic_error_message)
                    )
                }
                render()
            }
        }
    }

    private fun render() {
        val state = rentViewModel.rentUiState
        val car = state.selectedCar ?: return

        binding.textRentBalanceChip.text = getString(R.string.balance_chip_format, state.balance)
        binding.imageSelectedCar.setImageResource(car.imageResId)
        binding.textSelectedCarName.text = car.nameWithModel()
        binding.textSelectedRatingValue.text = car.formattedRating()
        binding.ratingSelectedCar.rating = car.rating
        binding.textSelectedCarModelText.text = getString(R.string.model_line_format, car.model)
        binding.textSelectedCarYearText.text = car.year.toString()
        binding.textSelectedCarMileageText.text = getString(
            R.string.mileage_value_format,
            car.formattedKilometres()
        )
        binding.textSelectedDailyCost.text = getString(R.string.daily_cost_format, car.dailyCost)
        binding.textSelectedDays.text = getString(R.string.selected_days_format, state.rentalDays)
        bindFavouriteIcon(binding.buttonSelectedFavourite, car.isFavourite)
        if (binding.sliderRentalDays.value != state.rentalDays.toFloat()) {
            binding.sliderRentalDays.value = state.rentalDays.toFloat()
        }
        binding.textSummaryDailyCost.text = getString(R.string.daily_cost_format, car.dailyCost)
        binding.textSummaryRentalDays.text = getString(R.string.days_format, state.rentalDays)
        binding.textSummaryTotalCost.text = getString(R.string.credit_total_format, state.totalCost)
        binding.textRemainingBalance.text = getString(R.string.credit_total_format, state.remainingBalance)
        val validationState = state.bookingValidationState

        // Colours are derived from the current validation state every render so old
        // error styling is cleared as soon as the slider returns to a valid value.
        val totalCostColor = ContextCompat.getColor(
            this,
            if (
                validationState == BookingValidationState.OVER_LIMIT ||
                validationState == BookingValidationState.OVER_LIMIT_AND_INSUFFICIENT_CREDITS
            ) {
                R.color.sr_error_red
            } else {
                R.color.sr_primary
            }
        )
        val remainingBalanceColor = ContextCompat.getColor(
            this,
            if (
                validationState == BookingValidationState.INSUFFICIENT_CREDITS ||
                validationState == BookingValidationState.OVER_LIMIT_AND_INSUFFICIENT_CREDITS
            ) {
                R.color.sr_error_red
            } else {
                R.color.sr_text_primary
            }
        )
        binding.textSummaryTotalCost.setTextColor(totalCostColor)
        binding.textRemainingBalance.setTextColor(remainingBalanceColor)

        val warningTextRes = warningMessageRes(validationState)
        if (warningTextRes != null) {
            // Inline warning gives preventive feedback before the final Save validation dialog.
            binding.cardBookingWarning.visibility = View.VISIBLE
            binding.textBookingWarning.text = getString(warningTextRes)
        } else {
            binding.cardBookingWarning.visibility = View.GONE
        }
        binding.buttonSaveBooking.isEnabled = true
    }

    private fun warningMessageRes(validationState: BookingValidationState): Int? {
        return when (validationState) {
            BookingValidationState.OVER_LIMIT -> R.string.warning_booking_exceeds_limit
            BookingValidationState.INSUFFICIENT_CREDITS -> R.string.warning_insufficient_credits
            BookingValidationState.OVER_LIMIT_AND_INSUFFICIENT_CREDITS ->
                R.string.warning_booking_exceeds_limit_and_insufficient_credits
            BookingValidationState.VALID -> null
        }
    }

    private fun showBookingValidationDialog(validationState: BookingValidationState) {
        // The save dialog uses the same validation state as the warning card so
        // preventive feedback and final validation never disagree.
        val (titleRes, messageRes) = when (validationState) {
            BookingValidationState.OVER_LIMIT -> R.string.booking_details to R.string.rental_total_limit_message
            BookingValidationState.INSUFFICIENT_CREDITS ->
                R.string.insufficient_credit_title to R.string.insufficient_credit_reduce_duration_message
            BookingValidationState.OVER_LIMIT_AND_INSUFFICIENT_CREDITS ->
                R.string.invalid_booking_title to R.string.combined_booking_validation_message
            BookingValidationState.VALID -> return
        }

        DialogHelper.showMessageDialog(
            this,
            getString(titleRes),
            getString(messageRes, CarRepository.getMaxRentalTotal())
        )
    }

    private fun finishWithCancel() {
        // Cancel and system back intentionally discard unsaved rental-day selections.
        setResult(Activity.RESULT_CANCELED)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        finishWithCancel()
        return true
    }

    private fun bindFavouriteIcon(button: ImageButton, isFavourite: Boolean) {
        val iconRes = if (isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        val tintRes = if (isFavourite) R.color.sr_favourite_red else R.color.sr_icon_grey
        button.setImageResource(iconRes)
        button.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, tintRes)
        )
    }

    companion object {
        const val EXTRA_SELECTED_CAR = "extra_selected_car"
        const val EXTRA_SELECTED_DAYS = "extra_selected_days"
        const val EXTRA_BOOKING_ID = "extra_booking_id"
        const val EXTRA_BOOKING_CAR_ID = "extra_booking_car_id"
        private const val TAG = "RentActivityDebug"

        fun createIntent(context: Context, car: Car, selectedDays: Int = 1): Intent {
            // Centralised factory prevents callers from using mismatched extra keys.
            return Intent(context, RentActivity::class.java)
                .putExtra(EXTRA_SELECTED_CAR, car)
                .putExtra(EXTRA_SELECTED_DAYS, selectedDays)
        }
    }
}

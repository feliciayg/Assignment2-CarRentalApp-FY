package com.example.assignment2_carrentalapp_fy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.assignment2_carrentalapp_fy.model.Booking
import com.example.assignment2_carrentalapp_fy.model.Car
import com.example.assignment2_carrentalapp_fy.repository.CarRepository
import com.example.assignment2_carrentalapp_fy.state.BookingValidationState
import com.example.assignment2_carrentalapp_fy.state.RentUiState

// Holds booking screen state and validation logic so RentActivity only renders results.
class RentViewModel(
    private val repository: CarRepository = CarRepository
) : ViewModel() {
    companion object {
        private const val TAG = "RentViewModelDebug"
    }

    var rentUiState: RentUiState =
        RentUiState(
            balance = repository.getCreditBalance(),
            remainingBalance = repository.getCreditBalance()
    )
        private set

    fun setCar(car: Car) {
        val refreshedCar = repository.getCarById(car.id) ?: car
        // Aim: confirm selected car state is set correctly when opening the booking page.
        Log.d(TAG, "setCar: carId=${refreshedCar.id}, dailyCost=${refreshedCar.dailyCost}, balance=${repository.getCreditBalance()}")
        rentUiState = buildRentUiState(refreshedCar, 1)
    }

    fun setRentalDays(days: Int) {
        val currentCar = rentUiState.selectedCar ?: return
        val totalCost = currentCar.dailyCost * days
        val balance = repository.getCreditBalance()
        val nextState = buildRentUiState(currentCar, days)
        // Aim: confirm rental day changes recalculate cost and remaining balance correctly.
        Log.d(TAG, "setRentalDays: days=$days, totalCost=$totalCost, remaining=${balance - totalCost}, error=${nextState.validationError}")
        rentUiState = nextState
    }

    fun toggleFavourite(): Result<Car> {
        val currentCar = rentUiState.selectedCar ?: return Result.failure(
            IllegalStateException("No car selected.")
        )
        // Keep favourite state in sync with the repository while user is on booking screen.
        val result = repository.toggleFavourite(currentCar.id)
        result.onSuccess { updatedCar ->
            rentUiState = rentUiState.copy(selectedCar = updatedCar)
        }
        return result
    }

    fun validateBooking(): String? {
        val state = rentUiState
        val car = state.selectedCar ?: return "No car selected."
        // Rebuild before save so the latest credit balance and car availability are used.
        val nextState = buildRentUiState(car, state.rentalDays)
        rentUiState = nextState
        return nextState.validationError
    }

    fun saveBooking(): Result<Booking> {
        val state = rentUiState
        val car = state.selectedCar ?: return Result.failure(IllegalStateException("No car selected."))
        if (state.bookingValidationState != BookingValidationState.VALID) {
            return Result.failure(IllegalStateException(state.validationError ?: "Invalid booking."))
        }

        val error = validateBooking(car, state.rentalDays)
        // Aim: confirm save booking validation before repository booking is attempted.
        Log.d(TAG, "saveBooking: carId=${car.id}, rentalDays=${state.rentalDays}, validationError=$error")
        if (error != null) {
            rentUiState = state.copy(validationError = error, canSave = false)
            return Result.failure(IllegalStateException(error))
        }

        val result = repository.rentCar(car.id, state.rentalDays)
        result.exceptionOrNull()?.let {
            rentUiState = state.copy(validationError = it.message, canSave = false)
        }
        return result
    }

    private fun validateBooking(car: Car, days: Int): String? {
        return repository.validateBooking(carId = car.id, rentalDays = days)
    }

    private fun buildRentUiState(car: Car, days: Int): RentUiState {
        val totalCost = car.dailyCost * days
        val balance = repository.getCreditBalance()
        val remainingBalance = balance - totalCost
        // Keep all booking feedback derived from one validation state to avoid
        // mismatches between inline warnings, text colours, and save dialogs.
        val bookingValidationState = resolveBookingValidation(totalCost, remainingBalance)
        val error = validateBooking(car, days)

        return RentUiState(
            selectedCar = car,
            rentalDays = days,
            totalCost = totalCost,
            balance = balance,
            remainingBalance = remainingBalance,
            bookingValidationState = bookingValidationState,
            validationError = error,
            canSave = error == null
        )
    }

    private fun resolveBookingValidation(totalCost: Int, remainingBalance: Int): BookingValidationState {
        val isOverLimit = totalCost > repository.getMaxRentalTotal()
        val hasInsufficientCredits = remainingBalance < 0

        // The matrix lets the UI highlight only the value that is actually invalid.
        return when {
            isOverLimit && hasInsufficientCredits -> BookingValidationState.OVER_LIMIT_AND_INSUFFICIENT_CREDITS
            isOverLimit -> BookingValidationState.OVER_LIMIT
            hasInsufficientCredits -> BookingValidationState.INSUFFICIENT_CREDITS
            else -> BookingValidationState.VALID
        }
    }
}

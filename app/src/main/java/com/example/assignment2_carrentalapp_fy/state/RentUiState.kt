package com.example.assignment2_carrentalapp_fy.state

import com.example.assignment2_carrentalapp_fy.model.Car

// A single snapshot of the booking screen. RentActivity renders from this state
// so warning text, colours, and dialogs stay consistent after every slider change.
data class RentUiState(
    val selectedCar: Car? = null,
    val rentalDays: Int = 1,
    val totalCost: Int = 0,
    val balance: Int = 500,
    val remainingBalance: Int = 500,
    val bookingValidationState: BookingValidationState = BookingValidationState.VALID,
    val validationError: String? = null,
    val canSave: Boolean = false
)

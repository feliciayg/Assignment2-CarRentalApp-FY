package com.example.assignment2_carrentalapp_fy.state

enum class BookingValidationState {
    VALID,
    // Total cost breaks the assignment's 400-credit limit, but the user still has enough balance.
    OVER_LIMIT,
    // Total cost is within the assignment limit, but the user's available balance is not enough.
    INSUFFICIENT_CREDITS,
    // Both validation rules fail, so both cost and remaining balance need error feedback.
    OVER_LIMIT_AND_INSUFFICIENT_CREDITS
}

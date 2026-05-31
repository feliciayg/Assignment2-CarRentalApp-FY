package com.example.assignment2_carrentalapp_fy.model

data class Booking(
    val bookingId: String,
    val carId: String,
    val carName: String,
    val carModel: String,
    val carImageResId: Int,
    val rentalDays: Int,
    val totalCost: Int
)

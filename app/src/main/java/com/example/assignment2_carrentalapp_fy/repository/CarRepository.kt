package com.example.assignment2_carrentalapp_fy.repository

import android.util.Log
import com.example.assignment2_carrentalapp_fy.R
import com.example.assignment2_carrentalapp_fy.model.Booking
import com.example.assignment2_carrentalapp_fy.model.Car
import com.example.assignment2_carrentalapp_fy.model.FilterOption
import com.example.assignment2_carrentalapp_fy.model.SortOption
import java.util.UUID

// Central in-memory data source for the proof-of-concept app.
// Activities and ViewModels call this instead of editing car or booking state directly.
object CarRepository {
    private const val TAG = "CarRepositoryDebug"

    // Assignment rules are kept here so activities render state rather than own business rules.
    private const val startingCredit = 500
    private const val maxRentalTotal = 400
    private const val minRentalDays = 1
    private const val maxRentalDays = 7
    private const val minRating = 1f
    private const val maxRating = 5f

    private val initialCars = listOf(
        Car("car_1", "Proton", "X50", 2022, 4.4f, 28_000, 55, R.drawable.car1_proton),
        Car("car_2", "Mazda", "CX-5", 2023, 4.8f, 19_500, 62, R.drawable.car2_mazada),
        Car("car_3", "Mini", "Cooper", 2021, 4.2f, 34_200, 50, R.drawable.car3_minicooper),
        Car("car_4", "BYD", "Atto 3", 2024, 4.9f, 9_800, 78, R.drawable.car4_byd),
        Car("car_5", "Lexus", "UX", 2020, 4.1f, 41_000, 46, R.drawable.car5_lexus)
    )
    private val cars = initialCars.toMutableList()

    // In-memory proof-of-concept state. Nothing here is written to disk.
    private var activeBooking: Booking? = null
    private var creditBalance: Int = startingCredit

    init {
        // Assignment requires exactly five rentable cars.
        require(cars.size == 5) { "The repository must contain exactly 5 cars." }
    }

    fun getAllCars(): List<Car> = cars.toList()

    fun getVisibleCars(
        searchQuery: String,
        sortOption: SortOption,
        filterOption: FilterOption
    ): List<Car> {
        val query = searchQuery.trim().lowercase()

        // Search, filter, and sort are applied to the current in-memory list without
        // mutating the base records, so the same data can be viewed in different ways.
        return cars.asSequence()
            .filter { car ->
                query.isBlank() ||
                    car.name.lowercase().contains(query) ||
                    car.model.lowercase().contains(query)
            }
            .filter { car ->
                when (filterOption) {
                    FilterOption.ALL -> true
                    FilterOption.AVAILABLE -> car.isAvailable
                    FilterOption.RENTED -> !car.isAvailable
                    FilterOption.FAVOURITES -> car.isFavourite
                }
            }
            .sortedWith(sortComparator(sortOption))
            .toList()
    }

    fun getCarById(carId: String): Car? = cars.firstOrNull { it.id == carId }

    fun getActiveBooking(): Booking? = activeBooking

    fun getCreditBalance(): Int = creditBalance

    fun getMaxRentalTotal(): Int = maxRentalTotal

    fun resetForTesting() {
        // Espresso tests call this to avoid state leaking between test cases.
        cars.clear()
        cars.addAll(initialCars)
        activeBooking = null
        creditBalance = startingCredit
    }

    fun setCreditBalanceForTesting(balance: Int) {
        creditBalance = balance
    }

    fun getFavouriteCars(): List<Car> {
        // Favourites are sorted consistently so the section stays predictable.
        return cars
            .filter { it.isFavourite }
            .sortedWith(sortComparator(SortOption.RATING_DESC))
    }

    fun canStartNewBooking(): Boolean = activeBooking == null

    fun validateBooking(carId: String, rentalDays: Int): String? {
        val car = getCarById(carId) ?: return "Selected car could not be found."
        // Aim: confirm booking validation rules and why a booking is rejected.
        Log.d(TAG, "validateBooking: carId=$carId, rentalDays=$rentalDays, activeBooking=${activeBooking != null}, creditBalance=$creditBalance")

        if (activeBooking != null) {
            return "You already have an active rental. Please end your current booking before renting another car."
        }

        if (!car.isAvailable) {
            return "This car is not currently available."
        }

        if (rentalDays !in minRentalDays..maxRentalDays) {
            return "Rental days must be between $minRentalDays and $maxRentalDays."
        }

        val totalCost = car.dailyCost * rentalDays
        if (totalCost > maxRentalTotal) {
            return "Rental total cannot exceed $maxRentalTotal credits."
        }

        if (totalCost > creditBalance) {
            return "Insufficient credits for this booking."
        }

        return null
    }

    fun rentCar(carId: String, rentalDays: Int): Result<Booking> {
        val validationError = validateBooking(carId, rentalDays)
        if (validationError != null) {
            return Result.failure(IllegalStateException(validationError))
        }

        val carIndex = cars.indexOfFirst { it.id == carId }
        if (carIndex == -1) {
            return Result.failure(NoSuchElementException("Selected car could not be found."))
        }

        val selectedCar = cars[carIndex]
        val totalCost = selectedCar.dailyCost * rentalDays
        // Copy the car details into the booking so the active booking card can still
        // display a stable summary even after the car availability changes.
        val booking = Booking(
            bookingId = UUID.randomUUID().toString(),
            carId = selectedCar.id,
            carName = selectedCar.name,
            carModel = selectedCar.model,
            carImageResId = selectedCar.imageResId,
            rentalDays = rentalDays,
            totalCost = totalCost
        )

        cars[carIndex] = selectedCar.copy(isAvailable = false)
        activeBooking = booking
        creditBalance -= totalCost
        // Aim: confirm a booking is successfully created and the credit balance is reduced.
        Log.d(TAG, "rentCar success: carId=${selectedCar.id}, totalCost=$totalCost, newBalance=$creditBalance")

        return Result.success(booking)
    }

    fun endRental(): Result<Booking> {
        val booking = activeBooking
            ?: return Result.failure(IllegalStateException("There is no active booking to end."))

        val carIndex = cars.indexOfFirst { it.id == booking.carId }
        if (carIndex >= 0) {
            cars[carIndex] = cars[carIndex].copy(isAvailable = true)
        }

        activeBooking = null
        // Aim: confirm ending a rental restores the car availability and clears the active booking.
        Log.d(TAG, "endRental: bookingId=${booking.bookingId}, carId=${booking.carId}")
        return Result.success(booking)
    }

    fun updateCarRating(carId: String, newRating: Float): Result<Car> {
        if (newRating !in minRating..maxRating) {
            return Result.failure(IllegalArgumentException("Rating must be between 1 and 5."))
        }

        val carIndex = cars.indexOfFirst { it.id == carId }
        if (carIndex == -1) {
            return Result.failure(NoSuchElementException("Car to update was not found."))
        }

        val updatedCar = cars[carIndex].copy(rating = newRating)
        cars[carIndex] = updatedCar
        return Result.success(updatedCar)
    }

    fun toggleFavourite(carId: String): Result<Car> {
        val carIndex = cars.indexOfFirst { it.id == carId }
        if (carIndex == -1) {
            return Result.failure(NoSuchElementException("Car to update was not found."))
        }

        val updatedCar = cars[carIndex].copy(isFavourite = !cars[carIndex].isFavourite)
        cars[carIndex] = updatedCar
        // Aim: confirm favourite changes persist in repository state.
        Log.d(TAG, "toggleFavourite: carId=$carId, newState=${updatedCar.isFavourite}")
        return Result.success(updatedCar)
    }

    private fun sortComparator(sortOption: SortOption): Comparator<Car> {
        // Tie-breakers keep ordering stable when two cars share the same primary sort value.
        return when (sortOption) {
            SortOption.RATING_DESC -> compareByDescending<Car> { it.rating }
                .thenByDescending { it.year }
                .thenBy { it.dailyCost }
            SortOption.YEAR_DESC -> compareByDescending<Car> { it.year }
                .thenByDescending { it.rating }
                .thenBy { it.dailyCost }
            SortOption.COST_ASC -> compareBy<Car> { it.dailyCost }
                .thenByDescending { it.rating }
                .thenByDescending { it.year }
        }
    }
}

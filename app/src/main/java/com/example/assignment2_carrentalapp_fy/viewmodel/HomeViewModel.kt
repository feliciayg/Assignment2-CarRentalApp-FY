package com.example.assignment2_carrentalapp_fy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.assignment2_carrentalapp_fy.model.Booking
import com.example.assignment2_carrentalapp_fy.model.Car
import com.example.assignment2_carrentalapp_fy.model.FilterOption
import com.example.assignment2_carrentalapp_fy.model.SortOption
import com.example.assignment2_carrentalapp_fy.repository.CarRepository
import com.example.assignment2_carrentalapp_fy.state.HomeUiState

// Prepares all data needed by MainActivity. The activity should render HomeUiState
// instead of querying the repository directly.
class HomeViewModel(
    private val repository: CarRepository = CarRepository
) : ViewModel() {
    companion object {
        private const val TAG = "HomeViewModelDebug"
    }

    var homeUiState: HomeUiState = HomeUiState()
        private set

    private var currentIndex = 0
    private var searchQuery = ""
    private var selectedSort = SortOption.RATING_DESC
    private var selectedFilter = FilterOption.AVAILABLE

    init {
        refresh()
    }

    fun refresh() {
        val visibleCars = repository.getVisibleCars(
            searchQuery = searchQuery,
            sortOption = selectedSort,
            filterOption = selectedFilter
        )

        if (visibleCars.isEmpty()) {
            currentIndex = 0
        } else if (currentIndex > visibleCars.lastIndex) {
            currentIndex = visibleCars.lastIndex
        }

        val currentCar = visibleCars.getOrNull(currentIndex)
        // Aim: confirm refresh builds the correct UI state after search, sort, filter, or booking changes.
        Log.d(
            TAG,
            "refresh: visibleCars=${visibleCars.size}, currentIndex=$currentIndex, selectedFilter=$selectedFilter, selectedSort=$selectedSort"
        )
        // MainActivity renders from one HomeUiState snapshot after every search,
        // filter, favourite, booking, or rating change.
        homeUiState = HomeUiState(
            visibleCars = visibleCars,
            currentCar = currentCar,
            currentIndex = currentIndex,
            totalCars = visibleCars.size,
            favourites = repository.getFavouriteCars(),
            activeBooking = repository.getActiveBooking(),
            balance = repository.getCreditBalance(),
            searchQuery = searchQuery,
            selectedSort = selectedSort,
            selectedFilter = selectedFilter,
            canRent = currentCar != null && repository.canStartNewBooking(),
            emptyMessage = if (visibleCars.isEmpty()) "No cars match your current search or filter." else null
        )
    }

    fun nextCar() {
        val cars = homeUiState.visibleCars
        if (cars.isNotEmpty()) {
            // Wrap around so browsing never gets stuck at the end of the list.
            currentIndex = (currentIndex + 1) % cars.size
            refresh()
        }
    }

    fun previousCar() {
        val cars = homeUiState.visibleCars
        if (cars.isNotEmpty()) {
            // Wrap to the last car when moving backward from the first car.
            currentIndex = if (currentIndex == 0) cars.lastIndex else currentIndex - 1
            refresh()
        }
    }

    fun searchCars(query: String) {
        // Reset index so the first matching car is shown after the result set changes.
        searchQuery = query
        currentIndex = 0
        refresh()
    }

    fun sortCars(option: SortOption) {
        selectedSort = option
        currentIndex = 0
        refresh()
    }

    fun filterCars(option: FilterOption) {
        selectedFilter = option
        currentIndex = 0
        refresh()
    }

    fun toggleFavourite(car: Car): Result<Car> {
        // Aim: confirm favourite toggles propagate through the view model and refresh the UI.
        Log.d(TAG, "toggleFavourite: carId=${car.id}, currentFavourite=${car.isFavourite}")
        val result = repository.toggleFavourite(car.id)
        refresh()
        return result
    }

    fun getCarById(carId: String): Car? = repository.getCarById(carId)

    fun endRental(): Result<Booking> {
        val result = repository.endRental()
        refresh()
        return result
    }

    fun submitRatingAfterRental(carId: String, rating: Float): Result<Car> {
        val result = repository.updateCarRating(carId, rating)
        refresh()
        return result
    }

    fun selectFavouriteCar(carId: String) {
        // Selecting a favourite clears restrictive filters so the chosen car can be
        // shown in the main one-car browsing card.
        searchQuery = ""
        selectedFilter = FilterOption.ALL
        val visibleCars = repository.getVisibleCars(
            searchQuery = searchQuery,
            sortOption = selectedSort,
            filterOption = selectedFilter
        )
        val index = visibleCars.indexOfFirst { it.id == carId }
        if (index >= 0) {
            currentIndex = index
            refresh()
        }
    }
}

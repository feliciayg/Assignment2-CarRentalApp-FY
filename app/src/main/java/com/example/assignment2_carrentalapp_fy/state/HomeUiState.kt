package com.example.assignment2_carrentalapp_fy.state

import com.example.assignment2_carrentalapp_fy.model.Booking
import com.example.assignment2_carrentalapp_fy.model.Car
import com.example.assignment2_carrentalapp_fy.model.FilterOption
import com.example.assignment2_carrentalapp_fy.model.SortOption

// A render-ready snapshot for MainActivity. Keeping these values together makes
// search, filter, favourites, active booking, and credit updates easier to reason about.
data class HomeUiState(
    val visibleCars: List<Car> = emptyList(),
    val currentCar: Car? = null,
    val currentIndex: Int = 0,
    val totalCars: Int = 0,
    val favourites: List<Car> = emptyList(),
    val activeBooking: Booking? = null,
    val balance: Int = 500,
    val searchQuery: String = "",
    val selectedSort: SortOption = SortOption.RATING_DESC,
    val selectedFilter: FilterOption = FilterOption.AVAILABLE,
    val canRent: Boolean = false,
    val emptyMessage: String? = null
)

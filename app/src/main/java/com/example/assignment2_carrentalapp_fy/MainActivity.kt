package com.example.assignment2_carrentalapp_fy

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.assignment2_carrentalapp_fy.databinding.ActivityMainBinding
import com.example.assignment2_carrentalapp_fy.fragment.ActiveBookingFragment
import com.example.assignment2_carrentalapp_fy.fragment.FavouritesFragment
import com.example.assignment2_carrentalapp_fy.model.Booking
import com.example.assignment2_carrentalapp_fy.model.FilterOption
import com.example.assignment2_carrentalapp_fy.model.SortOption
import com.example.assignment2_carrentalapp_fy.model.formattedKilometres
import com.example.assignment2_carrentalapp_fy.model.formattedRating
import com.example.assignment2_carrentalapp_fy.model.nameWithModel
import com.example.assignment2_carrentalapp_fy.ui.RentActivity
import com.example.assignment2_carrentalapp_fy.util.DialogHelper
import com.example.assignment2_carrentalapp_fy.util.SnackbarHelper
import com.example.assignment2_carrentalapp_fy.util.ThemeHelper
import com.example.assignment2_carrentalapp_fy.viewmodel.HomeViewModel

// Home screen controller. It renders HomeUiState, handles top-level user actions,
// and coordinates child fragments without storing business rules in the activity.
class MainActivity : AppCompatActivity(),
    ActiveBookingFragment.Listener,
    FavouritesFragment.Listener {

    companion object {
        private const val TAG = "MainActivityDebug"
        private const val SHARE_EMAIL = "feliciayongys@gmail.com"
    }

    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var activeBookingFragment: ActiveBookingFragment
    private lateinit var favouritesFragment: FavouritesFragment
    private var isUpdatingThemeSwitch = false

    // Refresh the home state after RentActivity finishes because saving a booking
    // changes credit balance, car availability, and the active booking card.
    private val rentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            homeViewModel.refresh()
            render()

            if (result.resultCode == Activity.RESULT_OK) {
                SnackbarHelper.showShort(binding.main, getString(R.string.booking_saved_message))
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                SnackbarHelper.showShort(binding.main, getString(R.string.booking_cancelled_message))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonFavouritesShortcut.paintFlags =
            binding.buttonFavouritesShortcut.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        setSupportActionBar(binding.toolbarMain)
        if (savedInstanceState == null) {
            activeBookingFragment = ActiveBookingFragment()
            favouritesFragment = FavouritesFragment()
            attachFragments()
        } else {
            activeBookingFragment =
                supportFragmentManager.findFragmentById(binding.fragmentActiveBookingContainer.id) as? ActiveBookingFragment
                    ?: ActiveBookingFragment()
            favouritesFragment =
                supportFragmentManager.findFragmentById(binding.fragmentFavouritesContainer.id) as? FavouritesFragment
                    ?: FavouritesFragment()
        }
        bindActions()
        render()
    }

    override fun onEndRentalClicked() {
        val activeBooking = homeViewModel.homeUiState.activeBooking ?: return
        // Ending a rental is confirmed first because it changes car availability
        // and opens the post-rental rating flow.
        DialogHelper.showCurrentBookingDialog(
            context = this,
            booking = activeBooking,
            onEndRental = {
                endActiveRentalAndPromptRating()
            },
            onClose = {}
        )
    }

    override fun onFavouriteSelected(carId: String) {
        homeViewModel.selectFavouriteCar(carId)
        render()
    }

    override fun onFavouriteRemoved(carId: String) {
        val car = homeViewModel.getCarById(carId) ?: return
        homeViewModel.toggleFavourite(car)
        render()
        SnackbarHelper.showWithAction(
            binding.main,
            getString(R.string.car_removed_from_favourites_message),
            getString(R.string.undo)
        ) {
            homeViewModel.getCarById(carId)?.let { restoredCar ->
                homeViewModel.toggleFavourite(restoredCar)
                render()
            }
        }
    }

    private fun attachFragments() {
        // Fragments are attached once and later updated through bind/submit methods.
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentActiveBookingContainer.id, activeBookingFragment)
            .replace(binding.fragmentFavouritesContainer.id, favouritesFragment)
            .commitNow()
    }

    private fun bindActions() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingThemeSwitch) return@setOnCheckedChangeListener
            ThemeHelper.applyDarkMode(isChecked)
            updateThemeIndicator(isChecked)
        }

        binding.editTextSearchCars.doAfterTextChanged {
            homeViewModel.searchCars(it?.toString().orEmpty())
            // Aim: confirm search input updates and affects the visible car list.
            Log.d(TAG, "search changed: query=${it?.toString().orEmpty()}")
            render()
        }

        binding.buttonSort.setOnClickListener {
            DialogHelper.showSortDialog(this, homeViewModel.homeUiState.selectedSort) { selected ->
                homeViewModel.sortCars(selected)
                render()
            }
        }

        binding.buttonFilter.setOnClickListener {
            DialogHelper.showFilterDialog(this, homeViewModel.homeUiState.selectedFilter) { selected ->
                homeViewModel.filterCars(selected)
                render()
            }
        }

        binding.buttonFavouritesShortcut.setOnClickListener {
            // The shortcut keeps favourites discoverable even when the section is below the fold.
            binding.scrollMainContent.post {
                binding.scrollMainContent.smoothScrollTo(0, binding.fragmentFavouritesContainer.top)
            }
        }

        binding.buttonPreviousCar.setOnClickListener {
            homeViewModel.previousCar()
            render()
        }

        binding.buttonNextCar.setOnClickListener {
            homeViewModel.nextCar()
            render()
        }

        binding.buttonCurrentFavourite.setOnClickListener {
            val currentCar = homeViewModel.homeUiState.currentCar ?: return@setOnClickListener
            val wasFavourite = currentCar.isFavourite
            val carId = currentCar.id
            // Aim: confirm favourite button was pressed and whether the car was previously favourited.
            Log.d(TAG, "favourite click: carId=${currentCar.id}, wasFavourite=$wasFavourite")
            homeViewModel.toggleFavourite(currentCar)
            render()
            if (!wasFavourite) {
                SnackbarHelper.showWithAction(
                    binding.main,
                    getString(R.string.car_added_to_favourites_message),
                    getString(R.string.undo)
                ) {
                    homeViewModel.getCarById(carId)?.let { car ->
                        homeViewModel.toggleFavourite(car)
                        render()
                    }
                }
            } else {
                SnackbarHelper.showWithAction(
                    binding.main,
                    getString(R.string.car_removed_from_favourites_message),
                    getString(R.string.undo)
                ) {
                    homeViewModel.getCarById(carId)?.let { car ->
                        homeViewModel.toggleFavourite(car)
                        render()
                    }
                }
            }
        }

        binding.buttonCurrentShare.setOnClickListener {
            val currentCar = homeViewModel.homeUiState.currentCar ?: return@setOnClickListener
            shareCar(currentCar.nameWithModel())
        }

        binding.buttonRentCar.setOnClickListener {
            val currentCar = homeViewModel.homeUiState.currentCar ?: return@setOnClickListener
            // Aim: confirm whether the rent action is being blocked by an active booking.
            Log.d(TAG, "rent click: carId=${currentCar.id}, activeBookingExists=${homeViewModel.homeUiState.activeBooking != null}")
            if (homeViewModel.homeUiState.activeBooking != null) {
                DialogHelper.showBookingBlockedDialog(this) {
                    endActiveRentalAndPromptRating()
                }
                return@setOnClickListener
            }

            reopenBookingDetails(currentCar)
        }
    }

    private fun render() {
        val state = homeViewModel.homeUiState
        // Aim: confirm the home screen is rendering the expected browsing and booking state.
        Log.d(
            TAG,
            "render: currentCar=${state.currentCar?.nameWithModel()}, totalCars=${state.totalCars}, currentIndex=${state.currentIndex}, activeBooking=${state.activeBooking != null}"
        )
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        ThemeHelper.applySystemBars(this, isDarkMode)
        if (binding.switchDarkMode.isChecked != isDarkMode) {
            isUpdatingThemeSwitch = true
            binding.switchDarkMode.isChecked = isDarkMode
            isUpdatingThemeSwitch = false
        }
        updateThemeIndicator(isDarkMode)
        binding.textBalanceChip.text = getString(R.string.balance_chip_format, state.balance)
        binding.textPositionIndicator.text =
            getString(
                R.string.position_indicator_format,
                if (state.totalCars == 0) 0 else state.currentIndex + 1,
                state.totalCars
            )

        binding.buttonSort.text = getString(
            when (state.selectedSort) {
                SortOption.RATING_DESC -> R.string.sort_button_default
                SortOption.YEAR_DESC -> R.string.sort_button_year
                SortOption.COST_ASC -> R.string.sort_button_cost
            }
        )

        binding.buttonFilter.text = getString(
            when (state.selectedFilter) {
                FilterOption.ALL -> R.string.filter_button_all
                FilterOption.AVAILABLE -> R.string.filter_button_available
                FilterOption.RENTED -> R.string.filter_button_rented
                FilterOption.FAVOURITES -> R.string.filter_button_favourites
            }
        )
        binding.buttonFavouritesShortcut.text = getString(
            R.string.view_all_favourites_count_format,
            state.favourites.size
        )

        val hasCurrentCar = state.currentCar != null
        binding.cardCurrentCar.visibility = if (hasCurrentCar) View.VISIBLE else View.GONE
        binding.cardEmptyCars.visibility = if (hasCurrentCar) View.GONE else View.VISIBLE
        binding.layoutPagerButtons.visibility = if (hasCurrentCar) View.VISIBLE else View.GONE
        binding.buttonRentCar.visibility = if (hasCurrentCar) View.VISIBLE else View.GONE
        binding.textEmptyCarsMessage.text = state.emptyMessage ?: getString(R.string.no_cars_match_search)

        state.currentCar?.let { car ->
            binding.imageCurrentCar.setImageResource(car.imageResId)
            binding.textCurrentCarName.text = car.nameWithModel()
            binding.textCurrentRatingValue.text = car.formattedRating()
            binding.ratingCurrentCar.rating = car.rating
            binding.textCurrentCarModelText.text = getString(R.string.model_line_format, car.model)
            binding.textCurrentCarYearText.text = car.year.toString()
            binding.textCurrentCarMileageText.text = getString(
                R.string.mileage_value_format,
                car.formattedKilometres()
            )
            binding.textCurrentDailyCost.text = getString(R.string.daily_cost_format, car.dailyCost)
            bindFavouriteIcon(binding.buttonCurrentFavourite, car.isFavourite)
        }

        binding.buttonPreviousCar.isEnabled = state.totalCars > 1
        binding.buttonNextCar.isEnabled = state.totalCars > 1
        val rentBlockedByBooking = state.activeBooking != null
        binding.buttonRentCar.isEnabled = state.currentCar != null
        // Keep the button visible but muted when one-active-booking rule blocks renting.
        binding.buttonRentCar.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (rentBlockedByBooking) R.color.sr_button_disabled else R.color.sr_primary
            )
        )

        binding.fragmentActiveBookingContainer.visibility =
            if (state.activeBooking != null) View.VISIBLE else View.GONE
        // Fragments stay presentation-focused; MainActivity decides what state they show.
        activeBookingFragment.bindBooking(state.activeBooking)
        favouritesFragment.submitFavourites(state.favourites)
    }

    private fun updateThemeIndicator(isDarkMode: Boolean) {
        binding.imageThemeIndicator.setImageResource(
            if (isDarkMode) R.drawable.ic_moon else R.drawable.ic_sun
        )
    }

    private fun endActiveRentalAndPromptRating() {
        homeViewModel.endRental()
            .onSuccess { endedBooking ->
                render()
                showRatingDialogFor(endedBooking)
            }
            .onFailure {
                DialogHelper.showError(
                    this,
                    getString(R.string.end_rental),
                    it.message ?: getString(R.string.generic_error_message)
                )
            }
    }

    private fun showRatingDialogFor(booking: Booking) {
        val currentRating = homeViewModel.getCarById(booking.carId)?.rating?.toInt() ?: 5
        // Rating is collected after ending a rental so feedback is tied to a completed booking.
        DialogHelper.showRatingDialog(this, currentRating) { rating ->
            homeViewModel.submitRatingAfterRental(booking.carId, rating.toFloat())
            render()
            SnackbarHelper.showShort(binding.main, getString(R.string.rating_saved_message))
        }
    }

    private fun reopenBookingDetails(
        car: com.example.assignment2_carrentalapp_fy.model.Car,
        selectedDays: Int = 1
    ) {
        // RentActivity receives a Parcelable Car and returns OK/CANCELED through rentLauncher.
        rentLauncher.launch(
            RentActivity.createIntent(this, car, selectedDays),
            ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
        )
        overridePendingTransition(0, 0)
    }

    private fun shareCar(carName: String) {
        // Implicit share intent lets the user choose a compatible external app.
        startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(SHARE_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_car))
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.share_car_message, carName))
                },
                getString(R.string.share_car)
            )
        )
    }

    private fun bindFavouriteIcon(button: ImageButton, isFavourite: Boolean) {
        val iconRes = if (isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        val tintRes = if (isFavourite) R.color.sr_favourite_red else R.color.sr_icon_grey
        button.setImageResource(iconRes)
        button.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, tintRes)
        )
    }
}

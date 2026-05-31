package com.example.assignment2_carrentalapp_fy

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment2_carrentalapp_fy.repository.CarRepository
import com.example.assignment2_carrentalapp_fy.ui.RentActivity
import com.google.android.material.slider.Slider
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RentActivityTest {

    @Before
    fun setUp() {
        CarRepository.resetForTesting()
    }

    @After
    fun tearDown() {
        CarRepository.resetForTesting()
    }

    private fun buildIntent(carId: String = "car_1"): Intent {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val car = requireNotNull(CarRepository.getCarById(carId))
        return RentActivity.createIntent(context, car)
    }

    private fun string(id: Int, vararg args: Any): String {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return context.getString(id, *args)
    }

    private fun color(id: Int): Int {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return androidx.core.content.ContextCompat.getColor(context, id)
    }

    private fun setRentalDays(scenario: ActivityScenario<RentActivity>, days: Int) {
        scenario.onActivity { activity ->
            activity.findViewById<Slider>(R.id.sliderRentalDays).value = days.toFloat()
        }
    }

    private fun withCurrentTextColor(expectedColor: Int): Matcher<android.view.View> {
        return object : TypeSafeMatcher<android.view.View>() {
            override fun describeTo(description: Description) {
                description.appendText("with current text color: $expectedColor")
            }

            override fun matchesSafely(item: android.view.View): Boolean {
                return item is TextView && item.currentTextColor == expectedColor
            }
        }
    }

    // Aim: verify that Booking Details opens and shows selected car content.
    @Test
    fun rentScreen_displaysSelectedCarDetails() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withText(string(R.string.booking_details))).check(matches(isDisplayed()))
        onView(withId(R.id.textSelectedCarName)).check(matches(isDisplayed()))
        onView(withId(R.id.sliderRentalDays)).check(matches(isDisplayed()))
    }

    // Aim: verify that the rent screen shows the credit balance card.
    @Test
    fun rentScreen_displaysCreditBalanceCard() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.cardRentBalanceChip)).check(matches(isDisplayed()))
        onView(withId(R.id.textRentBalanceChip))
            .check(matches(withText(string(R.string.balance_chip_format, CarRepository.getCreditBalance()))))
    }

    // Aim: verify that the rental days and cost summary sections are shown on the booking screen.
    @Test
    fun rentScreen_displaysSummarySections() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.textRentalDaysLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.textCostSummaryLabel)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.textSummaryTotalCost)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.textRemainingBalance)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    // Aim: verify that favourite add on booking page shows snackbar with Undo.
    @Test
    fun rentFavouriteAdd_showsSnackbarWithUndo() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.buttonSelectedFavourite)).perform(click())

        onView(withText(string(R.string.car_added_to_favourites_message))).check(matches(isDisplayed()))
        onView(withText(string(R.string.undo))).check(matches(isDisplayed()))
    }

    // Aim: verify that cancel booking button is visible and clickable.
    @Test
    fun cancelButton_isVisible() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.buttonCancelBooking)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    // Aim: verify that the save booking action is available on the booking screen.
    @Test
    fun saveButton_isVisible() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.buttonSaveBooking)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    // Aim: verify that the booking screen shows the selected rental days feedback for the slider.
    @Test
    fun rentScreen_displaysSelectedDaysChip() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.textSelectedDays)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.textSelectedDays))
            .check(matches(withText(string(R.string.selected_days_format, 1))))
    }

    // Aim: verify that the booking warning is hidden when the booking is valid.
    @Test
    fun bookingWarning_isHiddenWhenBookingIsValid() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withId(R.id.cardBookingWarning))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // Aim: verify that exceeding the 400-credit limit highlights only the total cost and shows the correct warning.
    @Test
    fun bookingWarning_overLimitOnly_showsLimitWarningAndKeepsRemainingCreditNormal() {
        val scenario = ActivityScenario.launch<RentActivity>(buildIntent(carId = "car_4"))

        setRentalDays(scenario, 6)

        onView(withId(R.id.textBookingWarning)).perform(scrollTo()).check(
            matches(withText(string(R.string.warning_booking_exceeds_limit)))
        )
        onView(withId(R.id.textSummaryTotalCost))
            .perform(scrollTo())
            .check(matches(withCurrentTextColor(color(R.color.sr_error_red))))
        onView(withId(R.id.textRemainingBalance))
            .perform(scrollTo())
            .check(matches(withCurrentTextColor(color(R.color.sr_text_primary))))
    }

    // Aim: verify that insufficient credits without the 400-credit limit shows the credit warning.
    @Test
    fun bookingWarning_insufficientCreditsOnly_showsCreditWarning() {
        CarRepository.setCreditBalanceForTesting(350)
        val scenario = ActivityScenario.launch<RentActivity>(buildIntent(carId = "car_4"))

        setRentalDays(scenario, 5)

        onView(withId(R.id.textBookingWarning)).perform(scrollTo()).check(
            matches(withText(string(R.string.warning_insufficient_credits)))
        )
        onView(withId(R.id.textSummaryTotalCost))
            .perform(scrollTo())
            .check(matches(withCurrentTextColor(color(R.color.sr_primary))))
        onView(withId(R.id.textRemainingBalance))
            .perform(scrollTo())
            .check(matches(withCurrentTextColor(color(R.color.sr_error_red))))
    }

    // Aim: verify that failing both booking rules shows the combined warning message.
    @Test
    fun bookingWarning_combinedInvalid_showsCombinedWarning() {
        val scenario = ActivityScenario.launch<RentActivity>(buildIntent(carId = "car_4"))

        setRentalDays(scenario, 7)

        onView(withId(R.id.textBookingWarning)).perform(scrollTo()).check(
            matches(withText(string(R.string.warning_booking_exceeds_limit_and_insufficient_credits)))
        )
    }

    // Aim: verify that saving an over-limit booking shows the matching validation dialog.
    @Test
    fun saveBooking_overLimitOnly_showsLimitDialog() {
        val scenario = ActivityScenario.launch<RentActivity>(buildIntent(carId = "car_4"))

        setRentalDays(scenario, 6)
        onView(withId(R.id.buttonSaveBooking)).perform(scrollTo(), click())

        onView(withText(string(R.string.booking_details))).check(matches(isDisplayed()))
        onView(withText(string(R.string.rental_total_limit_message, CarRepository.getMaxRentalTotal())))
            .check(matches(isDisplayed()))
    }

    // Aim: verify that saving a booking with insufficient credits shows the matching validation dialog.
    @Test
    fun saveBooking_insufficientCreditsOnly_showsCreditDialog() {
        CarRepository.setCreditBalanceForTesting(350)
        val scenario = ActivityScenario.launch<RentActivity>(buildIntent(carId = "car_4"))

        setRentalDays(scenario, 5)
        onView(withId(R.id.buttonSaveBooking)).perform(scrollTo(), click())

        onView(withText(string(R.string.insufficient_credit_title))).check(matches(isDisplayed()))
        onView(withText(string(R.string.insufficient_credit_reduce_duration_message)))
            .check(matches(isDisplayed()))
    }

    // Aim: verify that saving a booking that fails both rules shows the combined validation dialog.
    @Test
    fun saveBooking_combinedInvalid_showsCombinedDialog() {
        val scenario = ActivityScenario.launch<RentActivity>(buildIntent(carId = "car_4"))

        setRentalDays(scenario, 7)
        onView(withId(R.id.buttonSaveBooking)).perform(scrollTo(), click())

        onView(withText(string(R.string.invalid_booking_title))).check(matches(isDisplayed()))
        onView(withText(string(R.string.combined_booking_validation_message, CarRepository.getMaxRentalTotal())))
            .check(matches(isDisplayed()))
    }

    // Aim: verify that the credit balance label is visible on the booking header card.
    @Test
    fun creditBalanceLabel_isVisible() {
        ActivityScenario.launch<RentActivity>(buildIntent())

        onView(withText(string(R.string.credit_balance).uppercase())).check(matches(isDisplayed()))
    }
}

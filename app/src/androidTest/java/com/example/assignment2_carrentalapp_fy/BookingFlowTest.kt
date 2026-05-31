package com.example.assignment2_carrentalapp_fy

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment2_carrentalapp_fy.repository.CarRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookingFlowTest {

    @Before
    fun setUp() {
        CarRepository.resetForTesting()
    }

    @After
    fun tearDown() {
        CarRepository.resetForTesting()
    }

    // Aim: verify that user can open booking details from the home screen.
    @Test
    fun rentButton_opensBookingDetails() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonRentCar)).perform(click())
        onView(withText("Booking Details")).check(matches(isDisplayed()))
    }

    // Aim: verify that the active booking container exists in the layout and is hidden before any booking is made.
    @Test
    fun activeBookingContainer_existsInLayout() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fragmentActiveBookingContainer))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // Aim: verify that the home screen keeps the rent action visible before any booking is created.
    @Test
    fun rentButton_isVisibleBeforeBooking() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonRentCar)).check(matches(isDisplayed()))
    }
}

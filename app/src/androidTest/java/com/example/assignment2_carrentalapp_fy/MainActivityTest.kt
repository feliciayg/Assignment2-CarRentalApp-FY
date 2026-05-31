package com.example.assignment2_carrentalapp_fy

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.assignment2_carrentalapp_fy.repository.CarRepository
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setUp() {
        CarRepository.resetForTesting()
    }

    @After
    fun tearDown() {
        CarRepository.resetForTesting()
    }

    // Aim: verify that the home screen loads and shows the main browsing UI.
    @Test
    fun homeScreen_displaysMainUi() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.toolbarMain)).check(matches(isDisplayed()))
        onView(withId(R.id.textFindCarTitle)).check(matches(withText("Find a car")))
        onView(withId(R.id.buttonRentCar)).check(matches(isDisplayed()))
    }

    // Aim: verify that tapping Next changes the currently shown car.
    @Test
    fun nextButton_changesDisplayedCar() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.textCurrentCarName)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonNextCar)).perform(click())
        onView(withId(R.id.textPositionIndicator)).check(matches(withText(containsString("of 5"))))
    }

    // Aim: verify that a search with no match shows the empty state card.
    @Test
    fun search_noMatch_showsEmptyState() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.editTextSearchCars)).perform(replaceText("zzznomatch"))
        closeSoftKeyboard()

        onView(withId(R.id.cardEmptyCars)).check(matches(isDisplayed()))
        onView(withId(R.id.textEmptyCarsMessage))
            .check(matches(withText(containsString("No cars match"))))
    }

    // Aim: verify that the sort and filter controls are visible on the home screen.
    @Test
    fun sortAndFilterButtons_areVisible() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonSort)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonFilter)).check(matches(isDisplayed()))
        onView(withId(R.id.textPositionIndicator)).check(matches(isDisplayed()))
    }

    // Aim: verify that tapping the dark mode switch updates its checked state.
    @Test
    fun darkModeToggle_changesCheckedState() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.switchDarkMode)).perform(click())
        onView(withId(R.id.switchDarkMode)).check(matches(isChecked()))
    }

    // Aim: verify that the favourites section is present on the home screen for quick access.
    @Test
    fun favouritesSection_isVisible() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.fragmentFavouritesContainer)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    // Aim: verify that adding a favourite shows snackbar feedback with Undo.
    @Test
    fun favouriteAdd_showsSnackbarWithUndo() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonCurrentFavourite)).perform(click())

        onView(withText("Car added to favourites")).check(matches(isDisplayed()))
        onView(withText("Undo")).check(matches(isDisplayed()))
    }

    // Aim: verify that removing a favourite shows snackbar feedback with Undo.
    @Test
    fun favouriteRemove_showsSnackbarWithUndo() {
        if (CarRepository.getCarById("car_4")?.isFavourite == false) {
            CarRepository.toggleFavourite("car_4")
        }
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonCurrentFavourite)).perform(click())

        onView(withText("Car removed from favourites")).check(matches(isDisplayed()))
        onView(withText("Undo")).check(matches(isDisplayed()))
    }
}

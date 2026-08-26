package com.asptechinc.daymark

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.asptechinc.daymark", appContext.packageName)
    }

    @Test
    fun testAddActivityButton_opensNewActivity() {
        // Click the FAB to add a new activity
        onView(withId(R.id.btn_add_activity)).perform(click())

        // Check if NewActivity is opened by verifying the toolbar title
        val expectedTitle =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.screen_new_activity)
        onView(withText(expectedTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun testSearchButton_opensSearchDialogue() {
        // Click the search button in the menu
        onView(withId(R.id.searchButton)).perform(click())

        // Check if the search dialogue is displayed by looking for search_input
        onView(withId(R.id.search_input)).check(matches(isDisplayed()))
    }
}

package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testBottomNavigation_JoinEvent() throws InterruptedException {
        // Make sure view is fully loaded
        Thread.sleep(1000);

        // Click the Join Event button
        onView(withId(R.id.navigation_join_event))
                .check(matches(isDisplayed()))
                .perform(click());

        // Verify we're on the Join Event screen by checking for any view that's definitely in your join event fragment
        // Replace R.id.your_join_event_view with an actual view ID from your join event fragment
        Thread.sleep(1000); // Give time for navigation
        onView(withId(R.id.fragment_join_event))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBottomNavigation_CreateEvent() throws InterruptedException {
        // Make sure view is fully loaded
        Thread.sleep(1000);

        // Click the Create Event button
        onView(withId(R.id.navigation_create_event))
                .check(matches(isDisplayed()))
                .perform(click());

        // Verify we're on the Create Event screen
        Thread.sleep(1000); // Give time for navigation
        onView(withId(R.id.fragment_create_event))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBottomNavigation_Home() throws InterruptedException {
        // Make sure view is fully loaded
        Thread.sleep(1000);

        // First navigate away from home
        onView(withId(R.id.navigation_create_event))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(1000); // Give time for navigation

        // Then click home button
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());

        // Verify we're back on the home screen
        Thread.sleep(1000); // Give time for navigation
        onView(withId(R.id.fragment_home))
                .check(matches(isDisplayed()));
    }
}
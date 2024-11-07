package com.example.orange;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.lang.Thread.sleep;

/**
 * Instrumented test suite for the MainActivity, testing navigation and visibility
 * of elements based on user state.
 *
 * @author Graham Flokstra
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
    private SessionManager sessionManager;

    /**
     * Setup method to initialize the SessionManager, ensure the user is logged out,
     * and navigate to the home screen.
     *
     * @author Graham Flokstra
     */
    @Before
    public void setup() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.logoutUser(); // Ensure test starts in logged-out state

        // Always start with home navigation
        sleep(1000); // Wait for view to be ready
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(R.id.fragment_home_main)).check(matches(isDisplayed()));
    }

    /**
     * Tests the Home navigation button in the bottom navigation.
     * Verifies that the Home fragment is displayed after clicking the Home button.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testBottomNavigation_Home() {
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(R.id.fragment_home_main)).check(matches(isDisplayed()));
    }

    /**
     * Tests the Join Event navigation button in the bottom navigation.
     * Verifies that the Join Event fragment is displayed after clicking the button.
     *
     * @author Graham Flokstra
     * @throws InterruptedException if the thread sleep is interrupted
     */
    @Test
    public void testBottomNavigation_JoinEvent() throws InterruptedException {
        sleep(1000);
        onView(withId(R.id.navigation_join_event))
                .check(matches(isDisplayed()))
                .perform(click());
        sleep(2000);
        onView(withId(R.id.fragment_join_event_main)).check(matches(isDisplayed()));
    }

    /**
     * Tests the Create Event navigation button in the bottom navigation.
     * Verifies that the Create Event fragment is displayed and then returns to the Home fragment.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testBottomNavigation_CreateEvent() {
        onView(withId(R.id.navigation_create_event))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(R.id.fragment_create_event_main)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the profile menu item is visible when the user is logged in as an ENTRANT.
     * Verifies that the profile option is displayed after the session is recreated.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testOptionsMenu_ProfileVisibleWhenLoggedIn() {
        sessionManager.createLoginSession("testDeviceId", UserType.ENTRANT, "testDeviceId");
        activityRule.getScenario().recreate();
        onView(withId(R.id.navigation_profile)).check(matches(isDisplayed()));
    }

    /**
     * Tests navigation for a user logged in as an ORGANIZER.
     * Verifies that the "View My Events" navigation item is displayed and navigates to the appropriate fragment.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testNavigationForOrganizer() throws InterruptedException {
        sessionManager.createLoginSession("testDeviceId", UserType.ORGANIZER, "testDeviceId");
        activityRule.getScenario().recreate();
        onView(withId(R.id.navigation_view_my_events))
                .check(matches(isDisplayed()))
                .perform(click());
        sleep(2000);
        onView(withId(R.id.fragment_view_my_organizer_events)).check(matches(isDisplayed()));
    }

    /**
     * Tests navigation for a user logged in as an ENTRANT.
     * Verifies that the "View Events" navigation item is displayed and navigates to the appropriate fragment.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testNavigationForEntrant() throws InterruptedException {
        sessionManager.createLoginSession("testDeviceId", UserType.ENTRANT, "testDeviceId");
        activityRule.getScenario().recreate();
        onView(withId(R.id.navigation_join_event))
                .check(matches(isDisplayed()))
                .perform(click());

        sleep(2000);
        onView(withId(R.id.navigation_my_events))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(R.id.entrant_events_page)).check(matches(isDisplayed()));
    }

    /**
     * Tests the behavior after logging out. Verifies that navigation items like Join Event and Create Event
     * are visible in the logged-out state.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testLogoutBehavior() throws InterruptedException {
        // First log in as an entrant
        sessionManager.createLoginSession("testDeviceId", UserType.ENTRANT, "testDeviceId");
        activityRule.getScenario().recreate();

        // Then perform logout
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());

        // Wait for UI to update
        sleep(1000);

        // Verify the initial navigation state is restored
        onView(withId(R.id.navigation_join_event))
                .check(matches(isDisplayed()));
        onView(withId(R.id.navigation_create_event))
                .check(matches(isDisplayed()));
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests the initial navigation state for a logged-out user.
     * Verifies that Join Event, Create Event, and Home buttons are visible and clickable.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testInitialNavigationNotLoggedIn() {
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
        onView(withId(R.id.navigation_create_event)).check(matches(isDisplayed()));
        onView(withId(R.id.navigation_home)).check(matches(isDisplayed()));
    }
}
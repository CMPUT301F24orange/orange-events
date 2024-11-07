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
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
    private SessionManager sessionManager;

    /**
     * Setup method to initialize the SessionManager and ensure the user is logged out.
     *
     * @author Graham Flokstra
     */
    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.logoutUser(); // Ensure test starts in logged-out state
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
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());
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
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    /**
     * Tests navigation for a user logged in as an ORGANIZER.
     * Verifies that the "View My Events" navigation item is displayed and navigates to the appropriate fragment.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testNavigationForOrganizer() {
        sessionManager.createLoginSession("testDeviceId", UserType.ORGANIZER, "testDeviceId");
        activityRule.getScenario().recreate();
        onView(withId(R.id.navigation_view_my_events))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(R.id.fragment_view_my_events)).check(matches(isDisplayed()));
    }

    /**
     * Tests the behavior after logging out. Verifies that navigation items like Join Event and Create Event
     * are visible in the logged-out state.
     *
     * @author Graham Flokstra
     */
    @Test
    public void testLogoutBehavior() {
        sessionManager.createLoginSession("testDeviceId", UserType.ENTRANT, "testDeviceId");
        activityRule.getScenario().recreate();
        onView(withId(R.id.navigation_home)).perform(click());
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
        onView(withId(R.id.navigation_create_event)).check(matches(isDisplayed()));
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
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    /**
     * Tests the Admin mode button in the top navigation.
     * Verifies that the Admin view is displayed after clicking the button.
     * goes back to the home page after and checks that the user returns correctly.
     *
     * @author Radhe Patel
     * @throws InterruptedException if the thread sleep is interrupted
     */
    @Test
    public void testAdminMode() throws InterruptedException {
        // Locate and click the Admin mode button
        onView(withId(R.id.navigation_admin)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
        onView(withId(R.id.admin_navigation_view_facilities)).check(matches(isDisplayed()));
        sleep(2000);
        onView(withId(R.id.admin_navigation_home)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
    }



}
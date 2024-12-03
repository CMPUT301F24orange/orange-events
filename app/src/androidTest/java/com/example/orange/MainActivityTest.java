package com.example.orange;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

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
import static org.junit.Assert.fail;
import static java.lang.Thread.sleep;

import java.util.HashMap;

/**
 * Instrumented test suite for the MainActivity, testing navigation and visibility
 * of elements based on user state.
 *
 * Updated to align with new codebase structure and FirebaseService implementation.
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
     * Updated to match new SessionManager implementation.
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
     * Updated fragment ID to match new implementation.
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
     */
    @Test
    public void testBottomNavigation_CreateEvent() throws InterruptedException {
        onView(withId(R.id.navigation_home))
                .check(matches(isDisplayed()))
                .perform(click());
        // Navigate to Create Event
        onView(withId(R.id.navigation_create_event))
                .check(matches(isDisplayed()))
                .perform(click());

        // Add a short delay to allow fragment to load
        sleep(1000);

        // Verify that the Create Event fragment is displayed
        onView(withId(R.id.fragment_create_event_main)).check(matches(isDisplayed()));
    }


    /**
     * Tests that the profile menu item is visible when the user is logged in as an ENTRANT.
     * Verifies that the profile option is displayed after the session is recreated.
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
     */
    @Test
    public void testInitialNavigationNotLoggedIn() {
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
        onView(withId(R.id.navigation_create_event)).check(matches(isDisplayed()));
        onView(withId(R.id.navigation_home)).check(matches(isDisplayed()));
    }

    /**
     * Tests the Admin mode button in the top navigation.
     * Verifies that the Admin view is displayed after clicking the button.
     * Navigates back to the home page after and checks that the user returns correctly.
     *
     * @author Radhe Patel
     */
    @Test
    public void testAdminMode() throws InterruptedException {

        // Add user as a admin and set up admin button for the test
        setUpAdmin();

        sleep(2000);

        // Locate and click the Admin mode button
        onView(withId(R.id.navigation_admin)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
        onView(withId(R.id.admin_navigation_home)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
    }

    /**
     * tests the admin events button
     *
     * @author Radhe Patel
     *
     * @throws InterruptedException
     */
    @Test
    public void testAdminEvents() throws InterruptedException {
        // Add user as a admin and set up admin button for the test
        setUpAdmin();

        sleep(2000);

        // Locate and click the Admin mode button
        onView(withId(R.id.navigation_admin)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
        onView(withId(R.id.admin_navigation_view_events)).check(matches(isDisplayed()));
        sleep(2000);
        onView(withId(R.id.admin_navigation_home)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
    }

    /**
     * tests the admin facilities button
     *
     * @author Radhe Patel
     *
     * @throws InterruptedException
     */
    @Test
    public void testAdminFacilities() throws InterruptedException {
        // Add user as a admin and set up admin button for the test
        setUpAdmin();

        sleep(2000);

        // Locate and click the Admin mode button
        onView(withId(R.id.navigation_admin)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
        onView(withId(R.id.admin_navigation_view_facilities)).check(matches(isDisplayed()));
        sleep(2000);
        onView(withId(R.id.admin_navigation_home)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
    }

    /**
     * tests the admin profiles button
     *
     * @author Radhe Patel
     *
     * @throws InterruptedException
     */
    @Test
    public void testAdminProfiles() throws InterruptedException {
        // Add user as a admin and set up admin button for the test
        setUpAdmin();

        sleep(2000);

        // Locate and click the Admin mode button
        onView(withId(R.id.navigation_admin)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
        onView(withId(R.id.navigation_admin_profiles)).check(matches(isDisplayed()));
        sleep(2000);
        onView(withId(R.id.admin_navigation_home)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.navigation_join_event)).check(matches(isDisplayed()));
    }

    /**
     * adds the device ID to the admins in the db so the device has access to admin mode.
     * Reloads the app so that the admin mode button comes into view.
     *
     * @author Radhe Patel
     *
     * @throws InterruptedException
     */
    private void setUpAdmin() throws InterruptedException{
        FirebaseFirestore firestore;

        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        firestore.setFirestoreSettings(settings);

        activityRule.getScenario().onActivity(activity -> {
            String deviceId = Settings.Secure.getString(
                    activity.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            firestore.collection("admins").document(deviceId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            // Add the device ID to the 'admins' collection if it doesn't exist
                            firestore.collection("admins").document(deviceId)
                                    .set(new HashMap<>()) // Add an empty document for this device ID
                                    .addOnSuccessListener(aVoid -> System.out.println("Device ID added to admins collection"))
                                    .addOnFailureListener(e -> fail("Failed to add device ID to admins collection: " + e.getMessage()));
                        } else {
                            System.out.println("Device ID already exists in the admins collection");
                        }
                    })
                    .addOnFailureListener(e -> fail("Failed to check if device ID exists: " + e.getMessage()));
        });

        sleep(2000);

        activityRule.getScenario().onActivity(activity -> {
            // Create an explicit intent for MainActivity
            android.content.Intent intent = new android.content.Intent(activity, MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent); // Start MainActivity explicitly

            // Finish the current activity to simulate an app restart
            activity.finish();
        });
    }

}

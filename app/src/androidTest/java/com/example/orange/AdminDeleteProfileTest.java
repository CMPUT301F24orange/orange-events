package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static java.lang.Thread.sleep;

/**
 * Intent Test for the delete profile functionality in the app.
 *
 * @author Dhairya Prajapati
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDeleteProfileTest {
    private FirebaseFirestore firestore;
    private String testUserId;
    private String testFacilityId;
    private String testEventId;

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Initializes the firebase and creates the mock user, along with
     * the mock facility and event and connects it to the user.
     *
     * @author Dhairya Prajapati
     */
    @Before
    public void setUp() {
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        firestore.setFirestoreSettings(settings);



        // Create a test facility in Firestore
        Facility testFacility = new Facility();
        testFacility.setName("Test Facility Delete");
        testFacilityId = firestore.collection("facilities").document().getId(); // Generate a test facility ID
        testFacility.setId(testFacilityId);
        firestore.collection("facilities").document(testFacilityId).set(testFacility);

        // Create a test organizer profile
        User testUser = new User();
        testUser.setUsername("Test Organizer Delete");
        testUser.setEmail("testorganizer@example.com");
        testUser.setPhone("123-456-7890");
        testUser.setUserType(UserType.ORGANIZER); // Set user type as ORGANIZER
        testUserId = firestore.collection("users").document().getId(); // Generate a test user ID
        testUser.setId(testUserId);
        testUser.setFacilityId(testFacilityId); // Link user to facility
        firestore.collection("users").document(testUserId).set(testUser);

        // Create a test event related to the test facility
        Event testEvent = new Event();
        testEvent.setTitle("Test Event Delete");
        String testEventId = firestore.collection("events").document().getId(); // Generate a test event ID
        testEvent.setId(testEventId);
        testEvent.setFacilityId(testFacilityId); // Link event to facility
        firestore.collection("events").document(testEventId).set(testEvent);
    }

    /**
     * Test Navigates to the admin profile list and deletes the test user created
     * in the setup. It then checks if the facility and event associated with the user
     * have been removed as well.
     *
     * @author Dhairya Prajapati
     */
    @Test
    public void testDeleteProfileAndRelatedFacilitiesAndEvents() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the profiles screen
        onView(withId(R.id.navigation_admin_profiles)).perform(click());
        sleep(2000);

        // Delete the test organizer profile
        onView(allOf(withId(R.id.profile_delete_button), hasSibling(withText("Test Organizer Delete")))).perform(scrollTo(), click());
        sleep(2000);

        // Verify that the organizer profile no longer exists in the list
        onView(withText("Test Organizer Delete")).check(doesNotExist());

        // Navigate to the facilities screen
        onView(withId(R.id.admin_navigation_view_facilities)).perform(click());
        sleep(2000);

        // Verify that the facility associated with the deleted profile no longer exists
        onView(withText("Test Facility Delete")).check(doesNotExist());

        // Navigate to the events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Verify that the event associated with the deleted organizer no longer exists
        onView(withText("Test Event Delete")).check(doesNotExist());
    }
}

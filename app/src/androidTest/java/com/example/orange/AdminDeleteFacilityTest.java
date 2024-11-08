package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
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
 * Intent Test for the delete facility functionality in the app.
 * @author Radhe Patel
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDeleteFacilityTest {
    private FirebaseFirestore firestore;
    private String testFacilityId;

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);


    /**
     * Initializes the firebase and creates the mock facility that is to be deleted
     * @author Radhe Patel
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

        // Create a test event related to the test facility
        Event testEvent = new Event();
        testEvent.setTitle("Test Event Delete");
        String testEventId = firestore.collection("events").document().getId(); // Generate a test event ID
        testEvent.setId(testEventId);
        testEvent.setFacilityId(testFacilityId); // Link event to facility
        firestore.collection("events").document(testEventId).set(testEvent);
    }

    /**
     * Test Navigates to the admin facility list and deletes the test facility created in the setup.
     * It then checks if the facility has been removed from the list.
     *
     * @author Radhe Patel
     */
    @Test
    public void testDeleteFacilityAndRelatedEvents() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the facilities screen
        onView(withId(R.id.admin_navigation_view_facilities)).perform(click());
        sleep(2000);

        // Delete the test facility
        onView(allOf(withId(R.id.facility_remove_button), hasSibling(withText("Test Facility Delete")))).perform(scrollTo(), click());
        sleep(2000);

        // Verify that the facility no longer exists in the list
        onView(withText("Test Facility Delete")).check(doesNotExist());

        // Navigate to the events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Verify that any events associated with the deleted facility no longer exist
        onView(withText("Test Event Delete")).check(doesNotExist());
    }

}
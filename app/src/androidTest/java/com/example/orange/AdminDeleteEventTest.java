package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.ui.admin.AdminEventListFragment;
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
 * Intent Test for the Delete Event Functionality in the app.
 *
 * @author Radhe Patel
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDeleteEventTest {
    private FirebaseFirestore firestore;
    private String testEventId;

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);


    /**
     * Initializes the firebase and creates the mock event that is to be deleted
     *
     * @author Radhe Patel
     */
    @Before
    public void setUp() {
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        firestore.setFirestoreSettings(settings);

        // Add a test event to Firestore
        Event testEvent = new Event();
        testEvent.setTitle("Test Event Delete");
        testEventId = firestore.collection("events").document().getId(); // Generating a test ID
        testEvent.setId(testEventId);
        firestore.collection("events").document(testEventId).set(testEvent);
    }


    /**
     * Test Navigates to the admin event list and deletes the test event created
     * in the setup. It then checks if the event has been removed from the list.
     *
     * @author Radhe Patel
     */
    @Test
    public void testCreateAndDeleteEvent() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the admin view events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Find the "Delete" button within the same CardView as the "Test Event Delete" text
        onView(allOf(withId(R.id.delete_button), hasSibling(withText("Test Event Delete")))).perform(scrollTo(), click());
        sleep(2000);

        // Verify that "Test Event Delete" no longer exists in the list
        onView(withText("Test Event Delete")).check(doesNotExist());
    }


}
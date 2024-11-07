package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
 *
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
     *
     * @author Radhe Patel
     */
    @Before
    public void setUp() {
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        firestore.setFirestoreSettings(settings);

        // Add a test event to Firestore
        Facility testFacility = new Facility();
        testFacility.setName("Test Facility Delete");
        testFacilityId = firestore.collection("facilities").document().getId(); // Generating a test ID
        testFacility.setId(testFacilityId);
        firestore.collection("facilities").document(testFacilityId).set(testFacility);
    }


    /**
     * Test Navigates to the admin facility list and deletes the test facility created
     * in the setup. It then checks if the facility has been removed from the list.
     *
     * @author Radhe Patel
     */
    @Test
    public void testDeleteFacility() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the admin view events screen
        onView(withId(R.id.admin_navigation_view_facilities)).perform(click());
        sleep(2000);

        // Find the "Delete" button within the same CardView as the "Test Event Delete" text
        onView(allOf(withId(R.id.facility_remove_button), hasSibling(withText("Test Facility Delete")))).perform(scrollTo(), click());
        sleep(2000);

        // Verify that "Test Event Delete" no longer exists in the list
        onView(withText("Test Facility Delete")).check(doesNotExist());
    }


}
package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.fail;
import static java.lang.Thread.sleep;

import android.provider.Settings;

import java.util.HashMap;

/**
 * Intent Test for the delete facility functionality in the app.
 * Updated to align with new FirebaseService methods and UI IDs.
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
     * Initializes Firebase and creates a mock facility to be deleted.
     * Also creates a related event linked to the facility.
     *
     * Updated to match new FirebaseService and Firestore implementations.
     *
     * @author Radhe Patel
     */
    @Before
    public void setUp() throws InterruptedException {
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        firestore.setFirestoreSettings(settings);

        // Get the device ID of the current user and add it to the admins in the Firestore
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


        sleep(3000);

        // Delete all existing facilities
        firestore.collection("facilities").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    firestore.collection("facilities").document(document.getId()).delete()
                            .addOnFailureListener(e -> System.err.println("Failed to delete document: " + e.getMessage()));
                }
            } else {
                System.err.println("Failed to fetch facilities: " + task.getException());
            }
        });

        sleep(3000);

        // Delete all existing events
        firestore.collection("events").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    firestore.collection("events").document(document.getId()).delete()
                            .addOnFailureListener(e -> System.err.println("Failed to delete document: " + e.getMessage()));
                }
            } else {
                System.err.println("Failed to fetch events: " + task.getException());
            }
        });

        sleep(3000);

        // Create a test facility in Firestore
        Facility testFacility = new Facility();
        testFacility.setName("Test Facility Delete");
        testFacilityId = firestore.collection("facilities").document().getId(); // Generate a test facility ID
        testFacility.setId(testFacilityId);
        firestore.collection("facilities").document(testFacilityId).set(testFacility);

        sleep(3000);

        // Create a test event related to the test facility
        Event testEvent = new Event();
        testEvent.setTitle("Test Event Delete");
        String testEventId = firestore.collection("events").document().getId(); // Generate a test event ID
        testEvent.setId(testEventId);
        testEvent.setFacilityId(testFacilityId); // Link event to facility
        firestore.collection("events").document(testEventId).set(testEvent);

        sleep(3000);

        // Relaunch the app to load admin button
        relaunchApp();

        sleep(2000);
    }

    /**
     * Tests deleting a facility and verifies that related events are also deleted.
     *
     * Updated to match new FirebaseService method signatures.
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
        onView(withId(R.id.facility_remove_button)).perform(scrollTo(), click());
        sleep(2000);

        // Verify that the facility no longer exists in the list
        onView(withText("Test Facility Delete")).check(doesNotExist());

        // Navigate to the events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Verify that any events associated with the deleted facility no longer exist
        onView(withText("Test Event Delete")).check(doesNotExist());
    }

    /**
     * When the device ID is added to the admins collection in the db the app
     * needs to be relaunched to load the button.
     *
     * @author Radhe Patel
     */
    private void relaunchApp() {
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

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static java.lang.Thread.sleep;

import android.provider.Settings;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Intent Test for the delete event functionality in the app.
 * Updated to align with new FirebaseService methods and UI IDs.
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
     * Initializes Firebase and creates a mock event to be deleted.
     *
     * Updated to match new FirebaseService and Firestore implementations.
     *
     * @author Radhe Patel, Graham
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

        // Add a test event to Firestore
        Event testEvent = new Event();
        testEvent.setTitle("Test Event Delete");
        testEventId = firestore.collection("events").document().getId(); // Generating a test ID
        testEvent.setId(testEventId);
        testEvent.setEventImageId("123");
        testEvent.setQr_hash("123");
        assertNotNull("eventImageId should not be null before deletion", testEvent.getEventImageId());
        assertNotNull("qr_hash should not be null before deletion", testEvent.getQr_hash());
        firestore.collection("events").document(testEventId).set(testEvent);

        sleep(3000);

        // Relaunch the app to load admin button
        relaunchApp();

        sleep(2000);
    }

    /**
     * Tests deleting an event image and verifies it has been set to null in Firestore.
     *
     * Updated to match new FirebaseService method signatures.
     *
     * @author Radhe Patel, Graham
     */
    @Test
    public void testDeleteEventImage() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the admin view events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Find the "Delete Poster" button and click it
        onView(withId(R.id.poster_delete_button)).perform(click());
        sleep(2000);

        // Verify that the eventImageId is null in the db
        CountDownLatch latch = new CountDownLatch(1);
        firestore.collection("events").document(testEventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("eventImageId")) {
                    String eventImageId = documentSnapshot.getString("eventImageId");
                    assertNull("eventImageId should be null after deletion", eventImageId);
                } else {
                    fail("The document exists but does not contain the 'eventImageId' field.");
                }
            } else {
                fail("The document does not exist in Firestore.");
            }
            latch.countDown();
        }).addOnFailureListener(e -> {
            fail("Failed to retrieve the document from Firestore: " + e.getMessage());
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
    }


    /**
     * Tests deleting an event and verifies it no longer exists in the UI and Firestore.
     *
     * Updated to match new FirebaseService method signatures.
     *
     * @author Radhe Patel, Graham
     */
    @Test
    public void testDeleteEvent() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the admin view events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Find the "Delete" button and click it
        onView(withId(R.id.delete_button)).perform(click());
        sleep(2000);

        // Verify that "Test Event Delete" no longer exists in the list
        onView(withText("Test Event Delete")).check(doesNotExist());
    }

    /**
     * Tests deleting an event's QR hash and verifies it has been set to null in Firestore.
     *
     * Updated to match new FirebaseService method signatures.
     *
     * @author Radhe Patel, Graham
     */
    @Test
    public void testDeleteEventHash() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the admin view events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Find the "Delete QR" button within the same CardView as the "Test Event Delete" text
        onView(withId(R.id.qr_delete_button)).perform(click());
        sleep(2000);

        // Verify that the qr_hash is null in the db
        CountDownLatch latch = new CountDownLatch(1);
        firestore.collection("events").document(testEventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("qr_hash")) {
                    String qr_hash = documentSnapshot.getString("qr_hash");
                    assertNull("qr_hash should be null after deletion", qr_hash);
                } else {
                    fail("The document exists but does not contain the 'qr_hash' field.");
                }
            } else {
                fail("The document does not exist in Firestore.");
            }
            latch.countDown();
        }).addOnFailureListener(e -> {
            fail("Failed to retrieve the document from Firestore: " + e.getMessage());
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
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
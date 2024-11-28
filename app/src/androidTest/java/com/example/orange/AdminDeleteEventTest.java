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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Intent Test for the delete event functionality in the app.
 * Updated to align with new FirebaseService methods and UI IDs.
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
     */
    @Before
    public void setUp() throws InterruptedException {
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        firestore.setFirestoreSettings(settings);

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
    }

    /**
     * Tests deleting an event image and verifies it has been set to null in Firestore.
     *
     * Updated to match new FirebaseService method signatures.
     */
    @Test
    public void testDeleteEventImage() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the admin view events screen
        onView(withId(R.id.admin_navigation_view_events)).perform(click());
        sleep(2000);

        // Find the "Delete Poster" button within the same CardView as the "Test Event Delete" text
        onView(allOf(withId(R.id.poster_delete_button), hasSibling(withText("Test Event Delete")))).perform(scrollTo(), click());
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
     */
    @Test
    public void testDeleteEvent() throws InterruptedException {
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

    /**
     * Tests deleting an event's QR hash and verifies it has been set to null in Firestore.
     *
     * Updated to match new FirebaseService method signatures.
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
        onView(allOf(withId(R.id.qr_delete_button), hasSibling(withText("Test Event Delete")))).perform(scrollTo(), click());
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

}

package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static java.lang.Thread.sleep;

import java.util.Objects;

/**
 * Intent Test for the delete event functionality in the app.
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

    @After
    public void tearDown() {
        // Delete the test event from Firestore
        firestore.collection("events").document(testEventId).delete()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Test event successfully deleted from Firestore.");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Failed to delete test event from Firestore: " + e.getMessage());
                });
    }



    /**
     * Tests db functionality. Creates a test event with an eventImageId set. It then checks
     * that the delete poster button in fact deletes the eventImageId from the db, i.e
     * sets it to null.
     *
     * @author Radhe Patel
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
        }).addOnFailureListener(e -> {
            fail("Failed to retrieve the document from Firestore: " + e.getMessage());
        });
        sleep(2000);
    }


    /**
     * Test Navigates to the admin event list and deletes the test event created
     * in the setup. It then checks if the event has been removed from the list.
     *
     * @author Radhe Patel
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
     * Tests db functionality. Creates a test event with qr_hash set. It then checks
     * that the delete poster button in fact deletes the qr_hash from the db, i.e
     * sets it to null.
     *
     * @author Radhe Patel
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
        }).addOnFailureListener(e -> {
            fail("Failed to retrieve the document from Firestore: " + e.getMessage());
        });
        sleep(2000);
    }

}
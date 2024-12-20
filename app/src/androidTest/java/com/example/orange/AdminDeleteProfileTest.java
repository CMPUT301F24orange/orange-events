package com.example.orange;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
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
 * Intent Test for the delete profile functionality in the app.
 * Updated to align with new FirebaseService methods and UI IDs.
 *
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
     * Initializes Firebase and creates a mock user, along with
     * the mock facility and event, and links them together.
     *
     * Updated to match new FirebaseService and Firestore implementations.
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

        // Delete all existing users
        firestore.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    firestore.collection("users").document(document.getId()).delete()
                            .addOnFailureListener(e -> System.err.println("Failed to delete document: " + e.getMessage()));
                }
            } else {
                System.err.println("Failed to fetch users: " + task.getException());
            }
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

        // Create a test organizer profile
        User testUser = new User();
        testUser.setUsername("Test Organizer Delete");
        testUser.setEmail("testorganizer@example.com");
        testUser.setPhone("123-456-7890");
        testUser.setUserType(UserType.ORGANIZER); // Set user type as ORGANIZER
        testUserId = firestore.collection("users").document().getId(); // Generate a test user ID
        testUser.setId(testUserId);
        testUser.setFacilityId(testFacilityId); // Link user to facility
        testUser.setProfileImageId("123");
        assertNotNull("profileImageId should not be null before deletion", testUser.getProfileImageId());
        firestore.collection("users").document(testUserId).set(testUser);

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
     * Tests deleting a user profile and verifies that related facilities and events are also deleted.
     *
     * Updated to match new FirebaseService method signatures.
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
        onView(withId(R.id.profile_delete_button)).perform(click());
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

    /**
     * Tests deleting a user's profile picture and verifies that the image reference is null in Firestore.
     *
     * Updated to match new FirebaseService method signatures.
     */
    @Test
    public void testDeleteProfilePicture() throws InterruptedException {
        // Navigate to the admin view
        onView(withId(R.id.navigation_admin)).perform(click());
        sleep(2000);

        // Navigate to the profiles screen
        onView(withId(R.id.navigation_admin_profiles)).perform(click());
        sleep(2000);

        // Delete the test profile image
        onView(withId(R.id.profile_pic_delete_button)).perform(click());
        sleep(2000);

        // Verify that the profileImageId is null in the db
        CountDownLatch latch = new CountDownLatch(1);
        firestore.collection("users").document(testUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("profileImageId")) {
                    String profileImageId = documentSnapshot.getString("profileImageId");
                    assertNull("profileImageId should be null after deletion", profileImageId);
                } else {
                    fail("The document exists but does not contain the 'profileImageId' field.");
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

        // Delete the test organizer profile
        onView(withId(R.id.profile_delete_button)).perform(click());
        sleep(2000);
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

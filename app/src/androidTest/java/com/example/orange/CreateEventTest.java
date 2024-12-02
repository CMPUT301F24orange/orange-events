package com.example.orange;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented test for creating an event.
 * Updated to match new FirebaseService implementations and UI IDs.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
    private SessionManager sessionManager;
    private String createdEventId; // To store the ID of the created event
    private String createdUserId = "testDeviceId_ORGANIZER"; // Use a consistent ID format
    private String createdFacilityId;

    private FirebaseService firebaseService;

    @Before
    public void setup() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.createLoginSession("testDeviceId", UserType.ORGANIZER, "testDeviceId");

        firebaseService = new FirebaseService();
        String facilityName = "Test Facility";
        String facilityAddress = "123 Test St";
        Facility facility = new Facility(facilityName, facilityAddress);

        CountDownLatch latch = new CountDownLatch(1);

        // Step 1: Create the facility in Firestore
        firebaseService.createFacility(facility, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String facilityId) {
                Log.d("Setup", "Facility created with ID: " + facilityId);
                createdFacilityId = facilityId;

                // Step 2: Create a new user explicitly
                User testUser = new User("testDeviceId", UserType.ORGANIZER);
                firebaseService.createUser(testUser, new FirebaseCallback<String>() {
                    @Override
                    public void onSuccess(String userId) {
                        Log.d("Setup", "User created with ID: " + userId);
                        // Step 3: Update the user with the facility ID
                        testUser.setFacilityId(facilityId);
                        firebaseService.updateUser(testUser, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                latch.countDown(); // Proceed after setting up
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("Setup", "Failed to update user with facility ID", e);
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("Setup", "Failed to create user", e);
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Setup", "Failed to create facility", e);
                latch.countDown();
            }
        });

        // Wait for the latch to ensure setup completion
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testFullEventCreation() throws InterruptedException {
        // Navigate to the Create Event fragment
        onView(withId(R.id.navigation_create_event))
                .check(matches(isDisplayed()))
                .perform(click());

        sleep(1000);

        // Fill in the event title
        onView(withId(R.id.titleEditText))
                .perform(replaceText("Sample Event Title"), closeSoftKeyboard());

        // Fill in the event description
        onView(withId(R.id.descriptionEditText))
                .perform(replaceText("This is a sample description for the event."), closeSoftKeyboard());

        // Fill in the start and end dates
        onView(withId(R.id.start_date_input))
                .perform(replaceText("2024/01/01"), closeSoftKeyboard());
        onView(withId(R.id.end_date_input))
                .perform(replaceText("2024/01/05"), closeSoftKeyboard());

        // Perform swipe up to bring additional fields into view
        onView(withId(R.id.fragment_create_event_main)).perform(swipeUp());

        // Fill in the event capacity
        onView(withId(R.id.capacityEditText))
                .perform(replaceText("20"), closeSoftKeyboard());

        // Fill in registration dates
        onView(withId(R.id.registration_opens_edit_text))
                .perform(replaceText("2023/12/01"), closeSoftKeyboard());
        onView(withId(R.id.registration_deadline_edit_text))
                .perform(replaceText("2023/12/15"), closeSoftKeyboard());

        // Perform another swipe up to bring the lottery day and event price fields into view
        onView(withId(R.id.fragment_create_event_main)).perform(swipeUp());

        // Fill in lottery day
        onView(withId(R.id.lottery_day_edit_text))
                .perform(replaceText("2023/12/20"), closeSoftKeyboard());

        // Fill in event price
        onView(withId(R.id.event_price_edit_text))
                .perform(replaceText("50.00"), closeSoftKeyboard());

        // Perform a final swipe up to bring the waitlist fields into view
        onView(withId(R.id.fragment_create_event_main)).perform(swipeUp());

        // Check the waitlist checkbox and fill in the waitlist limit
        sleep(2000);
        onView(withId(R.id.waitlist_limit_checkbox))
                .perform(click());
        sleep(2000);
        onView(withId(R.id.waitlist_limit_edit_text))
                .perform(replaceText("10"), closeSoftKeyboard());

        // Submit the form and save the event ID for cleanup
        onView(withId(R.id.createEventButton))
                .perform(click());

        // Wait briefly for navigation to complete
        sleep(2000);

        // Verify that the newly created event appears in the "View My Events" list
        onView(withText("Sample Event Title"))
                .check(matches(isDisplayed()));

        // After verification, fetch the event ID for cleanup
        CountDownLatch latch = new CountDownLatch(1);
        firebaseService.getAllEvents(new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                for (Event event : events) {
                    if ("Sample Event Title".equals(event.getTitle())) {
                        createdEventId = event.getId(); // Save the ID for deletion
                        break;
                    }
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("CreateEventTest", "Failed to fetch events", e);
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
    }

    @After
    public void cleanup() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        // Step 1: Delete the event
        if (createdEventId != null) {
            firebaseService.deleteEvent(createdEventId, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d("Cleanup", "Event deleted successfully");
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("Cleanup", "Failed to delete event", e);
                    latch.countDown();
                }
            });
        } else {
            latch.countDown();
        }

        // Step 2: Delete the user
        firebaseService.deleteUserAndRelatedFacilities(createdUserId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("Cleanup", "User deleted successfully");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Cleanup", "Failed to delete user", e);
                latch.countDown();
            }
        });

        // Step 3: Delete the facility
        if (createdFacilityId != null) {
            firebaseService.deleteFacility(createdFacilityId, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d("Cleanup", "Facility deleted successfully");
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("Cleanup", "Failed to delete facility", e);
                    latch.countDown();
                }
            });
        } else {
            latch.countDown();
        }

        // Wait for all cleanup tasks to complete
        latch.await(10, TimeUnit.SECONDS);
    }
}

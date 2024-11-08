package com.example.orange;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.LargeTest;

import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.lang.Thread.sleep;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
    private SessionManager sessionManager;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.createLoginSession("testDeviceId", UserType.ORGANIZER, "testDeviceId");
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
                .perform(replaceText("Sample Event Title"), ViewActions.closeSoftKeyboard());

        // Fill in the event description
        onView(withId(R.id.descriptionEditText))
                .perform(replaceText("This is a sample description for the event."), ViewActions.closeSoftKeyboard());

        // TO-DO: Add an event poster image

        // Fill in the start and end dates
        onView(withId(R.id.start_date_input))
                .perform(replaceText("2024/01/01"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.end_date_input))
                .perform(replaceText("2024/01/05"), ViewActions.closeSoftKeyboard());

        // Perform swipe up to bring additional fields into view
        onView(withId(R.id.fragment_create_event_main)).perform(swipeUp());

        // Fill in the event capacity
        onView(withId(R.id.capacityEditText))
                .perform(replaceText("20"), ViewActions.closeSoftKeyboard());

        // Fill in registration dates
        onView(withId(R.id.registration_opens_edit_text))
                .perform(replaceText("2023/12/01"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.registration_deadline_edit_text))
                .perform(replaceText("2023/12/15"), ViewActions.closeSoftKeyboard());

        // Perform another swipe up to bring the lottery day and event price fields into view
        onView(withId(R.id.fragment_create_event_main)).perform(swipeUp());

        // Fill in lottery day
        onView(withId(R.id.lottery_day_edit_text))
                .perform(replaceText("2023/12/20"), ViewActions.closeSoftKeyboard());

        // Fill in event price
        onView(withId(R.id.event_price_edit_text))
                .perform(replaceText("50.00"), ViewActions.closeSoftKeyboard());

        // Perform a final swipe up to bring the waitlist fields into view
        onView(withId(R.id.fragment_create_event_main)).perform(swipeUp());

        // Check the waitlist checkbox and fill in the waitlist limit
        onView(withId(R.id.waitlist_limit_checkbox))
                .perform(click());
        onView(withId(R.id.waitlist_limit_edit_text))
                .perform(replaceText("10"), ViewActions.closeSoftKeyboard());

        // Submit the form
        onView(withId(R.id.createEventButton))
                .perform(click());

        // Wait briefly for navigation to complete
        sleep(2000);

        // Navigate explicitly to "View My Events"
        onView(withId(R.id.navigation_view_my_events))
                .perform(click());

        // Wait briefly to load the events
        sleep(1000);

        // Verify that the newly created event appears in the "View My Events" list
        onView(withText("Sample Event Title"))
                .check(matches(isDisplayed()));
    }
}

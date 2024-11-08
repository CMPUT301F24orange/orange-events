package com.example.orange;


import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.ui.join.JoinEventFragment;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.utils.SessionManager;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.UserSession;
import com.example.orange.databinding.FragmentJoinEventBinding;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

/**
 * Lists the events the user can join. Tesing joining the event
 * and testing the leave waitlist event
 *
 * @author Viral Bhavsar
 */
public class JoinEventFragmentTest {

    @Mock private FirebaseService firebaseService;
    @Mock private SessionManager sessionManager;
    @Mock private UserSession mockUserSession;
    @Mock private Context mockContext;
    @Mock private LayoutInflater mockInflater;

    private JoinEventFragment joinEventFragment;

    /**
     * Sets up the mocks for the fragment
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);

        // Initialize the fragment
        joinEventFragment = new JoinEventFragment();

        // Use reflection to inject the mock FirebaseService into the fragment
        Field firebaseServiceField = JoinEventFragment.class.getDeclaredField("firebaseService");
        firebaseServiceField.setAccessible(true);
        firebaseServiceField.set(joinEventFragment, firebaseService);

        // Use reflection to inject the mock SessionManager into the fragment
        Field sessionManagerField = JoinEventFragment.class.getDeclaredField("sessionManager");
        sessionManagerField.setAccessible(true);
        sessionManagerField.set(joinEventFragment, sessionManager);

        // Set up a mock UserSession with a fake userId
        when(sessionManager.getUserSession()).thenReturn(mockUserSession);
        when(mockUserSession.getUserId()).thenReturn("user123");

        // Mock context if needed (e.g., for Toast messages or dialog interactions)
        when(mockContext.getString(anyInt())).thenReturn("Test String");
    }

    /**
     * Tests that the join event method adds the user to the waitlist
     *
     * @author Viral Bhavsar
     */
    @Test
    public void testJoinEvent_NoGeolocationRequired() {
        // Mock an event without geolocation requirement
        Event eventWithoutGeolocation = mock(Event.class);
        when(eventWithoutGeolocation.getId()).thenReturn("event2");
        when(eventWithoutGeolocation.getGeolocationEvent()).thenReturn(false);

        // Proceed to join the event (should add to waitlist directly)
        joinEventFragment.joinEvent(eventWithoutGeolocation);

        // Verify that addToEventWaitlist is called with the correct arguments
        verify(firebaseService).addToEventWaitlist(eq(eventWithoutGeolocation.getId()), eq("user123"), any());
    }

    /**
     * Tests the leaveWaitlist method and removes the user from the waitlist
     *
     * @author Viral Bhavsar
     */
    @Test
    public void testLeaveWaitlist() {
        // Mock an event that the user is on the waitlist for
        Event eventOnWaitlist = mock(Event.class);
        when(eventOnWaitlist.getId()).thenReturn("event3");
        when(eventOnWaitlist.getWaitingList()).thenReturn(Arrays.asList("user123"));

        // Call leaveWaitlist
        joinEventFragment.leaveWaitlist(eventOnWaitlist);

        // Verify that the removeFromEventWaitlist method is called with correct arguments
        verify(firebaseService).removeFromEventWaitlist(eq(eventOnWaitlist.getId()), eq("user123"), any());
    }
}


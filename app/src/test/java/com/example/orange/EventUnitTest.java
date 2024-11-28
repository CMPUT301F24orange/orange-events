package com.example.orange;

import com.example.orange.data.model.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

import android.content.Context;

public class EventUnitTest {

    private Event event;
    Context context;
    @Before
    public void setUp() {
        // Create an event with capacity N
        int capacity = 5;
        event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setTitle("Test Event");
        event.setCapacity(capacity);

        System.out.println("Event setup complete.");
        System.out.println("Event capacity: " + event.getCapacity());
    }

    @Test
    public void testEventParticipantSelection() {
        // Add M users to the waiting list (M > capacity)
        int numUsers = 10; // M > N
        System.out.println("\nAdding users to the waiting list:");
        for (int i = 0; i < numUsers; i++) {
            String userId = "user" + i;
            event.addToWaitingList(userId);
            System.out.println("Added " + userId + " to waiting list.");
        }

        // Print waiting list after adding users
        System.out.println("\nWaiting List after adding users: " + event.getWaitingList());

        // Select participants from the waiting list
        event.selectParticipantsFromWaitingList(event.getCapacity(),context.getApplicationContext());

        // Print selected participants after selection
        System.out.println("\nSelected Participants after initial selection: " + event.getSelectedParticipants());

        // Verify that selectedParticipants has 'capacity' number of users
        assertEquals(event.getCapacity().intValue(), event.getSelectedParticipants().size());

        // Simulate a user accepting the invitation
        String acceptingUserId = event.getSelectedParticipants().get(0);
        event.acceptInvitation(acceptingUserId);
        System.out.println("\nUser " + acceptingUserId + " accepted the invitation.");

        // Print participants and selected participants after acceptance
        System.out.println("Participants after acceptance: " + event.getParticipants());
        System.out.println("Selected Participants after acceptance: " + event.getSelectedParticipants());

        // Verify that the user is in participants list
        assertTrue(event.getParticipants().contains(acceptingUserId));

        // Verify that selectedParticipants has one less user
        assertEquals(event.getCapacity() - 1, event.getSelectedParticipants().size());

        // Simulate a user declining the invitation
        String decliningUserId = event.getSelectedParticipants().get(0);
        event.declineInvitation(decliningUserId);
        System.out.println("\nUser " + decliningUserId + " declined the invitation.");

        // Print cancelled list, waiting list, and selected participants after decline
        System.out.println("Cancelled List after decline: " + event.getCancelledList());
        System.out.println("Waiting List after decline: " + event.getWaitingList());
        System.out.println("Selected Participants after decline: " + event.getSelectedParticipants());

        // Verify that the user is in cancelledList
        assertTrue(event.getCancelledList().contains(decliningUserId));

        // Verify that selectedParticipants has one less user
        assertEquals(event.getCapacity() - 2, event.getSelectedParticipants().size());

        // Fill vacant spots
        event.fillSpotsFromWaitingList(context);
        System.out.println("\nFilled vacant spots from waiting list.");

        // Print selected participants after filling spots
        System.out.println("Selected Participants after filling spots: " + event.getSelectedParticipants());

        // Verify that selectedParticipants is back to capacity - participants size
        int expectedSelectedParticipants = event.getCapacity() - event.getParticipants().size();
        assertEquals(expectedSelectedParticipants, event.getSelectedParticipants().size());

        // Final state of all lists
        System.out.println("\nFinal State:");
        System.out.println("Participants: " + event.getParticipants());
        System.out.println("Selected Participants: " + event.getSelectedParticipants());
        System.out.println("Cancelled List: " + event.getCancelledList());
        System.out.println("Waiting List: " + event.getWaitingList());
    }
}

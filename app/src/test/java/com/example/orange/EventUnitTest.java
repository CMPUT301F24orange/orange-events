package com.example.orange;

import com.example.orange.data.model.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for the Event class, ensuring proper handling of participants and waitlist management.
 */
public class EventUnitTest {

    private Event event;
    private static final int CAPACITY = 5;
    private static final int NUM_USERS = 10; // M > N

    @Before
    public void setUp() {
        // Initialize an Event instance with a specific capacity
        event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setTitle("Test Event");
        event.setCapacity(CAPACITY);
    }

    /**
     * Tests the process of selecting participants from the waiting list.
     * Adds more users than capacity and ensures correct selection.
     */
    @Test
    public void testParticipantSelectionFromWaitingList() {
        // Add M users to the waiting list (M > capacity)
        for (int i = 0; i < NUM_USERS; i++) {
            String userId = "user" + i;
            event.addToWaitingList(userId);
        }

        // Verify waiting list size
        assertEquals("Waiting list should have " + NUM_USERS + " users", NUM_USERS, event.getWaitingList().size());

        // Select participants from the waiting list
        event.selectParticipantsFromWaitingList(CAPACITY);

        // Verify that selectedParticipants has 'capacity' number of users
        assertEquals("Selected participants should be equal to capacity", CAPACITY, event.getSelectedParticipants().size());

        // Verify that all selected participants are part of the waiting list
        for (String selectedUser : event.getSelectedParticipants()) {
            assertTrue("Selected participant " + selectedUser + " should be in the waiting list",
                    event.getWaitingList().contains(selectedUser));
        }
    }

    /**
     * Tests accepting an invitation.
     */
    @Test
    public void testAcceptInvitation() {
        String userId = "user1";
        event.addToWaitingList(userId);
        event.selectParticipantsFromWaitingList(1);

        // Get the first selected participant
        String selectedUserId = event.getSelectedParticipants().get(0);

        event.acceptInvitation(selectedUserId);

        assertTrue("Participants should contain " + selectedUserId, event.getParticipants().contains(selectedUserId));
        assertFalse("Selected participants should not contain " + selectedUserId + " after acceptance",
                event.getSelectedParticipants().contains(selectedUserId));
        assertEquals("Selected participants size should decrease by 1", CAPACITY - 1, event.getSelectedParticipants().size());
    }

    /**
     * Tests declining an invitation.
     */
    @Test
    public void testDeclineInvitation() {
        String userId = "user2";
        event.addToWaitingList(userId);
        event.selectParticipantsFromWaitingList(1);

        // Get the first selected participant
        String selectedUserId = event.getSelectedParticipants().get(0);

        event.declineInvitation(selectedUserId);

        assertTrue("Cancelled list should contain " + selectedUserId, event.getCancelledList().contains(selectedUserId));
        assertFalse("Selected participants should not contain " + selectedUserId + " after decline",
                event.getSelectedParticipants().contains(selectedUserId));
        assertEquals("Selected participants size should decrease by 1", CAPACITY - 1, event.getSelectedParticipants().size());
    }

    /**
     * Tests filling vacant spots from the waiting list after some participants have accepted or declined.
     */
    @Test
    public void testFillVacantSpotsFromWaitingList() {
        // Add users to waiting list
        for (int i = 0; i < CAPACITY + 2; i++) { // Add more than capacity
            String userId = "user" + i;
            event.addToWaitingList(userId);
        }

        // Select participants
        event.selectParticipantsFromWaitingList(CAPACITY);

        // Accept first participant
        String acceptingUserId = event.getSelectedParticipants().get(0);
        event.acceptInvitation(acceptingUserId);

        // Decline second participant
        String decliningUserId = event.getSelectedParticipants().get(0);
        event.declineInvitation(decliningUserId);

        // Now, selectedParticipants should have CAPACITY - 2
        assertEquals("Selected participants should have " + (CAPACITY - 2) + " users", CAPACITY - 2, event.getSelectedParticipants().size());

        // Fill vacant spots
        event.fillSpotsFromWaitingList();

        // Calculate expected number of selected participants
        int expectedSelectedParticipants = CAPACITY - event.getParticipants().size();

        // Verify that selectedParticipants is back to capacity - participants size
        assertEquals("Selected participants should be back to capacity - participants size",
                expectedSelectedParticipants, event.getSelectedParticipants().size());

        // Verify that the newly selected participants are from the waiting list
        for (String selectedUser : event.getSelectedParticipants()) {
            assertTrue("Selected participant " + selectedUser + " should be in the waiting list",
                    event.getWaitingList().contains(selectedUser));
        }
    }

    /**
     * Tests adding duplicate users to the waiting list.
     * Ensures that duplicates are not added.
     */
    @Test
    public void testAddDuplicateUsersToWaitingList() {
        String userId = "user1";
        event.addToWaitingList(userId);
        event.addToWaitingList(userId); // Attempt to add duplicate

        // Assuming the implementation prevents duplicates
        assertEquals("Waiting list should contain only one instance of " + userId, 1, event.getWaitingList().size());
    }

    /**
     * Tests selecting participants when waiting list has fewer users than capacity.
     */
    @Test
    public void testSelectParticipantsWithInsufficientWaitingList() {
        // Add fewer users than capacity
        for (int i = 0; i < CAPACITY - 2; i++) {
            String userId = "user" + i;
            event.addToWaitingList(userId);
        }

        event.selectParticipantsFromWaitingList(CAPACITY);

        // Selected participants should have only available users
        assertEquals("Selected participants should match available users", CAPACITY - 2, event.getSelectedParticipants().size());

        for (int i = 0; i < CAPACITY - 2; i++) {
            String userId = "user" + i;
            assertTrue("Selected participants should contain " + userId, event.getSelectedParticipants().contains(userId));
        }
    }
}

package com.example.orange;

import com.example.orange.data.model.Event;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

import java.util.UUID;

/**
 * Unit tests for the Event class, ensuring proper handling of participants and waitlist management.
 *
 * @author Graham Flokstra
 */
public class EventUnitTest {

    private Event event;
    private static final int CAPACITY = 5;
    private static final int NUM_USERS = 10; // M > N

    @Before
    public void setUp() {
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);

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

        // Select participants from the waiting list without notifications
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
        assertEquals("Selected participants size should decrease by 1", 0, event.getSelectedParticipants().size());
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
        assertEquals("Selected participants size should decrease by 1", 0, event.getSelectedParticipants().size());
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

        // Select participants without notifications
        event.selectParticipantsFromWaitingList(CAPACITY);

        // Accept first participant
        String acceptingUserId = event.getSelectedParticipants().get(0);
        event.acceptInvitation(acceptingUserId);

        // Decline second participant
        String decliningUserId = event.getSelectedParticipants().get(0);
        event.declineInvitation(decliningUserId);

        // Now, selectedParticipants should have CAPACITY - 2
        assertEquals("Selected participants should have " + (CAPACITY - 2) + " users",
                CAPACITY - 2, event.getSelectedParticipants().size());

        // Fill vacant spots without notifications
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

    /**
     * Tests that participants are correctly removed from the waiting list upon acceptance.
     */
    @Test
    public void testAcceptInvitationRemovesFromSelectedAndAddsToParticipants() {
        String userId = "user3";
        event.addToWaitingList(userId);
        event.selectParticipantsFromWaitingList(1);

        // Ensure the user is in selectedParticipants
        assertTrue(event.getSelectedParticipants().contains(userId));

        // Accept the invitation
        event.acceptInvitation(userId);

        // Verify the user is moved to participants
        assertTrue(event.getParticipants().contains(userId));

        // Verify the user is removed from selectedParticipants
        assertFalse(event.getSelectedParticipants().contains(userId));

        // Verify the waiting list still contains the user
        assertTrue(event.getWaitingList().contains(userId));
    }

    /**
     * Tests that declining an invitation moves the user to the cancelled list.
     */
    @Test
    public void testDeclineInvitationMovesToCancelledList() {
        String userId = "user4";
        event.addToWaitingList(userId);
        event.selectParticipantsFromWaitingList(1);

        // Ensure the user is in selectedParticipants
        assertTrue(event.getSelectedParticipants().contains(userId));

        // Decline the invitation
        event.declineInvitation(userId);

        // Verify the user is added to the cancelled list
        assertTrue(event.getCancelledList().contains(userId));

        // Verify the user is removed from selectedParticipants
        assertFalse(event.getSelectedParticipants().contains(userId));

        // Verify the user is removed from the waiting list
        assertFalse(event.getWaitingList().contains(userId));
    }

    /**
     * Tests that the event is not marked as full when capacity is not reached.
     */
    @Test
    public void testIsFullReturnsFalseWhenNotFull() {
        // Initially, no participants
        assertFalse("Event should not be full initially", event.isFull());

        // Add participants less than capacity
        for (int i = 0; i < CAPACITY - 1; i++) {
            String userId = "user" + i;
            event.addParticipant(userId);
        }

        assertFalse("Event should not be full when participants are less than capacity", event.isFull());
    }

    /**
     * Tests that the event is marked as full when capacity is reached.
     */
    @Test
    public void testIsFullReturnsTrueWhenFull() {
        // Add participants equal to capacity
        for (int i = 0; i < CAPACITY; i++) {
            String userId = "user" + i;
            event.addParticipant(userId);
        }

        assertTrue("Event should be full when participants reach capacity", event.isFull());
    }
}

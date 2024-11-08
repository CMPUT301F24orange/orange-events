package com.example.orange;

import static org.junit.Assert.*;

import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;

import org.junit.Test;
import java.util.Collections;

/**
 * Unit tests for the User class
 * Testing to see if the User class is correctly initialized
 *
 * @author Viral Bhavsar
 */
public class UserTest {

    /**
     * Testing to see if user's participating and organizing are initialized as empty.
     *
     * @author Viral Bhavsar
     */
    @Test
    public void testDefaultConstructor() {
        User user = new User();

        // Verify that eventsParticipating and eventsOrganizing lists are initialized as empty lists
        assertNotNull("eventsParticipating should not be null", user.getEventsParticipating());
        assertNotNull("eventsOrganizing should not be null", user.getEventsOrganizing());
        assertEquals("eventsParticipating should be empty", Collections.emptyList(), user.getEventsParticipating());
        assertEquals("eventsOrganizing should be empty", Collections.emptyList(), user.getEventsOrganizing());
    }

    /**
     * Tests the parameterized constructor of the User class.
     * Tests that the deviceId and userType and lists are correctly set.
     *
     * @author Viral Bhavsar
     */
    @Test
    public void testParameterizedConstructor() {
        String deviceId = "orangeTeam";
        UserType userType = UserType.ORGANIZER;

        User user = new User(deviceId, userType);

        // Verify deviceId and userType are set correctly
        assertEquals("Device ID should match the input", deviceId, user.getDeviceId());
        assertEquals("User type should match the input", userType, user.getUserType());

        // Verify eventsParticipating and eventsOrganizing lists are initialized as empty lists
        assertNotNull("eventsParticipating should not be null", user.getEventsParticipating());
        assertNotNull("eventsOrganizing should not be null", user.getEventsOrganizing());
        assertEquals("eventsParticipating should be empty", Collections.emptyList(), user.getEventsParticipating());
        assertEquals("eventsOrganizing should be empty", Collections.emptyList(), user.getEventsOrganizing());

    }
}


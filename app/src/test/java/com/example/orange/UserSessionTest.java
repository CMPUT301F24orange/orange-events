package com.example.orange;

import static org.junit.Assert.*;

import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;

import org.junit.Test;

/**
 * Unit tests for the UserSession class.
 * Ensures correct handling of user session properties.
 *
 * @author graham flokstra
 */
public class UserSessionTest {

    private static final String DEVICE_ID = "testDevice123";
    private static final UserType USER_TYPE = UserType.ORGANIZER;
    private static final String USER_ID = "testUser456";

    /**
     * Tests the constructor and getter methods of the UserSession class.
     */
    @Test
    public void testConstructorAndGetters() {
        // Create a UserSession instance
        UserSession userSession = new UserSession(DEVICE_ID, USER_TYPE, USER_ID);

        // Verify that the constructor initializes fields correctly
        assertEquals("Device ID should match the input", DEVICE_ID, userSession.getdeviceId());
        assertEquals("User type should match the input", USER_TYPE, userSession.getUserType());
        assertEquals("User ID should match the input", USER_ID, userSession.getUserId());
    }

    /**
     * Tests behavior when null values are passed to the constructor.
     */
    @Test
    public void testConstructorWithNullValues() {
        // Create a UserSession with null values
        UserSession userSession = new UserSession(null, null, null);

        // Verify that the fields are initialized to null
        assertNull("Device ID should be null", userSession.getdeviceId());
        assertNull("User type should be null", userSession.getUserType());
        assertNull("User ID should be null", userSession.getUserId());
    }

    /**
     * Tests the UserSession class with edge cases for the input parameters.
     */
    @Test
    public void testConstructorWithEdgeCases() {
        // Edge case: Empty strings
        UserSession userSessionEmpty = new UserSession("", USER_TYPE, "");

        assertEquals("Device ID should be an empty string", "", userSessionEmpty.getdeviceId());
        assertEquals("User type should match the input", USER_TYPE, userSessionEmpty.getUserType());
        assertEquals("User ID should be an empty string", "", userSessionEmpty.getUserId());

        // Edge case: Long strings
        String longDeviceId = "a".repeat(1000); // String of length 1000
        String longUserId = "b".repeat(1000);
        UserSession userSessionLong = new UserSession(longDeviceId, USER_TYPE, longUserId);

        assertEquals("Device ID should match the input", longDeviceId, userSessionLong.getdeviceId());
        assertEquals("User type should match the input", USER_TYPE, userSessionLong.getUserType());
        assertEquals("User ID should match the input", longUserId, userSessionLong.getUserId());
    }
}

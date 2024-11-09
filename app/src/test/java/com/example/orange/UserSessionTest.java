package com.example.orange;

//package com.example.orange.data.model;

import static org.junit.Assert.*;

import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;

import org.junit.Test;

/**
 * Unit tests for the testing the userSession class
 *
 * @author Viral Bhavsar
 */
public class UserSessionTest {

    /**
     * Tests the UserSession constructor
     * Tests the userSession getter methods
     *
     * @author Viral Bhavsar
     */
    @Test
    public void testConstructorAndGetters() {
        // Sample data to test
        String expectedDeviceId = "orangeTeam";
        UserType expectedUserType = UserType.ORGANIZER;
        String expectedUserId = "orange";

        // Create a UserSession object with the sample data
        UserSession userSession = new UserSession(expectedDeviceId, expectedUserType, expectedUserId);

        // Verify that each getter returns the correct value
        assertEquals("Device ID should match the input", expectedDeviceId, userSession.getdeviceId());
        assertEquals("User type should match the input", expectedUserType, userSession.getUserType());
        assertEquals("User ID should match the input", expectedUserId, userSession.getUserId());
    }
}


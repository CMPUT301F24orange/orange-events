// SessionManagerTest.java
package com.example.orange;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the SessionManager class, ensuring proper handling of user sessions.
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_DEVICE_ID = "deviceID";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_ID = "userId";

    @Mock
    private Context mockContext;

    @Mock
    private SharedPreferences mockSharedPreferences;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Define behavior for context.getSharedPreferences
        when(mockContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)).thenReturn(mockSharedPreferences);

        // Define behavior for sharedPreferences.edit()
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);

        // Define behavior for editor.putString() to return editor for chaining
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);

        // Define behavior for editor.commit() to return true
        when(mockEditor.commit()).thenReturn(true);

        // Initialize SessionManager with mocked context
        sessionManager = new SessionManager(mockContext);
    }

    /**
     * Tests creating a login session and verifies that the correct data is stored in SharedPreferences.
     */
    @Test
    public void testCreateLoginSession() {
        // Sample data
        String username = "testUser";
        UserType userType = UserType.ENTRANT;
        String userId = "user123";

        // Call createLoginSession
        sessionManager.createLoginSession(username, userType, userId);

        // Capture the values passed to putString
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        // Verify that putString was called three times with correct keys and values
        verify(mockEditor, times(3)).putString(keyCaptor.capture(), valueCaptor.capture());

        assertEquals(KEY_DEVICE_ID, keyCaptor.getAllValues().get(0));
        assertEquals(username, valueCaptor.getAllValues().get(0));

        assertEquals(KEY_USER_TYPE, keyCaptor.getAllValues().get(1));
        assertEquals(userType.name(), valueCaptor.getAllValues().get(1));

        assertEquals(KEY_USER_ID, keyCaptor.getAllValues().get(2));
        assertEquals(userId, valueCaptor.getAllValues().get(2));

        // Verify that commit was called
        verify(mockEditor).commit();
    }

    /**
     * Tests retrieving the user session when a session exists.
     */
    @Test
    public void testGetUserSessionWhenSessionExists() {
        // Sample stored data
        String storedUsername = "storedUser";
        String storedUserType = UserType.ORGANIZER.name();
        String storedUserId = "organizer123";

        // Mock SharedPreferences to return stored values
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn(storedUsername);
        when(mockSharedPreferences.getString(KEY_USER_TYPE, null)).thenReturn(storedUserType);
        when(mockSharedPreferences.getString(KEY_USER_ID, null)).thenReturn(storedUserId);

        // Call getUserSession
        UserSession userSession = sessionManager.getUserSession();

        // Assert that the retrieved session is not null
        assertNotNull("UserSession should not be null when session exists", userSession);

        // Assert each field individually
        assertEquals("Device ID should match", storedUsername, userSession.getdeviceId());
        assertEquals("UserType should match", UserType.ORGANIZER, userSession.getUserType());
        assertEquals("User ID should match", storedUserId, userSession.getUserId());
    }

    /**
     * Tests retrieving the user session when no session exists.
     */
    @Test
    public void testGetUserSessionWhenSessionDoesNotExist() {
        // Mock SharedPreferences to return null for all keys
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn(null);
        when(mockSharedPreferences.getString(KEY_USER_TYPE, null)).thenReturn(null);
        when(mockSharedPreferences.getString(KEY_USER_ID, null)).thenReturn(null);

        // Call getUserSession
        UserSession userSession = sessionManager.getUserSession();

        // Assert that the session is null
        assertNull("UserSession should be null when no session exists", userSession);
    }

    /**
     * Tests logging out the user and verifies that SharedPreferences are cleared.
     */
    @Test
    public void testLogoutUser() {
        // Call logoutUser
        sessionManager.logoutUser();

        // Verify that editor.clear() and editor.commit() were called
        verify(mockEditor).clear();
        verify(mockEditor).commit();
    }

    /**
     * Tests isLoggedIn returns true when a user is logged in.
     */
    @Test
    public void testIsLoggedInWhenLoggedIn() {
        // Mock SharedPreferences to return a non-null device ID
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn("loggedInUser");

        // Call isLoggedIn
        boolean isLoggedIn = sessionManager.isLoggedIn();

        // Assert that isLoggedIn returns true
        assertTrue("isLoggedIn should return true when a user is logged in", isLoggedIn);
    }

    /**
     * Tests isLoggedIn returns false when no user is logged in.
     */
    @Test
    public void testIsLoggedInWhenNotLoggedIn() {
        // Mock SharedPreferences to return null for device ID
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn(null);

        // Call isLoggedIn
        boolean isLoggedIn = sessionManager.isLoggedIn();

        // Assert that isLoggedIn returns false
        assertFalse("isLoggedIn should return false when no user is logged in", isLoggedIn);
    }

    /**
     * Tests retrieving the user session when some keys are missing.
     * This ensures that getUserSession returns null if any key is missing.
     */
    @Test
    public void testGetUserSessionWithPartialData() {
        // Case 1: Missing userType
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn("partialUser");
        when(mockSharedPreferences.getString(KEY_USER_TYPE, null)).thenReturn(null);
        when(mockSharedPreferences.getString(KEY_USER_ID, null)).thenReturn("partial123");

        UserSession userSession1 = sessionManager.getUserSession();
        assertNull("UserSession should be null if userType is missing", userSession1);

        // Case 2: Missing userId
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn("partialUser");
        when(mockSharedPreferences.getString(KEY_USER_TYPE, null)).thenReturn(UserType.ADMIN.name());
        when(mockSharedPreferences.getString(KEY_USER_ID, null)).thenReturn(null);

        UserSession userSession2 = sessionManager.getUserSession();
        assertNull("UserSession should be null if userId is missing", userSession2);

        // Case 3: Missing deviceId
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn(null);
        when(mockSharedPreferences.getString(KEY_USER_TYPE, null)).thenReturn(UserType.ADMIN.name());
        when(mockSharedPreferences.getString(KEY_USER_ID, null)).thenReturn("partial123");

        UserSession userSession3 = sessionManager.getUserSession();
        assertNull("UserSession should be null if deviceId is missing", userSession3);
    }

    /**
     * Tests creating a login session with null or empty values.
     * Ensures that the SessionManager handles such cases appropriately.
     */
    @Test
    public void testCreateLoginSessionWithNullValues() {
        // Attempt to create a session with null values
        try {
            sessionManager.createLoginSession(null, null, null);
            fail("Expected NullPointerException due to null userType");
        } catch (NullPointerException e) {
            // Expected exception due to null userType.name()
            assertEquals("Cannot invoke \"com.example.orange.data.model.UserType.name()\" because \"userType\" is null", e.getMessage());
        }
    }

    /**
     * Tests the behavior when UserType is invalid.
     * Ensures that getUserSession handles invalid UserType gracefully.
     */
    @Test
    public void testGetUserSessionWithInvalidUserType() {
        // Mock SharedPreferences to return an invalid userType
        when(mockSharedPreferences.getString(KEY_DEVICE_ID, null)).thenReturn("invalidUser");
        when(mockSharedPreferences.getString(KEY_USER_TYPE, null)).thenReturn("INVALID_TYPE");
        when(mockSharedPreferences.getString(KEY_USER_ID, null)).thenReturn("invalid123");

        try {
            // Call getUserSession, expecting an IllegalArgumentException due to invalid enum
            sessionManager.getUserSession();
            fail("Expected IllegalArgumentException due to invalid UserType");
        } catch (IllegalArgumentException e) {
            // Test passes as exception is expected
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
}

package com.example.orange.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;

/**
 * SessionManager handles user session management using SharedPreferences.
 * It provides methods to create, retrieve, and clear user sessions.
 */
public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_DEVICE_ID = "deviceID";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_ID = "userId";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    /**
     * Constructor for SessionManager.
     * @param context The context used to access SharedPreferences.
     */
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Creates a login session by storing user details in SharedPreferences.
     * @param username The username of the logged-in user.
     * @param userType The type of the user (e.g., ENTRANT, ORGANIZER, ADMIN).
     * @param userId The unique identifier of the user.
     */
    public void createLoginSession(String username, UserType userType, String userId) {
        editor.putString(KEY_DEVICE_ID, username);
        editor.putString(KEY_USER_TYPE, userType.name());
        editor.putString(KEY_USER_ID, userId);
        editor.commit();
    }

    /**
     * Retrieves the current user session.
     * @return A UserSession object containing the user's details if a session exists, null otherwise.
     */
    public UserSession getUserSession() {
        String deviceId = pref.getString(KEY_DEVICE_ID, null);
        String userTypeStr = pref.getString(KEY_USER_TYPE, null);
        String userId = pref.getString(KEY_USER_ID, null);

        if (deviceId != null && userTypeStr != null && userId != null) {
            UserType userType = UserType.valueOf(userTypeStr);
            return new UserSession(deviceId, userType, userId);
        }
        return null;
    }

    /**
     * Logs out the current user by clearing all session data.
     */
    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

    /**
     * Checks if a user is currently logged in.
     * @return true if a user is logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return pref.getString(KEY_DEVICE_ID, null) != null;
    }
}
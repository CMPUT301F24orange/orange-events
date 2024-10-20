package com.example.orange.data.model;

/**
 * Represents a user session in the application.
 * This class stores information about the currently logged-in user.
 */
public class UserSession {
    private String deviceId;
    private UserType userType;
    private String userId;

    /**
     * Constructs a new UserSession.
     *
     * @param deviceId The deviceId of the logged-in user.
     * @param userType The type of the logged-in user (ENTRANT, ORGANIZER, or ADMIN).
     * @param userId The unique identifier of the logged-in user.
     */
    public UserSession(String deviceId, UserType userType, String userId) {
        this.deviceId = deviceId;
        this.userType = userType;
        this.userId = userId;
    }

    /**
     * Gets the deviceId of the logged-in user.
     *
     * @return The deviceId.
     */
    public String getdeviceId() {
        return deviceId;
    }

    /**
     * Gets the user type of the logged-in user.
     *
     * @return The UserType enum value.
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * Gets the unique identifier of the logged-in user.
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }
}
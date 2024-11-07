package com.example.orange.data.model;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the application.
 * This class contains user information and methods for managing user data.
 *
 * @author graham flokstra
 */
public class User {
    @DocumentId
    private String id;
    private String username;
    private UserType userType;
    private String email;
    private String phone;
    private String deviceId;
    private String profileImageUrl;
    private boolean receiveNotifications;
    private List<String> eventsParticipating;
    private List<String> eventsOrganizing;
    private boolean receiveOrganizerNotifications;
    private boolean receiveAdminNotifications;
    private Blob profileImageData; // Changed from byte[] to Blob
    private String facilityId;
    /**
     * Default constructor required for Firestore.
     * Initializes empty lists for events.
     */
    public User() {
        eventsParticipating = new ArrayList<>();
        eventsOrganizing = new ArrayList<>();
    }

    /**
     * Constructs a new User with specified username and user type.
     *
     * @param deviceId The deviceId of the user.
     * @param userType The type of the user (e.g., ENTRANT, ORGANIZER, ADMIN).
     */
    public User(String deviceId, UserType userType) {
        this.deviceId = deviceId;
        this.userType = userType;
        this.eventsParticipating = new ArrayList<>();
        this.eventsOrganizing = new ArrayList<>();
        this.receiveOrganizerNotifications = false;
        this.receiveAdminNotifications = false;
    }

    // Getters and Setters

    /**
     * Gets the user's ID.
     *
     * @return The user's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the user's ID.
     *
     * @param id The ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the user's username.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username.
     *
     * @param username The username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the user's type.
     *
     * @return The user type.
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * Sets the user's type.
     *
     * @param userType The user type to set.
     */
    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    /**
     * Gets the list of events the user is participating in.
     *
     * @return List of event IDs.
     */
    public List<String> getEventsParticipating() {
        return eventsParticipating;
    }

    /**
     * Sets the list of events the user is participating in.
     *
     * @param eventsParticipating List of event IDs to set.
     */
    public void setEventsParticipating(List<String> eventsParticipating) {
        this.eventsParticipating = eventsParticipating;
    }

    /**
     * Gets the list of events the user is organizing.
     *
     * @return List of event IDs.
     */
    public List<String> getEventsOrganizing() {
        return eventsOrganizing;
    }

    /**
     * Sets the list of events the user is organizing.
     *
     * @param eventsOrganizing List of event IDs to set.
     */
    public void setEventsOrganizing(List<String> eventsOrganizing) {
        this.eventsOrganizing = eventsOrganizing;
    }

    /**
     * Checks if the user receives organizer notifications.
     *
     * @return true if the user receives organizer notifications, false otherwise.
     */
    public boolean isReceiveOrganizerNotifications() {
        return receiveOrganizerNotifications;
    }

    /**
     * Sets whether the user receives organizer notifications.
     *
     * @param receiveOrganizerNotifications true to receive notifications, false otherwise.
     */
    public void setReceiveOrganizerNotifications(boolean receiveOrganizerNotifications) {
        this.receiveOrganizerNotifications = receiveOrganizerNotifications;
    }

    /**
     * Checks if the user receives admin notifications.
     *
     * @return true if the user receives admin notifications, false otherwise.
     */
    public boolean isReceiveAdminNotifications() {
        return receiveAdminNotifications;
    }

    /**
     * Sets whether the user receives admin notifications.
     *
     * @param receiveAdminNotifications true to receive notifications, false otherwise.
     */
    public void setReceiveAdminNotifications(boolean receiveAdminNotifications) {
        this.receiveAdminNotifications = receiveAdminNotifications;
    }


    /**
     * Adds an event to the list of events the user is participating in.
     *
     * @param eventId The ID of the event to add.
     */
    public void addEventParticipating(String eventId) {
        if (!this.eventsParticipating.contains(eventId)) {
            this.eventsParticipating.add(eventId);
        }
    }

    /**
     * Removes an event from the list of events the user is participating in.
     *
     * @param eventId The ID of the event to remove.
     */
    public void removeEventParticipating(String eventId) {
        this.eventsParticipating.remove(eventId);
    }

    /**
     * Adds an event to the list of events the user is organizing.
     *
     * @param eventId The ID of the event to add.
     */
    public void addEventOrganizing(String eventId) {
        if (!this.eventsOrganizing.contains(eventId)) {
            this.eventsOrganizing.add(eventId);
        }
    }

    /**
     * Removes an event from the list of events the user is organizing.
     *
     * @param eventId The ID of the event to remove.
     */
    public void removeEventOrganizing(String eventId) {
        this.eventsOrganizing.remove(eventId);
    }

    /**
     * Checks if the user is an organizer.
     *
     * @return true if the user is an organizer, false otherwise.
     */
    public boolean isOrganizer() {
        return this.userType == UserType.ORGANIZER;
    }

    /**
     * Checks if the user is an admin.
     *
     * @return true if the user is an admin, false otherwise.
     */
    public boolean isAdmin() {
        return this.userType == UserType.ADMIN;
    }

    /**
     * Retrieves the user's email address.
     *
     * @return The email address of the user.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The new email address to set for the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Retrieves the user's phone number.
     *
     * @return The phone number of the user.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number.
     *
     * @param phone The new phone number to set for the user.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Retrieves the user's device ID, which uniquely identifies the user's device.
     *
     * @return The device ID of the user.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the user's device ID, which is used to uniquely identify the user based on their device.
     *
     * @param deviceId The new device ID to set for the user.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Retrieves the user's profile image data.
     *
     * @return The profile image of the user.
     */
    public Blob getProfileImageData() {
        return profileImageData;
    }

    /**
     * Sets the user's profile image.
     *
     * @param profileImageData The new device image to set for the user.
     */
    public void setProfileImageData(Blob profileImageData) {
        this.profileImageData = profileImageData;
    }

    /**
     * Retrieves notification preference.
     *
     * @return boolean
     */
    public boolean isReceiveNotifications() {
        return receiveNotifications;
    }

    /**
     * Sets the user's notification settings.
     *
     * @param receiveNotifications The notification preferences.
     */
    public void setReceiveNotifications(boolean receiveNotifications) {
        this.receiveNotifications = receiveNotifications;
    }


    /**
     * Gets the facility ID associated with the user.
     *
     * @return The facility ID.
     */
    public String getFacilityId() {
        return facilityId;
    }

    /**
     * Sets the facility ID associated with the user.
     *
     * @param facilityId The facility ID to set.
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * Returns a string representation of the User object.
     *
     * @return A string containing user details.
     */
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", userType=" + userType +
                ", eventsParticipating=" + eventsParticipating.size() +
                ", eventsOrganizing=" + eventsOrganizing.size() +
                ", receiveOrganizerNotifications=" + receiveOrganizerNotifications +
                ", receiveAdminNotifications=" + receiveAdminNotifications +
                '}';
    }
}

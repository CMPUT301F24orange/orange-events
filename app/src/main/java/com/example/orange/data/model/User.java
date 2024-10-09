package com.example.orange.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the application.
 * This class contains user information and methods for managing user data.
 */
public class User {
    @DocumentId
    private String id;
    private String username;
    private UserType userType;
    private List<String> eventsParticipating;
    private List<String> eventsOrganizing;
    private boolean receiveOrganizerNotifications;
    private boolean receiveAdminNotifications;

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
     * @param username The username of the user.
     * @param userType The type of the user (e.g., ENTRANT, ORGANIZER, ADMIN).
     */
    public User(String username, UserType userType) {
        this.username = username;
        this.userType = userType;
        this.eventsParticipating = new ArrayList<>();
        this.eventsOrganizing = new ArrayList<>();
        this.receiveOrganizerNotifications = false;
        this.receiveAdminNotifications = false;
    }

    // Getters and Setters

    /**
     * Gets the user's ID.
     * @return The user's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the user's ID.
     * @param id The ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the user's username.
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username.
     * @param username The username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the user's type.
     * @return The user type.
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * Sets the user's type.
     * @param userType The user type to set.
     */
    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    /**
     * Gets the list of events the user is participating in.
     * @return List of event IDs.
     */
    public List<String> getEventsParticipating() {
        return eventsParticipating;
    }

    /**
     * Sets the list of events the user is participating in.
     * @param eventsParticipating List of event IDs to set.
     */
    public void setEventsParticipating(List<String> eventsParticipating) {
        this.eventsParticipating = eventsParticipating;
    }

    /**
     * Gets the list of events the user is organizing.
     * @return List of event IDs.
     */
    public List<String> getEventsOrganizing() {
        return eventsOrganizing;
    }

    /**
     * Sets the list of events the user is organizing.
     * @param eventsOrganizing List of event IDs to set.
     */
    public void setEventsOrganizing(List<String> eventsOrganizing) {
        this.eventsOrganizing = eventsOrganizing;
    }

    /**
     * Checks if the user receives organizer notifications.
     * @return true if the user receives organizer notifications, false otherwise.
     */
    public boolean isReceiveOrganizerNotifications() {
        return receiveOrganizerNotifications;
    }

    /**
     * Sets whether the user receives organizer notifications.
     * @param receiveOrganizerNotifications true to receive notifications, false otherwise.
     */
    public void setReceiveOrganizerNotifications(boolean receiveOrganizerNotifications) {
        this.receiveOrganizerNotifications = receiveOrganizerNotifications;
    }

    /**
     * Checks if the user receives admin notifications.
     * @return true if the user receives admin notifications, false otherwise.
     */
    public boolean isReceiveAdminNotifications() {
        return receiveAdminNotifications;
    }

    /**
     * Sets whether the user receives admin notifications.
     * @param receiveAdminNotifications true to receive notifications, false otherwise.
     */
    public void setReceiveAdminNotifications(boolean receiveAdminNotifications) {
        this.receiveAdminNotifications = receiveAdminNotifications;
    }

    // Helper methods

    /**
     * Adds an event to the list of events the user is participating in.
     * @param eventId The ID of the event to add.
     */
    public void addEventParticipating(String eventId) {
        if (!this.eventsParticipating.contains(eventId)) {
            this.eventsParticipating.add(eventId);
        }
    }

    /**
     * Removes an event from the list of events the user is participating in.
     * @param eventId The ID of the event to remove.
     */
    public void removeEventParticipating(String eventId) {
        this.eventsParticipating.remove(eventId);
    }

    /**
     * Adds an event to the list of events the user is organizing.
     * @param eventId The ID of the event to add.
     */
    public void addEventOrganizing(String eventId) {
        if (!this.eventsOrganizing.contains(eventId)) {
            this.eventsOrganizing.add(eventId);
        }
    }

    /**
     * Removes an event from the list of events the user is organizing.
     * @param eventId The ID of the event to remove.
     */
    public void removeEventOrganizing(String eventId) {
        this.eventsOrganizing.remove(eventId);
    }

    /**
     * Checks if the user is an organizer.
     * @return true if the user is an organizer, false otherwise.
     */
    @Exclude
    public boolean isOrganizer() {
        return this.userType == UserType.ORGANIZER;
    }

    /**
     * Checks if the user is an admin.
     * @return true if the user is an admin, false otherwise.
     */
    @Exclude
    public boolean isAdmin() {
        return this.userType == UserType.ADMIN;
    }

    /**
     * Returns a string representation of the User object.
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
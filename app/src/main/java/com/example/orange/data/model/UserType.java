package com.example.orange.data.model;

/**
 * Enum representing the different types of users in the application.
 */
public enum UserType {
    /**
     * Represents a regular user who participates in events.
     */
    ENTRANT,

    /**
     * Represents a user who can create and manage events.
     */
    ORGANIZER,

    /**
     * Represents a user with administrative privileges.
     */
    ADMIN
}
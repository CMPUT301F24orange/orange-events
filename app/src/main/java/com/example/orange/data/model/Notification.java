package com.example.orange.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.Timestamp;

/**
 * This class defines a Notification.
 *
 * It holds data related to notifications such as the associated event,
 * the recipient user, the type of notification, and its current status.
 * Depending on the type, the user may need to take specific actions.
 *
 * Example types include:
 * - Selected to participate in an event
 * - Not selected for participation
 * - Added to a cancelled list
 * - Added to a waitlist
 *
 * Each notification can have a status indicating whether it's pending,
 * accepted, declined, or resolved.
 *
 * @author
 */
public class Notification {
    @DocumentId
    private String id;
    private String eventId;
    private String userId;
    private NotificationType type;
    private NotificationStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Default constructor required for Firestore.
     */
    public Notification() {
        this.status = NotificationStatus.PENDING;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    /**
     * Constructor with parameters.
     *
     * @param eventId The ID of the event associated with this notification.
     * @param userId The ID of the user to whom the notification is sent.
     * @param type The type of the notification.
     */
    public Notification(String eventId, String userId, NotificationType type) {
        this.eventId = eventId;
        this.userId = userId;
        this.type = type;
        this.status = NotificationStatus.PENDING;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters and Setters

    /**
     * Gets the notification ID.
     *
     * @return The notification ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the notification ID.
     *
     * @param id The notification ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the event ID associated with this notification.
     *
     * @return The event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the event ID associated with this notification.
     *
     * @param eventId The event ID.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the user ID to whom the notification is sent.
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID to whom the notification is sent.
     *
     * @param userId The user ID.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the type of the notification.
     *
     * @return The notification type.
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * Sets the type of the notification.
     *
     * @param type The notification type.
     */
    public void setType(NotificationType type) {
        this.type = type;
    }

    /**
     * Gets the current status of the notification.
     *
     * @return The notification status.
     */
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * Sets the current status of the notification.
     *
     * @param status The notification status.
     */
    public void setStatus(NotificationStatus status) {
        this.status = status;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Gets the creation timestamp of the notification.
     *
     * @return The creation timestamp.
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the notification.
     *
     * @param createdAt The creation timestamp.
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last updated timestamp of the notification.
     *
     * @return The last updated timestamp.
     */
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last updated timestamp of the notification.
     *
     * @param updatedAt The last updated timestamp.
     */
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility Methods

    /**
     * Marks the notification as accepted.
     */
    public void accept() {
        this.status = NotificationStatus.ACCEPTED;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Marks the notification as declined.
     */
    public void decline() {
        this.status = NotificationStatus.DECLINED;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Marks the notification as resolved.
     */
    public void resolve() {
        this.status = NotificationStatus.RESOLVED;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Returns a string representation of the Notification object.
     *
     * @return A string containing notification details.
     */
    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

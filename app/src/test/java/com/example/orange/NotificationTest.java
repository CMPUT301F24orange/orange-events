package com.example.orange;

import static org.junit.Assert.*;
import org.junit.Test;

import com.example.orange.data.model.NotificationStatus;
import com.example.orange.data.model.Notification;
import com.example.orange.data.model.NotificationType;

/**
 * Unit tests for the Notification class, ensuring proper handling of its properties.
 * @author Dhairua Prajapati
 */
public class NotificationTest {

    /**
     * Tests the default constructor of the Notification class.
     * Verifies that all of its variables are initialized correctly.
     * @author Dhairya Prajapati
     */
    @Test
    public void testDefaultConstructor() {
        // Arrange & Act
        Notification notification = new Notification();

        // Assert
        assertNull("ID should be null for default constructor.", notification.getId());
        assertNull("Event ID should be null for default constructor.", notification.getEventId());
        assertNull("User ID should be null for default constructor.", notification.getUserId());
        assertNull("Type should be null for default constructor.", notification.getType());
        assertEquals("Default status should be PENDING.", NotificationStatus.PENDING, notification.getStatus());
        assertNotNull("CreatedAt should not be null for default constructor.", notification.getCreatedAt());
        assertNotNull("UpdatedAt should not be null for default constructor.", notification.getUpdatedAt());
    }

    /**
     * Tests the constructor with a parameter.
     * @author Dhairya Prajapati
     */
    @Test
    public void testParameterizedConstructor() {
        // Arrange
        String eventId = "Event123";
        String userId = "User456";
        NotificationType type = NotificationType.SELECTED_TO_PARTICIPATE;

        // Act
        Notification notification = new Notification(eventId, userId, type);

        // Assert
        assertNull("ID should be null when constructed with parameters.", notification.getId());
        assertEquals("Event ID should match the constructor argument.", eventId, notification.getEventId());
        assertEquals("User ID should match the constructor argument.", userId, notification.getUserId());
        assertEquals("Type should match the constructor argument.", type, notification.getType());
        assertEquals("Default status should be PENDING.", NotificationStatus.PENDING, notification.getStatus());
        assertNotNull("CreatedAt should not be null.", notification.getCreatedAt());
        assertNotNull("UpdatedAt should not be null.", notification.getUpdatedAt());
    }

    /**
     * Tests the accept method to ensure the status is accepted if the user clicks accept.
     * @author Dhairya Prajapati
     */
    @Test
    public void testAcceptMethod() {
        // Arrange
        Notification notification = new Notification();

        // Act
        notification.accept();

        // Assert
        assertEquals("Status should be ACCEPTED after calling accept.", NotificationStatus.ACCEPTED, notification.getStatus());
        assertNotNull("UpdatedAt should not be null after calling accept.", notification.getUpdatedAt());
    }

    /**
     * Tests the decline method to ensure the status is accepted if the user clicks decline.
     * @author Dhairya Prajapati
     */
    @Test
    public void testDeclineMethod() {
        // Arrange
        Notification notification = new Notification();

        // Act
        notification.decline();

        // Assert
        assertEquals("Status should be DECLINED after calling decline.", NotificationStatus.DECLINED, notification.getStatus());
        assertNotNull("UpdatedAt should not be null after calling decline.", notification.getUpdatedAt());
    }

    /**
     * Tests the resolve method to ensure the status is resolved if the user resolves it/
     * @author Dhairya Prajapati
     */
    @Test
    public void testResolveMethod() {
        // Arrange
        Notification notification = new Notification();

        // Act
        notification.resolve();

        // Assert
        assertEquals("Status should be RESOLVED after calling resolve.", NotificationStatus.RESOLVED, notification.getStatus());
        assertNotNull("UpdatedAt should not be null after calling resolve.", notification.getUpdatedAt());
    }
}


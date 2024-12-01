package com.example.orange;

import static org.junit.Assert.*;
import org.junit.Test;

import com.example.orange.data.model.ImageData;
import com.google.firebase.firestore.Blob;

/**
 * Unit tests for the ImageData class, ensuring correct handling of its properties.
 * @author Dhairya Prajapati
 */
public class ImageDataTest {

    /**
     * Tests the default constructor of the ImageData class.
     * Verifies that all fields are null upon initialization.
     * @author Dhairya Prajapati
     */
    @Test
    public void testDefaultConstructor() {
        // Arrange & Act
        ImageData imageData = new ImageData();

        // Assert
        assertNull("ID should be null for the default constructor", imageData.getId());
        assertNull("ImageData should be null for the default constructor.", imageData.getImageData());
    }

    /**
     * Tests the constructor with ImageData
     * @author Dhairya Prajapati
     */
    @Test
    public void testConstructorWithImageData() {
        // Arrange
        Blob blob = Blob.fromBytes(new byte[]{1, 2, 3});

        // Act
        ImageData imageData = new ImageData(blob);

        // Assert
        assertNull("ID should be null when constructed with only image data.", imageData.getId());
        assertEquals("ImageData should match the constructor argument.", blob, imageData.getImageData());
    }

    /**
     * Tests the full constructor of the ImageData class
     * @author Dhairya Prajapati
     */
    @Test
    public void testFullConstructor() {
        // Arrange
        String id = "Image123";
        Blob blob = Blob.fromBytes(new byte[]{1, 2, 3});

        // Act
        ImageData imageData = new ImageData(id, blob);

        // Assert
        assertEquals("ID should match the constructor argument.", id, imageData.getId());
        assertEquals("ImageData should match the constructor argument.", blob, imageData.getImageData());
    }
}


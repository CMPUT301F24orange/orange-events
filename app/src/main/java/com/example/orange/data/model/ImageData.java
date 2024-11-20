package com.example.orange.data.model;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents image data stored in Firebase.
 *
 * @author
 */
public class ImageData {
    @DocumentId
    private String id;
    private Blob imageData;

    /**
     * Default constructor required for Firestore.
     */
    public ImageData() {
        // Default constructor
    }

    /**
     * Constructor with image data.
     *
     * @param imageData The image data as a Blob.
     */
    public ImageData(Blob imageData) {
        this.imageData = imageData;
    }

    /**
     * Full constructor with ID and image data.
     *
     * @param id        The ID of the image.
     * @param imageData The image data as a Blob.
     */
    public ImageData(String id, Blob imageData) {
        this.id = id;
        this.imageData = imageData;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public Blob getImageData() {
        return imageData;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImageData(Blob imageData) {
        this.imageData = imageData;
    }
}

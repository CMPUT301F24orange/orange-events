package com.example.orange.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.Objects;

/**
 * Represents a Facility in the application.
 *
 * @author Graham Flokstra
 */
public class Facility {
    @DocumentId
    private String id;
    private String name;
    private String address;

    /**
     * Default constructor required for Firestore.
     */
    public Facility() {
    }

    /**
     * Constructor with parameters.
     *
     * @param name    The name of the facility.
     * @param address The address of the facility.
     */
    public Facility(String name, String address) {
        this.name = name;
        this.address = address;
    }

    // Getters and setters

    /**
     * Gets the facility's ID.
     *
     * @return The facility's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the facility's ID.
     *
     * @param id The facility's ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the facility's name.
     *
     * @return The facility's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the facility's name.
     *
     * @param name The facility's name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the facility's address.
     *
     * @return The facility's address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the facility's address.
     *
     * @param address The facility's address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    // Override equals() and hashCode() based on 'id'
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Facility facility = (Facility) o;
        return Objects.equals(id, facility.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

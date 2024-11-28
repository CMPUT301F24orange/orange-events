package com.example.orange;

//package com.example.orange.data.model;

import static org.junit.Assert.*;

import com.example.orange.data.model.Facility;

import org.junit.Test;

/**
 * Unit testing for the facility class
 *
 * @author Viral Bhavsar, Dhariya Prajapati
 */
public class FacilityTest {

    /**
     *Tests the default constructor of the Facility class.
     *Checkes if the lists are null
     *
     * @author Dhairya Prajapati
     */
    @Test
    public void testDefaultConstructor() {
        Facility facility = new Facility();

        // Verify that all fields are null when using the default constructor
        assertNull("ID should be null", facility.getId());
        assertNull("Name should be null", facility.getName());
        assertNull("Address should be null", facility.getAddress());
    }

    /**
     * Tests the parameterized constructor
     * Checks if name and address are correctly
     *
     * @author Viral bhavsar, Dhariya Prajapati
     */
    @Test
    public void testParameterizedConstructor() {
        String expectedName = "Orange Center";
        String expectedAddress = "123 Orange St";

        Facility facility = new Facility(expectedName, expectedAddress);

        // Verify that the name and address are set correctly
        assertEquals("Name should match the input", expectedName, facility.getName());
        assertEquals("Address should match the input", expectedAddress, facility.getAddress());
    }

    /**
     * Testing the ID getter and setter
     *
     * @author Viral Bhavsar, Dhairya Prajapati
     */
    @Test
    public void testIdGetterSetter() {
        Facility facility = new Facility();
        String expectedId = "orange123";

        facility.setId(expectedId);

        // Verify that the ID is set and retrieved correctly
        assertEquals("ID should match the set value", expectedId, facility.getId());
    }

    /**
     * Testing the name getter and setter
     *
     * @author Viral Bhavsar, Dhairya Prajapati
     */
    @Test
    public void testNameGetterSetter() {
        Facility facility = new Facility();
        String expectedName = "Orange University";

        facility.setName(expectedName);

        // Verify that the name is set and retrieved correctly
        assertEquals("Name should match the set value", expectedName, facility.getName());
    }

    /**
     * Testing the address getter and setter.
     *
     * @author Viral Bhavsar, Dhairya Prajapati
     */
    @Test
    public void testAddressGetterSetter() {
        Facility facility = new Facility();
        String expectedAddress = "123 Orange St";

        facility.setAddress(expectedAddress);

        // Verify that the address is set and retrieved correctly
        assertEquals("Address should match the set value", expectedAddress, facility.getAddress());
    }
}

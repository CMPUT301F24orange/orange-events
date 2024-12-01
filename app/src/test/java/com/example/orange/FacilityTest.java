package com.example.orange;

import static org.junit.Assert.*;
import com.example.orange.data.model.Facility;
import org.junit.Test;

/**
 * Unit tests for the Facility class, ensuring correct handling of properties.
 * @author Viral Bhavsar, Dhairya Prajapati
 */
public class FacilityTest {

    /**
     * Tests the default constructor of the Facility class.
     * Verifies that all fields are null upon initialization.
     * @author Dhairya Prajapati
     */
    @Test
    public void testDefaultConstructor() {
        Facility facility = new Facility();

        // Verify that all fields are null when using the default constructor
        assertNull("ID should be null", facility.getId());
        assertNull("Name should be null", facility.getName());
        assertNull("Address should be null", facility.getAddress());

        // If Facility has other fields, verify their initial state as well
        // e.g., List<Event> events
        // assertNull("Events list should be null", facility.getEvents());
    }

    /**
     * Tests the parameterized constructor of the Facility class.
     * Verifies that name and address are set correctly.
     * @author Viral Bhavsar
     */
    @Test
    public void testParameterizedConstructor() {
        String expectedName = "Orange Center";
        String expectedAddress = "123 Orange St";

        Facility facility = new Facility(expectedName, expectedAddress);

        // Verify that the name and address are set correctly
        assertEquals("Name should match the input", expectedName, facility.getName());
        assertEquals("Address should match the input", expectedAddress, facility.getAddress());

        // Verify that ID is still null if not set
        assertNull("ID should be null", facility.getId());

        // If Facility has other fields, verify their initial state
        // e.g., List<Event> events
        // assertNull("Events list should be null", facility.getEvents());
    }

    /**
     * Tests the ID getter and setter methods.
     * @author Dhairya Prajapati
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
     * Tests the name getter and setter methods.
     * @author Viral Bhavsar
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
     * Tests the address getter and setter methods.
     * @author Dhairya Prajapati
     */
    @Test
    public void testAddressGetterSetter() {
        Facility facility = new Facility();
        String expectedAddress = "123 Orange St";

        facility.setAddress(expectedAddress);

        // Verify that the address is set and retrieved correctly
        assertEquals("Address should match the set value", expectedAddress, facility.getAddress());
    }

    /**
     * Tests the equality of two Facility objects.
     * Assumes that equals() is overridden to compare based on ID.
     * @author Viral Bhavsar
     */
    @Test
    public void testFacilityEquality() {
        String facilityId = "facility123";
        Facility facility1 = new Facility();
        facility1.setId(facilityId);
        facility1.setName("Orange Center");
        facility1.setAddress("123 Orange St");

        Facility facility2 = new Facility();
        facility2.setId(facilityId);
        facility2.setName("Orange Center");
        facility2.setAddress("123 Orange St");

        assertEquals("Facilities with same ID should be equal", facility1, facility2);
    }

    /**
     * Tests that two Facilities with different IDs are not equal.
     * @author Viral Bhavsar
     */
    @Test
    public void testFacilityInequality() {
        Facility facility1 = new Facility();
        facility1.setId("facility123");
        facility1.setName("Orange Center");
        facility1.setAddress("123 Orange St");

        Facility facility2 = new Facility();
        facility2.setId("facility456");
        facility2.setName("Orange Center");
        facility2.setAddress("123 Orange St");

        assertNotEquals("Facilities with different IDs should not be equal", facility1, facility2);
    }
}

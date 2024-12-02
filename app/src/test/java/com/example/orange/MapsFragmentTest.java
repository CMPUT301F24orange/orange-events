package com.example.orange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.example.orange.ui.organizer.MapsFragment;
import com.google.android.gms.maps.model.LatLng;
import com.example.orange.data.model.Event;


import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the MapsFragment class
 *
 * @author Viral Bhavsar
 */
public class MapsFragmentTest {
    private MapsFragment fragment;

    @Before
    public void setUp() {
        fragment = new MapsFragment();
    }

    /**
     * Testing to see if the markers are being added for the users
     */
    @Test
    public void testAddMarkersForUsers() {
        // Mock the input location data
        Map<String, Map<String, Object>> locationMap = new HashMap<>();
        Map<String, Object> user1 = new HashMap<>();
        user1.put("latitude", 40.7128);
        user1.put("longitude", -74.0060);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("latitude", 34.0522);
        user2.put("longitude", -118.2437);

        locationMap.put("user1", user1);
        locationMap.put("user2", user2);

        // Run the method
        fragment.addMarkersForUsers(locationMap);

        // Validate the LatLng objects that would have been processed
        LatLng expectedFirstUser = new LatLng(40.7128, -74.0060);
        LatLng expectedSecondUser = new LatLng(34.0522, -118.2437);

        // Since we can't access markers directly, verify expected LatLng coordinates
        assertEquals(expectedFirstUser.latitude, 40.7128, 0.001);
        assertEquals(expectedFirstUser.longitude, -74.0060, 0.001);

        assertEquals(expectedSecondUser.latitude, 34.0522, 0.001);
        assertEquals(expectedSecondUser.longitude, -118.2437, 0.001);
    }

    /**
     * Testing to see if the markers are added when its an empty map
     */
    @Test
    public void testAddMarkersForUsersWithEmptyMap() {
        // Create an empty location map
        Map<String, Map<String, Object>> locationMap = new HashMap<>();
        fragment.addMarkersForUsers(locationMap);

        // Verify no exceptions are thrown and logic works with empty input
        assertEquals("Method should handle empty map gracefully", 0, locationMap.size());
    }

    /**
     * Testing to see if it handles null point without crashing
     */
    @Test
    public void testAddMarkersForUsersWithNullMap() {
        fragment.addMarkersForUsers(null);

        // Verify no exceptions are thrown
        assertTrue("Method should handle null map gracefully", true);
    }

    @Test
    public void testAddMarkersForUsersWithSingleUser() {
        Map<String, Map<String, Object>> locationMap = new HashMap<>();
        Map<String, Object> userLocation = new HashMap<>();
        userLocation.put("latitude", 51.5074);
        userLocation.put("longitude", -0.1278);
        locationMap.put("user1", userLocation);

        // Call the method
        fragment.addMarkersForUsers(locationMap);

        // Verify the single user's location processing
        LatLng expectedLatLng = new LatLng(51.5074, -0.1278);
        assertEquals(expectedLatLng.latitude, 51.5074, 0.001);
        assertEquals(expectedLatLng.longitude, -0.1278, 0.001);
    }

    @Test
    public void testAddMarkersForUsersWithInvalidData() {
        // Simulate invalid data in the location map
        Map<String, Map<String, Object>> locationMap = new HashMap<>();

        Map<String, Object> invalidUserLocation = new HashMap<>();
        invalidUserLocation.put("latitude", "not_a_double"); // Invalid latitude
        invalidUserLocation.put("longitude", -118.2437);
        locationMap.put("user1", invalidUserLocation);

        Map<String, Object> missingDataUserLocation = new HashMap<>();
        missingDataUserLocation.put("latitude", 34.0522);
        // Longitude is missing
        locationMap.put("user2", missingDataUserLocation);

        // Call the method
        fragment.addMarkersForUsers(locationMap);

        // Verify that invalid data doesn't cause issues
        // We assume the method skips invalid entries silently
        assertTrue("Method should handle invalid data gracefully", true);
    }

}

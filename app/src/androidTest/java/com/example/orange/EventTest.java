package com.example.orange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.example.orange.data.model.Event;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Test class for the Event class.
 * This class contains tests for various properties of the Event class,
 * including title, price, capacity, waiting list and more behaviours.
 * @author Dhairya Prajapati
 */
public class EventTest {

    private Event event;

    /**
     * Sets up a sample Event instance before each test.
     * Initializes the event with default values to be used in test methods.
     * @author Dhairya Prajapati
     */
    @BeforeEach
    public void setUp() {
        event = new Event();
        event.setTitle("Sample Event");
        event.setDescription("This is a sample event.");
        event.setPrice(50.0);
        event.setCapacity(100);
        event.setWaitlistLimit(10);
        event.setGeolocationEvent(true);
        event.setWaitingList(new ArrayList<>());
    }

    /**
     * Tests the getTitle and setTitle methods.
     * Verifies that the title can be retrieved and updated.
     * @author Dhairya Prajapati
     */
    @Test
    public void testTitle() {
        assertEquals("Sample Event", event.getTitle());
        event.setTitle("Updated Event");
        assertEquals("Updated Event", event.getTitle());
    }

    /**
     * Tests the getDescription and setDescription methods.
     * Verifies that the description can be retrieved and updated.
     * @author Dhairya Prajapati
     */
    @Test
    public void testDescription() {
        assertEquals("This is a sample event.", event.getDescription());
        event.setDescription("Updated description.");
        assertEquals("Updated description.", event.getDescription());
    }

    /**
     * Tests the getCapacity and setCapacity methods.
     * Ensures the capacity is set and retrieved correctly.
     * @author Dhairya Prajapati
     */
    @Test
    public void testCapacity() {
        assertEquals(100, event.getCapacity());
        event.setCapacity(200);
        assertEquals(200, event.getCapacity());
    }

    /**
     * Tests the getPrice and setPrice methods.
     * Verifies that the price can be set and retrieved correctly.
     * @author Dhairya Prajapati
     */
    @Test
    public void testPrice() {
        assertEquals(50.0, event.getPrice());
        event.setPrice(75.0);
        assertEquals(75.0, event.getPrice());
    }

    /**
     * Tests the getWaitlistLimit method.
     * Ensures the waitlist limit is correctly set and retrieved.
     * @author Dhairya Prajapati
     */
    @Test
    public void testWaitingListLimit() {
        assertEquals(10, event.getWaitlistLimit());
    }

    /**
     * Tests the getWaitingList and setWaitingList methods.
     * Verifies that users can be added to and retrieved from the waiting list.
     * @author Dhairya Prajapati
     */
    @Test
    public void testWaitingList() {
        List<String> sampleList = Arrays.asList("User1", "User2");
        event.setWaitingList(sampleList);
        assertEquals(sampleList, event.getWaitingList());

        event.getWaitingList().add("User3");
        assertEquals(3, event.getWaitingList().size());
        assertTrue(event.getWaitingList().contains("User3"));
    }

    /**
     * Tests the getGeolocationEvent and setGeolocationEvent methods.
     * Ensures the geolocationEvent property can be set and retrieved correctly.
     * @author Dhairya Prajapati
     */
    @Test
    public void testGeolocationEvent() {
        assertTrue(event.getGeolocationEvent());
        event.setGeolocationEvent(false);
        assertFalse(event.getGeolocationEvent());
    }

    /**
     * Tests the date-related fields, including startDate, endDate,
     * registrationOpens, registrationDeadline, lotteryDrawDate, and eventDate.
     * Verifies that each timestamp can be set and retrieved correctly.
     * @author Dhairya Prajapati
     */
    @Test
    public void testTimestampFields() {
        Timestamp now = Timestamp.now();
        event.setStartDate(now);
        assertEquals(now, event.getStartDate());

        event.setEndDate(now);
        assertEquals(now, event.getEndDate());

        event.setRegistrationOpens(now);
        assertEquals(now, event.getRegistrationOpens());

        event.setRegistrationDeadline(now);
        assertEquals(now, event.getRegistrationDeadline());

        event.setLotteryDrawDate(now);
        assertEquals(now, event.getLotteryDrawDate());

        event.setEventDate(now);
        assertEquals(now, event.getEventDate());
    }
}


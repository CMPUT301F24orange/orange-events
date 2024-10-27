package com.example.orange.data.model;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

/* Todo:
 *    Add in event image functionality
 *   Complete overall functionality
 */

/**
 * This class defines an Event
 *
 * @author Graham Flokstra
 */
public class Event {
    @DocumentId // Helps auto populate document id with firestore
    private String id;
    private String title;
    private String description;
    private Timestamp date;
    private Integer capacity;
    private String organizerId;
    private List<String> waitingList;
    private List<String> participants;

    /**
     * Default constructor required for Firestone
     *
     * @see <a href="https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects">...</a>
     */
    public Event() {
        waitingList = new ArrayList<>();
        participants = new ArrayList<>();
    }

    /**
     * Constructor
     *
     * @param title
     * @param description
     * @param date
     * @param capacity
     * @param organizerId
     */
    public Event(String title, String description, Timestamp date, Integer capacity, String organizerId) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.capacity = capacity;
        this.organizerId = organizerId;
        this.waitingList = new ArrayList<>();
        this.participants = new ArrayList<>();
    }

    /**
     * Id getter
     *
     * @return String: event id
     */
    public String getId() {
        return id;
    }

    /**
     * Id setter
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Title getter
     *
     * @return String: Title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Title setter
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Description getter
     *
     * @return String: event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Description setter
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Date getter
     *
     * @return Timestamp: event date
     */
    public Timestamp getDate() {
        return date;
    }

    /**
     * Date setter
     *
     * @param date
     */
    public void setDate(Timestamp date) {
        this.date = date;
    }

    /**
     * Capacity getter
     *
     * @return int : capacity
     */
    public Integer getCapacity() {
        return capacity;
    }

    /**
     * Capacity setter
     *
     * @param capacity
     */
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    /**
     * OrganizerId getter
     *
     * @return String: organizerId
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * OrganizerId setter
     *
     * @param organizerId
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * WaitingList getter
     *
     * @return List<String>: waiting list
     */
    public List<String> getWaitingList() {
        return waitingList;
    }

    /**
     * WaitingList setter
     *
     * @param waitingList
     */
    public void setWaitingList(List<String> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Participants List getter
     *
     * @return List<String>: participant list
     */
    public List<String> getParticipants() {
        return participants;
    }

    /**
     * ParticipantsList setter
     *
     * @param participants
     */
    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    // Utility methods
    /**
     * Function to add user to waiting list
     *
     * @param userId
     */
    public void addToWaitingList(String userId) {
        if (!waitingList.contains(userId)) {
            waitingList.add(userId);
        }
    }

    /**
     * Function to remove user from waitingList
     *
     * @param userId
     */
    public void removeFromWaitingList(String userId) {
        waitingList.remove(userId);
    }

    /**
     * Function to add user to participants list
     *
     * @param userId
     */
    public void addParticipant(String userId) {
        if (capacity == null || participants.size() < capacity) {
            if (!participants.contains(userId)) {
                participants.add(userId);
            }
        }
    }


    /**
     * This function checks if the participants list is at capacity.
     *
     * @return Boolean
     */
    public boolean isFull() {
        if (capacity == null) {
            return false;
        }
        return participants.size() >= capacity;
    }


    /**
     * Function to return specific string for an object of type Event
     * Mostly useful for debugging
     *
     * @return String
     */
    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", date=" + date +
                ", capacity=" + capacity +
                ", participants=" + participants.size() +
                '}';
    }

}
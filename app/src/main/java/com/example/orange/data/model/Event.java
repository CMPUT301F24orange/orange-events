package com.example.orange.data.model;

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
    private String description;    private Timestamp date;
    private Timestamp startDate;
    private Timestamp endDate;
    private Timestamp registrationOpens;
    private Timestamp registrationDeadline;
    private Timestamp lotteryDrawDate;
    private Timestamp eventDate;
    private Double price;
    private Integer capacity;
    private Integer waitlistLimit;
    private String organizerId;
    private List<String> waitingList;
    private List<String> participants;
    private List<String> selectedParticipants;

    /**
     * Default constructor required for Firestone
     *
     * @see <a href="https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects">...</a>
     */
    public Event() {
        waitingList = new ArrayList<>();
        participants = new ArrayList<>();
        selectedParticipants = new ArrayList<>();

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
     * Retrieves the lottery draw date for this event.
     *
     * @return Timestamp representing the date of the lottery draw.
     */
    public Timestamp getLotteryDrawDate() { return lotteryDrawDate; }

    /**
     * Sets the lottery draw date for this event.
     *
     * @param lotteryDrawDate Timestamp representing the lottery draw date.
     */
    public void setLotteryDrawDate(Timestamp lotteryDrawDate) { this.lotteryDrawDate = lotteryDrawDate; }

    /**
     * Retrieves the final event date.
     *
     * @return Timestamp representing the date of the event.
     */
    public Timestamp getEventDate() { return eventDate; }

    /**
     * Sets the final event date.
     *
     * @param eventDate Timestamp representing the event's final date.
     */
    public void setEventDate(Timestamp eventDate) { this.eventDate = eventDate; }

    /**
     * Retrieves a list of user IDs for participants selected for this event.
     *
     * @return List<String> containing the IDs of selected participants.
     */
    public List<String> getSelectedParticipants() { return selectedParticipants; }

    /**
     * Sets the list of participants selected for this event.
     *
     * @param selectedParticipants List<String> containing IDs of selected participants.
     */
    public void setSelectedParticipants(List<String> selectedParticipants) { this.selectedParticipants = selectedParticipants; }

    /**
     * Adds a user to the selected participants list if they are not already present.
     *
     * @param userId String representing the ID of the user to be added.
     */
    public void addSelectedParticipant(String userId) {
        if (!selectedParticipants.contains(userId)) {
            selectedParticipants.add(userId);
        }
    }

    /**
     * Removes a user from the selected participants list.
     *
     * @param userId String representing the ID of the user to be removed.
     */
    public void removeSelectedParticipant(String userId) {
        selectedParticipants.remove(userId);
    }

    /**
     * Retrieves the start date of this event.
     *
     * @return Timestamp representing the event's start date.
     */
    public Timestamp getStartDate() { return startDate; }

    /**
     * Sets the start date of this event.
     *
     * @param startDate Timestamp representing the event's start date.
     */
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    /**
     * Retrieves the end date of this event.
     *
     * @return Timestamp representing the event's end date.
     */
    public Timestamp getEndDate() { return endDate; }

    /**
     * Sets the end date of this event.
     *
     * @param endDate Timestamp representing the event's end date.
     */
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    /**
     * Retrieves the date when registration for this event opens.
     *
     * @return Timestamp representing the registration open date.
     */
    public Timestamp getRegistrationOpens() { return registrationOpens; }

    /**
     * Sets the date when registration for this event opens.
     *
     * @param registrationOpens Timestamp representing the registration open date.
     */
    public void setRegistrationOpens(Timestamp registrationOpens) { this.registrationOpens = registrationOpens; }

    /**
     * Retrieves the registration deadline for this event.
     *
     * @return Timestamp representing the registration deadline.
     */
    public Timestamp getRegistrationDeadline() { return registrationDeadline; }

    /**
     * Sets the registration deadline for this event.
     *
     * @param registrationDeadline Timestamp representing the registration deadline.
     */
    public void setRegistrationDeadline(Timestamp registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    /**
     * Retrieves the price of this event.
     *
     * @return Double representing the event price.
     */
    public Double getPrice() { return price; }

    /**
     * Sets the price for this event.
     *
     * @param price Double representing the event price.
     */
    public void setPrice(Double price) { this.price = price; }

    /**
     * Retrieves the waitlist limit for this event.
     *
     * @return Integer representing the maximum number of users on the waitlist.
     */
    public Integer getWaitlistLimit() { return waitlistLimit; }

    /**
     * Sets the waitlist limit for this event.
     *
     * @param waitlistLimit Integer representing the maximum waitlist size.
     */
    public void setWaitlistLimit(Integer waitlistLimit) { this.waitlistLimit = waitlistLimit; }

    /**
     * Function to return specific string for an object of type Event
     * Mostly useful for debugging
     *
     * @author Graham Flokstra
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
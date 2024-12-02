package com.example.orange.data.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;


import android.content.Context;
import android.util.Log;

import com.example.orange.MainActivity;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.ui.notifications.EntrantNotifications;
import com.example.orange.ui.notifications.FirebaseNotifications;
import com.example.orange.utils.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class defines an Event
 *
 * @author Graham Flokstra
 */
public class Event implements Parcelable {
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
    private String qr_hash; // Add this field
    private Boolean geolocationEvent;
    private List<String> waitingList;
    private List<String> participants;
    private List<String> selectedParticipants;
    private List<String> cancelledList;
    private String eventImageId; // Changed from Blob to String ID
    private String facilityId;
    private Map<String, Map<String, Object>> location;
    Context context;

    /**
     * Default constructor required for Firestone
     *
     * @see <a href="https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects">...</a>
     */
    public Event() {
        waitingList = new ArrayList<>();
        participants = new ArrayList<>();
        selectedParticipants = new ArrayList<>();
        cancelledList = new ArrayList<>();
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


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeParcelable(date, flags);
        dest.writeParcelable(startDate, flags);
        dest.writeParcelable(endDate, flags);
        dest.writeParcelable(registrationOpens, flags);
        dest.writeParcelable(registrationDeadline, flags);
        dest.writeParcelable(lotteryDrawDate, flags);
        dest.writeParcelable(eventDate, flags);

        if (price == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(price);
        }

        if (capacity == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(capacity);
        }

        if (waitlistLimit == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(waitlistLimit);
        }

        dest.writeString(organizerId);
        dest.writeString(qr_hash);
        dest.writeValue(geolocationEvent);
        dest.writeStringList(waitingList);
        dest.writeStringList(participants);
        dest.writeStringList(selectedParticipants);
        dest.writeStringList(cancelledList);
        dest.writeString(eventImageId);
        dest.writeString(facilityId);

        // Handle location map by writing it as a Bundle (or HashMap)
        if (location != null) {
            dest.writeBundle(convertLocationToBundle(location));
        } else {
            dest.writeBundle(null);
        }
    }

    private Bundle convertLocationToBundle(Map<String, Map<String, Object>> location) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Map<String, Object>> entry : location.entrySet()) {
            Bundle innerBundle = new Bundle();
            for (Map.Entry<String, Object> innerEntry : entry.getValue().entrySet()) {
                // Assuming Object can be cast to a type, otherwise handle it accordingly
                if (innerEntry.getValue() instanceof Double) {
                    innerBundle.putDouble(innerEntry.getKey(), (Double) innerEntry.getValue());
                } else if (innerEntry.getValue() instanceof String) {
                    innerBundle.putString(innerEntry.getKey(), (String) innerEntry.getValue());
                }
                // Handle other types as needed
            }
            bundle.putBundle(entry.getKey(), innerBundle);
        }
        return bundle;
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public Event(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        date = in.readParcelable(Timestamp.class.getClassLoader());
        startDate = in.readParcelable(Timestamp.class.getClassLoader());
        endDate = in.readParcelable(Timestamp.class.getClassLoader());
        registrationOpens = in.readParcelable(Timestamp.class.getClassLoader());
        registrationDeadline = in.readParcelable(Timestamp.class.getClassLoader());
        lotteryDrawDate = in.readParcelable(Timestamp.class.getClassLoader());
        eventDate = in.readParcelable(Timestamp.class.getClassLoader());

        if (in.readByte() == 0) {
            price = null;
        } else {
            price = in.readDouble();
        }

        if (in.readByte() == 0) {
            capacity = null;
        } else {
            capacity = in.readInt();
        }

        if (in.readByte() == 0) {
            waitlistLimit = null;
        } else {
            waitlistLimit = in.readInt();
        }

        organizerId = in.readString();
        qr_hash = in.readString();
        geolocationEvent = (Boolean) in.readValue(Boolean.class.getClassLoader());
        waitingList = in.createStringArrayList();
        participants = in.createStringArrayList();
        selectedParticipants = in.createStringArrayList();
        cancelledList = in.createStringArrayList();
        eventImageId = in.readString();
        facilityId = in.readString();

        // Read the location bundle and convert it back to a Map
        Bundle locationBundle = in.readBundle(getClass().getClassLoader());
        if (locationBundle != null) {
            location = convertBundleToLocation(locationBundle);
        }
    }

    private Map<String, Map<String, Object>> convertBundleToLocation(Bundle bundle) {
        Map<String, Map<String, Object>> locationMap = new HashMap<>();

        // Iterate over the keys in the bundle
        for (String key : bundle.keySet()) {
            // Get the inner bundle for this key
            Bundle innerBundle = bundle.getBundle(key);

            if (innerBundle != null) {
                Map<String, Object> innerMap = new HashMap<>();

                // Iterate over the inner bundle's keys and add them to the inner map
                for (String innerKey : innerBundle.keySet()) {
                    Object value = null;

                    // Retrieve the appropriate type of value
                    if (innerBundle.containsKey(innerKey)) {
                        value = innerBundle.get(innerKey); // Can be Double, String, etc.
                    }

                    // Add the value to the inner map
                    if (value != null) {
                        innerMap.put(innerKey, value);
                    }
                }

                // Add the inner map to the outer map
                locationMap.put(key, innerMap);
            }
        }

        return locationMap;
    }


    @Override
    public int describeContents() {
        return 0;
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
     * Qr_hash getter
     *
     * @return String: qr_hash
     */
    public String getQr_hash() {
        return qr_hash;
    }

    /**
     * Qr_hash setter
     *
     * @param qr_hash
     */
    public void setQr_hash(String qr_hash) {
        this.qr_hash = qr_hash;
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


    public Map<String, Map<String, Object>> getLocation() {
        return location;
    }

    public void setLocation(Map<String, Map<String, Object>> location) {
        this.location = location;
    }

    /**
     * Retrieves the event's image ID.
     *
     * @return The image ID of the event.
     */
    public String getEventImageId() {
        return eventImageId;
    }

    /**
     * Sets the event's image ID.
     *
     * @param eventImageId The new image ID to set for the event.
     */
    public void setEventImageId(String eventImageId) {
        this.eventImageId = eventImageId;
    }

    /**
     * Gets the facility ID associated with the event.
     *
     * @return The facility ID.
     */
    public String getFacilityId() {
        return facilityId;
    }

    /**
     * Sets the facility ID associated with the event.
     *
     * @param facilityId The facility ID to set.
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     *
     * @author Graham Flokstra
     * @return
     */
    public Boolean getGeolocationEvent() {
        return geolocationEvent;
    }

    /**
     * Set geolocation on or off
     *
     * @author Graham Flokstra
     * @param geolocationEvent
     */
    public void setGeolocationEvent(Boolean geolocationEvent) {
        this.geolocationEvent = geolocationEvent;
    }

    // Getter and Setter for cancelledList
    public List<String> getCancelledList() {
        return cancelledList;
    }

    public void setCancelledList(List<String> cancelledList) {
        this.cancelledList = cancelledList;
    }

    /**
     * Selects users randomly from the waiting list to be invited as participants.
     *
     * @author Graham Flokstra
     * @param number The number of users to select.
     */
    public void selectParticipantsFromWaitingList(int number, Context context, Notification notification) {
        FirebaseService firebaseService = new FirebaseService();
        // Create a copy of the waiting list to avoid modifying the original list during iteration
        List<String> waitingListCopy = new ArrayList<>(waitingList);

        // Remove users who are already selected or have cancelled
        waitingListCopy.removeAll(selectedParticipants);
        waitingListCopy.removeAll(cancelledList);

        // Shuffle the list to randomize the selection
        Collections.shuffle(waitingListCopy);

        int slotsAvailable = number;
        for (String userId : waitingListCopy) {
            if (slotsAvailable == 0) {
                break;
            }
            selectedParticipants.add(userId);
            Log.d("ORANGE", userId);
            firebaseService.getUserById(userId, new FirebaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    Log.d("EntrantNotifications", user.getFcmToken());
                    EntrantNotifications entrantNotifications = new EntrantNotifications();
                    notification.setUserId(userId);
                    notification.setType(NotificationType.SELECTED_TO_PARTICIPATE);
                    firebaseService.createNotification(notification, new FirebaseCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            entrantNotifications.sendToPhone(context, "You Have Won The Lottery!", "You have just been selected to join "+title +". Choose whether to accept to decline the offer.", user, notification);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.d("EntrantNotifications", "Failed to create notification");
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.d("EntrantNotifications", "Failed to get user");
                }
            });
            // TODO: Trigger notification to userId to accept or decline.

            slotsAvailable--;
        }
    }

    /**
     * Selects users randomly from the waiting list to be invited as participants.
     * This method excludes notification logic for testing purposes.
     *
     * @author Graham Flokstra
     * @param number The number of users to select.
     */
    public void selectParticipantsFromWaitingList(int number) {
        selectParticipantsFromWaitingListInternal(number);
    }

    /**
     * Internal method that contains the core selection logic.
     *
     * @author Graham Flokstra
     * @param number The number of users to select.
     * @return List of newly selected user IDs.
     */
    private List<String> selectParticipantsFromWaitingListInternal(int number) {
        List<String> selectedUsers = new ArrayList<>();
        List<String> waitingListCopy = new ArrayList<>(waitingList);

        waitingListCopy.removeAll(selectedParticipants);
        waitingListCopy.removeAll(cancelledList);

        Collections.shuffle(waitingListCopy);

        int slotsAvailable = number;
        for (String userId : waitingListCopy) {
            if (slotsAvailable == 0) {
                break;
            }
            selectedParticipants.add(userId);
            selectedUsers.add(userId);
            slotsAvailable--;
        }
        return selectedUsers;
    }

    /**
     * Handles the acceptance of an invitation by a user.
     *
     * @author Graham Flokstra
     * @param userId The ID of the user who accepted.
     */
    public void acceptInvitation(String userId) {
        if (selectedParticipants.contains(userId)) {
            selectedParticipants.remove(userId);
            participants.add(userId);
            // Optionally, update the User object.
            // TODO: Update user's eventsParticipating list.
        }
    }

    /**
     * Handles the decline of an invitation by a user.
     * @param userId The ID of the user who declined.
     */
    public void declineInvitation(String userId) {
        if (selectedParticipants.contains(userId)) {
            selectedParticipants.remove(userId);
            waitingList.remove(userId);
            cancelledList.add(userId);
            // Optionally, update the User object.
            // TODO: Update user's eventsParticipating list.
        }
    }


    /**
     * Fills available spots by selecting new participants from the waiting list.
     *
     * @author Graham Flokstra
     * @param context
     * @param notification
     */
    public void fillSpotsFromWaitingList(Context context, Notification notification) {
        int totalConfirmed = participants.size() + selectedParticipants.size();
        int spotsNeeded = capacity - totalConfirmed;
        if (spotsNeeded > 0) {
            selectParticipantsFromWaitingList(spotsNeeded, context, notification);
        }
    }


    /**
     * Fills available spots by selecting new participants from the waiting list.
     * This method excludes notification logic for testing purposes.
     *
     * @author Graham Flokstra
     */
    public void fillSpotsFromWaitingList() {
        fillSpotsFromWaitingListInternal();
    }


    /**
     * Internal method that contains the core logic to fill spots.
     *
     * @author Graham Flokstra
     */
    private void fillSpotsFromWaitingListInternal() {
        int totalConfirmed = participants.size() + selectedParticipants.size();
        int spotsNeeded = capacity - totalConfirmed;
        if (spotsNeeded > 0) {
            selectParticipantsFromWaitingList(spotsNeeded);
        }
    }

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
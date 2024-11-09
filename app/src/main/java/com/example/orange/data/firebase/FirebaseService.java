package com.example.orange.data.firebase;

import android.util.Log;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.orange.data.model.UserSession;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseService provides methods to interact with Firebase Firestore.
 * It handles operations for users and events.
 *
 * @author graham flokstra
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private FirebaseFirestore db;
    private UserSession currentUserSession; // Hold the session of the logged-in user

    /**
     * Constructor for FirebaseService.
     * Initializes the Firestore instance.
     */
    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Retrieves a user from Firestore based on device ID and user type.
     *
     * @param deviceId The device ID of the user to retrieve.
     * @param userType The type of the user (e.g., ENTRANT, ORGANIZER, ADMIN).
     * @param callback A callback to handle the result of the operation.
     */
    public void getUserByDeviceIdAndType(String deviceId, UserType userType, FirebaseCallback<User> callback) {
        String userId = generateUserId(deviceId, userType);
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Signs in a user or organizer. If the user does not exist in Firestore, it creates a new one.
     *
     * @param deviceId The device ID of the user.
     * @param userType The type of user (ENTRANT or ORGANIZER).
     * @param callback A callback to handle the result of the sign-in operation.
     */
    public void signInUser(String deviceId, UserType userType, FirebaseCallback<Boolean> callback) {
        getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user == null) {
                    user = new User(deviceId, userType);
                    createUser(user, new FirebaseCallback<String>() {
                        @Override
                        public void onSuccess(String userId) {
                            currentUserSession = new UserSession(deviceId, userType, userId);
                            callback.onSuccess(true);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
                } else {
                    currentUserSession = new UserSession(deviceId, userType, user.getId());
                    callback.onSuccess(true);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Logs out the current user by clearing the session.
     */
    public void logOut() {
        currentUserSession = null;
    }

    /**
     * Creates a new user in Firestore.
     *
     * @param user     The User object to be created in Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void createUser(User user, FirebaseCallback<String> callback) {
        String userId = generateUserId(user.getDeviceId(), user.getUserType());
        user.setId(userId); // Set the user's ID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(userId))
                .addOnFailureListener(callback::onFailure);
    }


    /**
     * Updates an existing user in Firestore.
     *
     * @param user
     * @param callback
     */
    public void updateUser(User user, FirebaseCallback<Void> callback) {
        String userId = user.getId(); // Use the user's existing ID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }


    /**
     * Generates a userId based on deviceId and userType.
     *
     * @param deviceId
     * @param userType
     * @return String
     */
    private String generateUserId(String deviceId, UserType userType) {
        return deviceId + "_" + userType.name();
    }

    /**
     * Retrieves a user by their unique user ID from Firebase Firestore.
     *
     * @param userId   The unique identifier of the user.
     * @param callback The callback to handle the response, which provides the User object if found,
     *                 or null if the user is not found.
     */
    public void getUserById(String userId, FirebaseCallback<User> callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates a new event in Firestore.
     *
     * @param event    The Event object to be created in Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void createEvent(Event event, FirebaseCallback<String> callback) {
        DocumentReference newEventRef = db.collection("events").document();
        event.setId(newEventRef.getId());
        newEventRef.set(event)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event created successfully in Firestore");
                    callback.onSuccess(event.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create event in Firestore", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Retrieves an event from Firestore based on its ID.
     *
     * @param eventId  The ID of the event to retrieve.
     * @param callback A callback to handle the result of the operation.
     */
    public void getEventById(String eventId, FirebaseCallback<Event> callback) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Event event = documentSnapshot.toObject(Event.class);
                    callback.onSuccess(event);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Updates an existing event in Firestore.
     *
     * @param event    The Event object with updated information.
     * @param callback A callback to handle the result of the operation.
     */
    public void updateEvent(Event event, FirebaseCallback<Void> callback) {
        db.collection("events").document(event.getId()).set(event)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Deletes an event from Firestore based on its ID.
     *
     * @param eventId  The ID of the event to delete.
     * @param callback A callback to handle the result of the operation.
     */
    public void deleteEvent(String eventId, FirebaseCallback<Void> callback) {
        db.collection("events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Adds a user to the waitlist of an event.
     * @param eventId The ID of the event.
     * @param userId The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void addToEventWaitlist(String eventId, String userId, FirebaseCallback<Void> callback) {
        getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    event.addToWaitingList(userId);
                    updateEvent(event, callback);
                } else {
                    callback.onFailure(new Exception("Event not found"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Removes a user from the waitlist of an event.
     * @param eventId The ID of the event.
     * @param userId The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void removeFromEventWaitlist(String eventId, String userId, FirebaseCallback<Void> callback) {
        getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    event.removeFromWaitingList(userId);
                    updateEvent(event, callback);
                } else {
                    callback.onFailure(new Exception("Event not found"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Moves the first user from the waitlist to participants if the event is not full.
     * @param eventId The ID of the event.
     * @param callback Callback for success or failure.
     */
    public void moveFromWaitlistToParticipants(String eventId, FirebaseCallback<Void> callback) {
        getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event != null && !event.getWaitingList().isEmpty() && !event.isFull()) {
                    String userId = event.getWaitingList().get(0);
                    event.removeFromWaitingList(userId);
                    event.addParticipant(userId);
                    updateEvent(event, callback);
                } else {
                    callback.onFailure(new Exception("No users in waitlist or event is full"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Retrieves all events from Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void getAllEvents(FirebaseCallback<List<Event>> callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Retrieves a list of events that the current user is participating in or is on the waitlist for.
     *
     * @author Graham Flokstra
     * @param userId   String representing the unique ID of the current user.
     * @param callback FirebaseCallback<List<Event>> to handle the result, providing a list of Event objects
     *                 the user is associated with (either in the participants or waiting list).
     */
    public void getUserEvents(String userId, FirebaseCallback<List<Event>> callback) {
        db.collection("events")
                .whereArrayContainsAny("waitingList", Collections.singletonList(userId))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes a specified user from the list of participants in a given event.
     * This function enables the "Leave Event" functionality, allowing users to leave events they have joined.
     *
     * @author Graham Flokstra
     * @param eventId  String representing the unique ID of the event from which the user will be removed.
     * @param userId   String representing the unique ID of the user to be removed from the participants list.
     * @param callback FirebaseCallback<Void> to handle the success or failure of the operation.
     */
    public void removeFromEventParticipants(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.update("participants", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Adds a specified user to the selected participants list for an event.
     * This is typically used for users chosen through a lottery system.
     *
     * @author Graham Flokstra
     * @param eventId  String representing the unique ID of the event.
     * @param userId   String representing the unique ID of the user to be added to the selected participants list.
     * @param callback FirebaseCallback<Void> to handle the success or failure of the operation.
     */
    public void addToSelectedParticipants(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.update("selectedParticipants", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes a specified user from the selected participants list for an event.
     * This may be used if a user decides not to participate or if they are no longer eligible.
     *
     * @author Graham Flokstra
     * @param eventId  String representing the unique ID of the event.
     * @param userId   String representing the unique ID of the user to be removed from the selected participants list.
     * @param callback FirebaseCallback<Void> to handle the success or failure of the operation.
     */
    public void removeFromSelectedParticipants(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.update("selectedParticipants", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Retrieves all events created by a specified organizer from Firebase.
     *
     * @author Graham Flokstra
     * @param organizerId String representing the unique ID of the organizer.
     * @param callback    FirebaseCallback<List<Event>> to handle the result, providing a list of Event objects.
     */
    public void getOrganizerEvents(String organizerId, FirebaseCallback<List<Event>> callback) {
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }
    public void storeEventHash(String eventId, String hash) {
        Map<String, Object> hashData = new HashMap<>();
        hashData.put("qr_hash", hash);

        db.collection("events").document(eventId)
                .set(hashData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FirebaseService", "Hash stored successfully"))
                .addOnFailureListener(e -> Log.w("FirebaseService", "Error storing hash", e));
    }

    /**
     * Retrieves a list of entrants on the waiting list for a specific event.
     *
     *
     * @param eventId  The ID of the event to retrieve waitlist details.
     * @param callback Callback to handle the result of the operation.
     */
    public void getEventWaitlist(String eventId, FirebaseCallback<List<String>> callback) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null && event.getWaitingList() != null) {
                            callback.onSuccess(event.getWaitingList());
                        } else {
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        callback.onFailure(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates a new facility in Firestore.
     *
     * @author Graham Flokstra
     * @param facility The Facility object to be created in Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void createFacility(Facility facility, FirebaseCallback<String> callback) {
        DocumentReference newFacilityRef = db.collection("facilities").document();
        facility.setId(newFacilityRef.getId());
        newFacilityRef.set(facility)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Facility created successfully in Firestore");
                    callback.onSuccess(facility.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create facility in Firestore", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Updates an existing facility in Firestore.
     *
     * @author Graham Flokstra
     * @param facility The Facility object with updated information.
     * @param callback A callback to handle the result of the operation.
     */
    public void updateFacility(Facility facility, FirebaseCallback<Void> callback) {
        db.collection("facilities").document(facility.getId()).set(facility)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Retrieves a facility from Firestore based on its ID.
     *
     * @author Graham Flokstra
     * @param facilityId The ID of the facility to retrieve.
     * @param callback   A callback to handle the result of the operation.
     */
    public void getFacilityById(String facilityId, FirebaseCallback<Facility> callback) {
        db.collection("facilities").document(facilityId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Facility facility = documentSnapshot.toObject(Facility.class);
                        callback.onSuccess(facility);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Retrieves all facilities from Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void getAllFacilities(FirebaseCallback<List<Facility>> callback) {
        db.collection("facilities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Facility> facilities = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Facility facility = document.toObject(Facility.class);
                        if (facility != null) {
                            facilities.add(facility);
                        }
                    }
                    callback.onSuccess(facilities);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes a facility from Firestore based on its ID.
     *
     * @author Radhe Patel
     * @param facilityId  The ID of the facility to delete.
     * @param callback A callback to handle the result of the operation.
     */
    public void deleteFacility(String facilityId, FirebaseCallback<Void> callback) {
        db.collection("facilities").document(facilityId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // Inside FirebaseService
    public void deleteFacilityAndRelatedEvents(String facilityId, FirebaseCallback<Void> callback) {
        // First, delete related events
        db.collection("events")
                .whereEqualTo("facilityId", facilityId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        document.getReference().delete();
                    }

                    // Then, delete the facility itself
                    db.collection("facilities").document(facilityId).delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates the event image in Firestore.
     *
     * @author Graham Flokstra
     * @param eventId   The ID of the event to update.
     * @param imageData Byte array representing the image data.
     * @param callback  Callback to handle success or failure.
     */
    public void updateEventImage(String eventId, byte[] imageData, FirebaseCallback<Void> callback) {
        // Convert byte array to Blob
        Blob imageBlob = Blob.fromBytes(imageData);

        // Update the event image in Firestore
        db.collection("events").document(eventId)
                .update("eventImageData", imageBlob)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event image updated successfully in Firestore");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update event image in Firestore", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Removes the event image from Firestore.
     *
     * @author Graham Flokstra
     * @param eventId  The ID of the event to update.
     * @param callback Callback to handle success or failure.
     */
    public void removeEventImage(String eventId, FirebaseCallback<Void> callback) {
        // Set eventImageData to null
        db.collection("events").document(eventId)
                .update("eventImageData", null)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event image removed successfully in Firestore");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove event image in Firestore", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Retrieves all users from Firestore.
     * @author Viral Bhavsar
     * @param callback A callback to handle the result of the operation.
     */
    public void getAllUsers(FirebaseCallback<List<User>> callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes the User from the database, if it has a facility and events associated with it,
     * it also deletes those.
     *
     * @author Viral Bhavsar
     * @param userId  The ID of the user to delete.
     * @param callback Callback to handle success or failure.
     */
    public void deleteUserAndRelatedFacilities(String userId, FirebaseCallback<Void> callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        String facilityId = userSnapshot.getString("facilityId");

                        if (facilityId != null) {
                            db.collection("events").whereEqualTo("facilityId", facilityId).get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                            document.getReference().delete();
                                        }

                                        db.collection("facilities").document(facilityId).delete()
                                                .addOnSuccessListener(aVoid -> {

                                                    db.collection("users").document(userId).delete()
                                                            .addOnSuccessListener(aVoid2 -> callback.onSuccess(null))
                                                            .addOnFailureListener(callback::onFailure);
                                                })
                                                .addOnFailureListener(callback::onFailure);
                                    })
                                    .addOnFailureListener(callback::onFailure);

                        } else {
                            db.collection("users").document(userId).delete()
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                    .addOnFailureListener(callback::onFailure);
                        }
                    } else {
                        callback.onFailure(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Draws entrants from the event's waitlist and adds them to the selected participants list.
     * If the event has a capacity limit, the number of selected entrants should not exceed that limit.
     *
     * @param eventId The ID of the event.
     * @param callback Callback for success or failure.
     */
    public void drawFromWaitlist(String eventId, FirebaseCallback<Void> callback) {
        getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event != null && event.getWaitingList() != null && !event.getWaitingList().isEmpty()) {
                    int eventLimit = (event.getCapacity() != null) ? event.getCapacity() : Integer.MAX_VALUE;
                    List<String> waitlist = new ArrayList<>(event.getWaitingList());
                    Collections.shuffle(waitlist);

                    int numToSelect = Math.min(eventLimit - event.getParticipants().size(), waitlist.size());

                    if (numToSelect > 0) {
                        List<String> selectedParticipants = waitlist.subList(0, numToSelect);

                        for (String userId : selectedParticipants) {
                            event.removeFromWaitingList(userId);
                            event.addParticipant(userId);
                        }

                        updateEvent(event, callback);
                    } else {
                        callback.onFailure(new Exception("Event is already full or no participants to draw"));
                    }
                } else {
                    callback.onFailure(new Exception("No users in the waitlist or event is not found"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }


}
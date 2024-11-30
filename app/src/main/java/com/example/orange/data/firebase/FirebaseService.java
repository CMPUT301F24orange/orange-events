package com.example.orange.data.firebase;

import android.util.Log;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;
import com.example.orange.data.model.ImageData;
import com.example.orange.data.model.Notification;
import com.example.orange.data.model.NotificationType;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.orange.data.model.UserSession;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

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
        return deviceId + "_" + userType.toString();
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
     * Creates a new event in Firestore and updates the organizer's eventsOrganizing list atomically.
     *
     * @author Graham Flokstra
     *
     * @param event    The Event object to be created in Firestore.
     * @param userId   The ID of the organizer creating the event.
     * @param callback A callback to handle the result of the operation.
     */
    public void createEvent(Event event, String userId, FirebaseCallback<String> callback) {
        DocumentReference newEventRef = db.collection("events").document();
        event.setId(newEventRef.getId());

        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the user document
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            if (!userSnapshot.exists()) {
                throw new FirebaseServiceException("Organizer does not exist");
            }

            // Optionally, verify that the user is an organizer
            User user = userSnapshot.toObject(User.class);
            if (user == null || user.getUserType() != UserType.ORGANIZER) {
                throw new FirebaseServiceException("User is not an organizer");
            }

            // Set organizerId and facilityId in the event
            event.setOrganizerId(user.getId());
            event.setFacilityId(user.getFacilityId());

            // Create the event document
            transaction.set(newEventRef, event);

            // Update the user's eventsOrganizing list
            transaction.update(userRef, "eventsOrganizing", FieldValue.arrayUnion(event.getId()));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Event created successfully in Firestore and organizer's list updated");
            callback.onSuccess(event.getId());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to create event in Firestore", e);
            callback.onFailure(e);
        });
    }

    /**
     * Custom exception class for FirebaseService-related errors.
     */
    public static class FirebaseServiceException extends RuntimeException {
        public FirebaseServiceException(String message) {
            super(message);
        }
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
        // First, retrieve the event to get the eventImageId
        getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    String imageId = event.getEventImageId();
                    // Proceed to delete the event
                    db.collection("events").document(eventId).delete()
                            .addOnSuccessListener(aVoid -> {
                                // If there's an associated image, delete it
                                if (imageId != null) {
                                    deleteImage(imageId, new FirebaseCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            callback.onSuccess(null);
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            callback.onFailure(e);
                                        }
                                    });
                                } else {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(callback::onFailure);
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
     * Retrieves all events associated with a user, whether they are in the waitlist,
     * selected participants, or confirmed participants.     *
     * @author Graham Flokstra
     * @param userId   String representing the unique ID of the current user.
     * @param callback FirebaseCallback<List<Event>> to handle the result, providing a list of Event objects
     *                 the user is associated with (either in the participants or waiting list).
     */

    public void getUserEvents(String userId, FirebaseCallback<List<Event>> callback) {
        List<Event> combinedEvents = new ArrayList<>();

        // Query 1: Events where the user is in the waitingList
        Task<QuerySnapshot> waitlistTask = db.collection("events")
                .whereArrayContains("waitingList", userId)
                .get();

        // Query 2: Events where the user is in the selectedParticipants
        Task<QuerySnapshot> selectedTask = db.collection("events")
                .whereArrayContains("selectedParticipants", userId)
                .get();

        // Query 3: Events where the user is in the participants
        Task<QuerySnapshot> participantsTask = db.collection("events")
                .whereArrayContains("participants", userId)
                .get();

        // Execute all queries asynchronously
        Tasks.whenAllComplete(waitlistTask, selectedTask, participantsTask)
                .addOnSuccessListener(task -> {
                    for (Task<?> individualTask : task) {
                        if (individualTask.isSuccessful()) {
                            QuerySnapshot snapshot = ((Task<QuerySnapshot>) individualTask).getResult();
                            for (DocumentSnapshot document : snapshot.getDocuments()) {
                                Event event = document.toObject(Event.class);
                                if (event != null && !combinedEvents.contains(event)) {
                                    combinedEvents.add(event);
                                }
                            }
                        } else {
                            // Log individual query failures but continue processing
                            Log.e("FirebaseService", "Error fetching user events: " + individualTask.getException());
                        }
                    }

                    // Logging for debugging
                    Log.d("FirebaseService", "Total Events Found: " + combinedEvents.size());
                    for (Event event : combinedEvents) {
                        Log.d("FirebaseService", "Event ID: " + event.getId());
                        Log.d("FirebaseService", "Participants: " + event.getParticipants());
                        Log.d("FirebaseService", "Selected Participants: " + event.getSelectedParticipants());
                        Log.d("FirebaseService", "Waitlist: " + event.getWaitingList());
                    }

                    callback.onSuccess(combinedEvents);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseService", "Error fetching user events", e);
                    callback.onFailure(e);
                });
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
     * Adds a user to the waitlist of an event and updates the user's waitlisted events.
     *
     * @param eventId  The ID of the event to join.
     * @param userId   The ID of the user joining the event.
     * @param callback A callback to handle success or failure.
     */
    public void joinEventWaitlist(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the event and user documents
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            if (!eventSnapshot.exists()) {
                throw new FirebaseServiceException("Event does not exist.");
            }

            if (!userSnapshot.exists()) {
                throw new FirebaseServiceException("User does not exist.");
            }

            Event event = eventSnapshot.toObject(Event.class);
            User user = userSnapshot.toObject(User.class);

            if (event == null || user == null) {
                throw new FirebaseServiceException("Failed to parse Event or User data.");
            }

            // Check if the user is already a participant or on the waitlist
            if (event.getParticipants().contains(userId)) {
                throw new FirebaseServiceException("You are already a participant of this event.");
            }

            if (event.getWaitingList().contains(userId)) {
                throw new FirebaseServiceException("You are already on the waitlist for this event.");
            }

            // Add user to the event's waiting list
            transaction.update(eventRef, "waitingList", FieldValue.arrayUnion(userId));

            // Add event to the user's eventsWaitlisted
            transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayUnion(eventId));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully added to waitlist");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error adding user to waitlist", e);
            callback.onFailure(e);
        });
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
                        Event event = document.toObject(Event.class);
                        String eventId = document.getId();
                        String imageId = event != null ? event.getEventImageId() : null;

                        // Delete the event document
                        document.getReference().delete();

                        // Delete the associated image if it exists
                        if (imageId != null) {
                            deleteImage(imageId, new FirebaseCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    // Image deleted
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    // Handle failure
                                }
                            });
                        }
                    }

                    // Then, delete the facility itself
                    db.collection("facilities").document(facilityId).delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
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
        // Retrieve the user document
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        String facilityId = userSnapshot.getString("facilityId");
                        String profileImageId = userSnapshot.getString("profileImageId");

                        // If the user has a facility ID, delete related facilities and events
                        if (facilityId != null) {
                            deleteFacilityAndRelatedEvents(facilityId, new FirebaseCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    // After deleting facilities and events, delete the user and profile image
                                    deleteUserAndProfileImage(userId, profileImageId, callback);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    callback.onFailure(e);
                                }
                            });
                        } else {
                            // If no facility, delete the user and profile image directly
                            deleteUserAndProfileImage(userId, profileImageId, callback);
                        }
                    } else {
                        callback.onFailure(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes the user document and their associated profile image from Firestore.
     *
     * @param userId         The ID of the user to delete.
     * @param profileImageId The ID of the profile image to delete (if any).
     * @param callback       Callback to handle success or failure of the operation.
     */
    private void deleteUserAndProfileImage(String userId, String profileImageId, FirebaseCallback<Void> callback) {
        // Delete the user document
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (profileImageId != null) {
                        // If the user has a profile image, delete it from the images collection
                        deleteImage(profileImageId, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                callback.onSuccess(null); // User and image successfully deleted
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(new Exception("User deleted, but failed to delete profile image", e));
                            }
                        });
                    } else {
                        callback.onSuccess(null); // User deleted, no profile image to delete
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }


    /**
     * Creates a new image in Firestore.
     *
     * @param imageData The image data as a Blob.
     * @param callback  A callback to handle the result of the operation.
     */
    public void createImage(Blob imageData, FirebaseCallback<String> callback) {
        DocumentReference newImageRef = db.collection("images").document();
        ImageData image = new ImageData(newImageRef.getId(), imageData);
        newImageRef.set(image)
                .addOnSuccessListener(aVoid -> callback.onSuccess(image.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Retrieves an image by its ID from Firestore.
     *
     * @param imageId  The ID of the image to retrieve.
     * @param callback A callback to handle the result of the operation.
     */
    public void getImageById(String imageId, FirebaseCallback<ImageData> callback) {
        db.collection("images").document(imageId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ImageData image = documentSnapshot.toObject(ImageData.class);
                        callback.onSuccess(image);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes an image from Firestore based on its ID.
     *
     * @param imageId  The ID of the image to delete.
     * @param callback A callback to handle the result of the operation.
     */
    public void deleteImage(String imageId, FirebaseCallback<Void> callback) {
        db.collection("images").document(imageId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes a users profile picture from the database based on the user ID
     *
     * @author Viral Bhavsar
     * @param userId The unique user Id
     * @param callback A callback to handle the result of the operation.
     */
    public void deleteUserProfilePicture(String userId, FirebaseCallback<Void> callback){
        if (userId == null || userId.isEmpty()){
            //Handling the case where the userID is invalid
            callback.onFailure(new Exception("User ID is null or empty"));
            return;
        }

        // Getting the user's document from firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()){
                        //Retrieve the profile image ID from the user's document
                        String profileImageId = documentSnapshot.getString("profileImageId");

                        if (profileImageId != null && !profileImageId.isEmpty()){
                            // If the user has a profile image ID, delete it
                            deleteImage(profileImageId, new FirebaseCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    db.collection("users").document(userId)
                                            .update("profileImageId", null)
                                            .addOnSuccessListener(aVoid -> {
                                                // Successfully updated the user's profileImageId field
                                                callback.onSuccess(null);
                                            })
                                            .addOnFailureListener(e -> {
                                                // Handle the failure to update the user's document
                                                callback.onFailure(new Exception("Failed to update user profileImageId", e));
                                            });
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    // Handle failure to delete the image
                                    callback.onFailure(new Exception("Failed to delete profile image", e));
                                }
                            });
                        } else {
                            // if no profile image exists, return success
                            callback.onSuccess(null);
                        }
                    } else {
                        // Handle case where the user document doesn't exist
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener( e -> callback.onFailure(new Exception("Failed to retrieve user document.", e)));
    }

    /**
     * Deletes an event poster from the database based on the event ID
     *
     * @author Radhe Patel
     *
     * @param eventId The unique event Id
     * @param callback A callback to handle the result of the operation.
     */
    public void deletePosterAdmin(String eventId, FirebaseCallback<Void> callback){
        if (eventId == null || eventId.isEmpty()){
            //Handling the case where the eventId is invalid
            callback.onFailure(new Exception("Event ID is null or empty"));
            return;
        }

        // Getting the event's document from firestore
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()){
                        //Retrieve the event image ID from the event's document
                        String eventImageId = documentSnapshot.getString("eventImageId");

                        if (eventImageId != null && !eventImageId.isEmpty()){
                            // If the event has a event image ID, delete it
                            deleteImage(eventImageId, new FirebaseCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    db.collection("events").document(eventId)
                                            .update("eventImageId", null)
                                            .addOnSuccessListener(aVoid -> {
                                                // Successfully updated the events eventImageId field
                                                callback.onSuccess(null);
                                            })
                                            .addOnFailureListener(e -> {
                                                // Handle the failure to update the event's document
                                                callback.onFailure(new Exception("Failed to update event eventImageId", e));
                                            });
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    // Handle failure to delete the image
                                    callback.onFailure(new Exception("Failed to delete event image", e));
                                }
                            });
                        } else {
                            // if no event image exists, return success
                            callback.onSuccess(null);
                        }
                    } else {
                        // Handle case where the event document doesn't exist
                        callback.onFailure(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener( e -> callback.onFailure(new Exception("Failed to retrieve event document.", e)));
    }

    /**
     * Deletes an event QR hashed data from the database based on the event ID
     *
     * @author Radhe Patel
     *
     * @param eventId The unique event Id
     * @param callback A callback to handle the result of the operation.
     */
    public void deleteQR(String eventId, FirebaseCallback<Void> callback){
        if (eventId == null || eventId.isEmpty()){
            // Handling the case where the eventId is invalid
            callback.onFailure(new Exception("Event ID is null or empty"));
            return;
        }

        // Getting the event's document from firestore
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()){
                        // Retrieve the event hashed data from the event's document if the field exists
                        if (documentSnapshot.contains("qr_hash")) {

                            String eventQR = documentSnapshot.getString("qr_hash");

                            if (eventQR != null && !eventQR.isEmpty()){
                                // If the event has hashed qr code data, delete it
                                deleteImage(eventQR, new FirebaseCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        db.collection("events").document(eventId)
                                                .update("qr_hash", null)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Successfully updated the events qr_hash field
                                                    callback.onSuccess(null);
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Handle the failure to update the event's document
                                                    callback.onFailure(new Exception("Failed to update event qr_hash", e));
                                                });
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        // Handle failure to delete hashed QR code data
                                        callback.onFailure(new Exception("Failed to delete event hashed QR code data", e));
                                    }
                                });
                            } else {
                                // if no hashed QR code data exists, return success
                                callback.onSuccess(null);
                            }
                        } else {
                            // Handle case where the event document has not created a qr code yet (no field qr_hash)
                            callback.onSuccess(null);
                        }
                    } else {
                        // Handle case where the event document doesn't exist
                        callback.onFailure(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener( e -> callback.onFailure(new Exception("Failed to retrieve event document.", e)));
    }

    /**
     * Adds a user to an event's waitlist and updates the user's eventsWaitlisted list.
     * Assumes that event existence and capacity have been validated beforehand.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void addUserToWaitlist(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the event and user documents
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            // Proceed only if both documents exist
            if (eventSnapshot.exists() && userSnapshot.exists()) {
                // Add user to event's waiting list
                transaction.update(eventRef, "waitingList", FieldValue.arrayUnion(userId));

                // Add event to user's eventsWaitlisted
                transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayUnion(eventId));
            }
            // If either document doesn't exist, do nothing
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully added to waitlist");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error adding user to waitlist", e);
            callback.onFailure(e);
        });
    }

    /**
     * Removes a user from an event's waitlist and updates the user's eventsWaitlisted list.
     * Assumes that event existence and user's waitlist status have been validated beforehand.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void removeUserFromWaitlist(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the event and user documents
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            // Proceed only if both documents exist
            if (eventSnapshot.exists() && userSnapshot.exists()) {
                // Remove user from event's waiting list
                transaction.update(eventRef, "waitingList", FieldValue.arrayRemove(userId));

                // Remove event from user's eventsWaitlisted
                transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayRemove(eventId));
            }
            // If either document doesn't exist, do nothing
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully removed from waitlist");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error removing user from waitlist", e);
            callback.onFailure(e);
        });
    }

    /**
     * Moves a user from the waitlist to participants and updates both Event and User documents.
     * Assumes that event is not full and user is on the waitlist.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void moveUserFromWaitlistToParticipants(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the event and user documents
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            // Proceed only if both documents exist
            if (eventSnapshot.exists() && userSnapshot.exists()) {
                // Move user from waitingList to participants
                transaction.update(eventRef, "waitingList", FieldValue.arrayRemove(userId));
                transaction.update(eventRef, "participants", FieldValue.arrayUnion(userId));

                // Update user's lists
                transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayRemove(eventId));
                transaction.update(userRef, "eventsParticipating", FieldValue.arrayUnion(eventId));
            }
            // If either document doesn't exist, do nothing
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully moved from waitlist to participants");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error moving user from waitlist to participants", e);
            callback.onFailure(e);
        });
    }

    /**
     * Cancels a user's participation in an event, moving them to the cancelled list and updating User lists.
     * Assumes that user is either a participant, selected participant, or waitlisted.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void cancelUserParticipation(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the event and user documents
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            // Proceed only if both documents exist
            if (eventSnapshot.exists() && userSnapshot.exists()) {
                // Remove user from all possible lists
                transaction.update(eventRef, "participants", FieldValue.arrayRemove(userId));
                transaction.update(eventRef, "selectedParticipants", FieldValue.arrayRemove(userId));
                transaction.update(eventRef, "waitingList", FieldValue.arrayRemove(userId));

                // Add user to cancelledList in Event
                transaction.update(eventRef, "cancelledList", FieldValue.arrayUnion(userId));

                // Add event to user's eventsCancelled
                transaction.update(userRef, "eventsCancelled", FieldValue.arrayUnion(eventId));

                // Remove event from user's other lists
                transaction.update(userRef, "eventsParticipating", FieldValue.arrayRemove(eventId));
                transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayRemove(eventId));
            }
            // If either document doesn't exist, do nothing
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully cancelled participation");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error cancelling user participation", e);
            callback.onFailure(e);
        });
    }

    /**
     * Updates both Event and User documents when a user accepts an invitation to participate.
     * Assumes that event is not full and user is a selected participant.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void acceptEventInvitation(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            if (eventSnapshot.exists() && userSnapshot.exists()) {
                // Move user from selectedParticipants to participants
                transaction.update(eventRef, "selectedParticipants", FieldValue.arrayRemove(userId));
                transaction.update(eventRef, "participants", FieldValue.arrayUnion(userId));

                // Update user's lists
                transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayRemove(eventId));
                transaction.update(userRef, "eventsParticipating", FieldValue.arrayUnion(eventId));
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully accepted event invitation");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error accepting event invitation", e);
            callback.onFailure(e);
        });
    }


    /**
     * Updates both Event and User documents when a user declines an invitation to participate.
     * Assumes that user is a selected participant.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user.
     * @param callback Callback for success or failure.
     */
    public void declineEventInvitation(String eventId, String userId, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            // Retrieve the event and user documents
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            // Proceed only if both documents exist
            if (eventSnapshot.exists() && userSnapshot.exists()) {
                // Remove user from selectedParticipants and add to cancelledList
                transaction.update(eventRef, "selectedParticipants", FieldValue.arrayRemove(userId));
                transaction.update(eventRef, "cancelledList", FieldValue.arrayUnion(userId));

                // Update user's lists
                transaction.update(userRef, "eventsWaitlisted", FieldValue.arrayRemove(eventId));
                transaction.update(userRef, "eventsCancelled", FieldValue.arrayUnion(eventId));
            }
            // If either document doesn't exist, do nothing
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User successfully declined event invitation");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error declining event invitation", e);
            callback.onFailure(e);
        });
    }
    public void moveUsersToSelectedParticipants(String eventId, List<String> selectedUsers, FirebaseCallback<Void> callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new FirebaseServiceException("Event not found.");
            }

            Event event = snapshot.toObject(Event.class);

            if (event == null) {
                throw new FirebaseServiceException("Failed to parse event data.");
            }

            List<String> selectedParticipants = event.getSelectedParticipants() != null ? new ArrayList<>(event.getSelectedParticipants()) : new ArrayList<>();
            List<String> waitlist = event.getWaitingList() != null ? new ArrayList<>(event.getWaitingList()) : new ArrayList<>();

            // Add selected users to selectedParticipants and remove from waitlist
            for (String userId : selectedUsers) {
                if (waitlist.contains(userId)) {
                    selectedParticipants.add(userId);
                    waitlist.remove(userId);
                }
            }

            // Update Firestore document
            transaction.update(eventRef, "selectedParticipants", selectedParticipants);
            transaction.update(eventRef, "waitingList", waitlist);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully moved users to selectedParticipants.");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error moving users to selectedParticipants.", e);
            callback.onFailure(e);
        });
    }


    /* --------------- Notifications ---------------- */



    /**
     * Creates a new notification in Firestore.
     *
     * @param notification The Notification object to be created.
     * @param callback     A callback to handle the result of the operation.
     */
    public void createNotification(Notification notification, FirebaseCallback<String> callback) {
        DocumentReference newNotificationRef = db.collection("notifications").document();
        notification.setId(newNotificationRef.getId());
        db.collection("notifications").document(notification.getId())
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification created successfully");
                    callback.onSuccess(notification.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Retrieves a notification by its ID.
     *
     * @param notificationId The ID of the notification.
     * @param callback       A callback to handle the result of the operation.
     */
    public void getNotificationById(String notificationId, FirebaseCallback<Notification> callback) {
        db.collection("notifications").document(notificationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Notification notification = documentSnapshot.toObject(Notification.class);
                        callback.onSuccess(notification);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve notification", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Retrieves all notifications for a specific user.
     *
     * @param userId   The ID of the user.
     * @param callback A callback to handle the result of the operation.
     */
    public void getNotificationsForUser(String userId, FirebaseCallback<List<Notification>> callback) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = document.toObject(Notification.class);
                        if (notification != null) {
                            notifications.add(notification);
                        }
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve notifications for user", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Updates a notification in Firestore.
     *
     * @param notification The Notification object with updated data.
     * @param callback     A callback to handle the result of the operation.
     */
    public void updateNotification(Notification notification, FirebaseCallback<Void> callback) {
        db.collection("notifications").document(notification.getId())
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification updated successfully");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update notification", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Deletes a notification from Firestore.
     *
     * @param notificationId The ID of the notification to delete.
     * @param callback       A callback to handle the result of the operation.
     */
    public void deleteNotification(String notificationId, FirebaseCallback<Void> callback) {
        db.collection("notifications").document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted successfully");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete notification", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Creates notifications for selected and unselected users after drawing participants.
     *
     * @param eventId          The ID of the event.
     * @param selectedUserIds  List of user IDs who were selected.
     * @param unselectedUserIds List of user IDs who were not selected.
     * @param callback         A callback to handle the result of the operation.
     */
    public void createDrawNotifications(String eventId, List<String> selectedUserIds, List<String> unselectedUserIds, FirebaseCallback<Void> callback) {
        // Create a list to hold all notification creation tasks
        List<Notification> notifications = new ArrayList<>();

        // Create notifications for selected users
        for (String userId : selectedUserIds) {
            Notification notification = new Notification(eventId, userId, NotificationType.SELECTED_TO_PARTICIPATE);
            notifications.add(notification);
        }

        // Create notifications for unselected users
        for (String userId : unselectedUserIds) {
            Notification notification = new Notification(eventId, userId, NotificationType.NOT_SELECTED);
            notifications.add(notification);
        }

        // Batch write to Firestore
        WriteBatch batch = db.batch();
        CollectionReference notificationsRef = db.collection("notifications");

        for (Notification notification : notifications) {
            DocumentReference docRef = notificationsRef.document();
            notification.setId(docRef.getId());
            batch.set(docRef, notification);
        }

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "All notifications created successfully.");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notifications.", e);
                    callback.onFailure(e);
                });
    }




}
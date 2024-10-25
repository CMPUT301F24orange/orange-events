package com.example.orange.data.firebase;

import android.util.Log;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.orange.data.model.UserSession;

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
        String userId = generateUserId(user.getDeviceId(), user.getUserType());
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
}

package com.example.orange.data.firebase;

import android.util.Log;

import com.example.orange.data.model.Event;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * FirebaseService provides methods to interact with Firebase Firestore.
 * It handles operations for users and events.
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private FirebaseFirestore db;

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
        Log.d(TAG, "Starting getUserByDeviceIdAndType for deviceId: " + deviceId);

        Query query = db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("userType", userType)
                .limit(1);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().isEmpty()) {
                    User user = task.getResult().getDocuments().get(0).toObject(User.class);
                    Log.d(TAG, "User retrieved successfully");
                    callback.onSuccess(user);
                } else {
                    Log.d(TAG, "User not found");
                    callback.onSuccess(null);
                }
            } else {
                Log.e(TAG, "Error in getUserByDeviceIdAndType", task.getException());
                callback.onFailure(task.getException());
            }
        });
    }

    /**
     * Creates a new user in Firestore.
     *
     * @param user     The User object to be created in Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void createUser(User user, FirebaseCallback<String> callback) {
        Log.d(TAG, "Attempting to create user: " + user.getDeviceId());
        db.collection("users").add(user)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    user.setId(id);
                    Log.d(TAG, "User created successfully with ID: " + id);
                    documentReference.set(user)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User data updated with ID");
                                callback.onSuccess(id);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update user with ID", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user", e);
                    callback.onFailure(e);
                });
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
                        callback.onSuccess(null);  // Handle user not found
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates the user details in Firebase Firestore.
     *
     * @param userId   The unique identifier of the user to be updated.
     * @param user     The User object containing the updated information.
     * @param callback The callback to handle the response, which provides success if the user details
     *                 are updated successfully, or an error if the update fails.
     */
    public void updateUser(String userId, User user, FirebaseCallback<Void> callback) {
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
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
                .addOnSuccessListener(aVoid -> callback.onSuccess(event.getId()))
                .addOnFailureListener(callback::onFailure);
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

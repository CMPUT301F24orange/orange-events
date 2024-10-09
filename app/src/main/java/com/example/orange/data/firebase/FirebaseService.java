package com.example.orange.data.firebase;

import android.util.Log;

import com.example.orange.data.model.Event;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * FirebaseService provides methods to interact with Firebase Firestore.
 * It handles operations for users and events.
 */
public class FirebaseService {
    private FirebaseFirestore db;
    private static final String TAG = "FirebaseService";
    private final ExecutorService executorService;

    /**
     * Constructor for FirebaseService.
     * Initializes the Firestore instance and creates a single-threaded executor.
     */
    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Retrieves a user from Firestore based on username and user type.
     *
     * @param username The username of the user to retrieve.
     * @param userType The type of the user (e.g., ENTRANT, ORGANIZER, ADMIN).
     * @param callback A callback to handle the result of the operation.
     */
    public void getUserByUsernameAndType(String username, UserType userType, FirebaseCallback<User> callback) {
        Log.d(TAG, "Starting getUserByUsernameAndType for username: " + username);
        long startTime = System.currentTimeMillis();

        Query query = db.collection("users")
                .whereEqualTo("username", username)
                .whereEqualTo("userType", userType)
                .limit(1);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().isEmpty()) {
                    User user = task.getResult().getDocuments().get(0).toObject(User.class);
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "getUserByUsernameAndType completed in " + (endTime - startTime) + " ms");
                    callback.onSuccess(user);
                } else {
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "getUserByUsernameAndType completed in " + (endTime - startTime) + " ms. User not found.");
                    callback.onSuccess(null);
                }
            } else {
                Log.e(TAG, "Error in getUserByUsernameAndType", task.getException());
                callback.onFailure(task.getException());
            }
        });
    }

    /**
     * Creates a new user in Firestore.
     *
     * @param user The User object to be created in Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void createUser(User user, FirebaseCallback<String> callback) {
        Log.d(TAG, "Attempting to create user: " + user.getUsername());
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
     * Creates a new event in Firestore.
     *
     * @param event The Event object to be created in Firestore.
     * @param callback A callback to handle the result of the operation.
     */
    public void createEvent(Event event, FirebaseCallback<String> callback) {
        DocumentReference newEventRef = db.collection("events").document();
        event.setId(newEventRef.getId());
        newEventRef.set(event)
                .addOnSuccessListener(aVoid -> callback.onSuccess(event.getId()))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Retrieves an event from Firestore based on its ID.
     *
     * @param eventId The ID of the event to retrieve.
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
     * @param event The Event object with updated information.
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
     * @param eventId The ID of the event to delete.
     * @param callback A callback to handle the result of the operation.
     */
    public void deleteEvent(String eventId, FirebaseCallback<Void> callback) {
        db.collection("events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e));
    }
}
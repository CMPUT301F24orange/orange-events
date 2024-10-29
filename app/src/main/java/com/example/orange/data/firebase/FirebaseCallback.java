package com.example.orange.data.firebase;

/**
 * Interface for handling asynchronous Firebase operations.\
 *
 * @param <T> The type of result expected from the Firebase operation.
 * @author graham flokstra
 */
public interface FirebaseCallback<T> {
    /**
     * Called when the Firebase operation is successful.
     * @param result The result of the Firebase operation.
     */
    void onSuccess(T result);

    /**
     * Called when the Firebase operation fails.
     * @param e The exception thrown during the Firebase operation.
     */
    void onFailure(Exception e);
}
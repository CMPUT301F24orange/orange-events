// JoinEventFragment.java

package com.example.orange.ui.join;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.databinding.FragmentJoinEventBinding;
import com.example.orange.data.model.UserSession;
import com.example.orange.utils.SessionManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JoinEventFragment displays a list of events that the user is eligible to join.
 * Users can join the waitlist for events they are not already participating in.
 */
public class JoinEventFragment extends Fragment {
    private FragmentJoinEventBinding binding;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private static final String TAG = "JoinEventFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * Initializes Firebase service, session manager, and sets up the RecyclerView
     * to display available events.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentJoinEventBinding.inflate(inflater, container, false);

        // Initialize FirebaseService and SessionManager
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Initialize the event list and adapter
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList, requireContext(), this);

        // Set up RecyclerView with adapter and layout manager
        binding.eventListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventListRecyclerView.setAdapter(eventAdapter);

        // Load and display events the user can join
        loadEvents();

        return binding.getRoot();
    }

    /**
     * Clean up any references to the binding when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Loads all events from Firebase that the user is eligible to join.
     * Filters out events where the user is already a participant, on the waiting list,
     * or has declined the invitation.
     * Updates the RecyclerView with the filtered list of events.
     */
    private void loadEvents() {
        UserSession userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Toast.makeText(requireContext(), "No active session. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userID = userSession.getUserId();
        String userType = userSession.getUserType().toString();
        String userId = userID + "_" + userType;
        Log.d(TAG, "Loading events for user: " + userId);

        firebaseService.getUserEvents(userId, new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> userEvents) {
                firebaseService.getAllEvents(new FirebaseCallback<List<Event>>() {
                    @Override
                    public void onSuccess(List<Event> allEvents) {
                        eventList.clear();

                        // Iterate through all events and add those where the user is not involved
                        for (Event event : allEvents) {
                            List<String> participants = event.getParticipants() != null ? event.getParticipants() : new ArrayList<>();
                            List<String> selectedParticipants = event.getSelectedParticipants() != null ? event.getSelectedParticipants() : new ArrayList<>();
                            List<String> waitingList = event.getWaitingList() != null ? event.getWaitingList() : new ArrayList<>();
                            List<String> cancelledList = event.getCancelledList() != null ? event.getCancelledList() : new ArrayList<>();

                            // User should not be in any of these lists to join
                            if (!participants.contains(userId) &&
                                    !selectedParticipants.contains(userId) &&
                                    !waitingList.contains(userId) &&
                                    !cancelledList.contains(userId)) {
                                eventList.add(event);
                            }
                        }

                        // Notify adapter to update the RecyclerView with the new list
                        eventAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Available Events to Join: " + eventList.size());
                        for (Event event : eventList) {
                            Log.d(TAG, "Event ID: " + event.getId() + ", Title: " + event.getTitle());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(), "Failed to load all events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading all events", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load your events", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading user events", e);
            }
        });
    }

    /**
     * Handles the action of joining an event.
     * Checks if geolocation is required and prompts the user accordingly.
     *
     * @param event Event object the user wants to join the waitlist for.
     */
    public void joinEvent(Event event) {
        if (event.getGeolocationEvent() != null && event.getGeolocationEvent()) {
            // Show dialog to inform user that geolocation is required
            new AlertDialog.Builder(requireContext())
                    .setTitle("Geolocation Required")
                    .setMessage("This event requires geolocation. Do you want to join the waitlist?")
                    .setPositiveButton("Join Waitlist", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // Check location permissions
                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                            } else {
                                // Permissions are granted, proceed to get location
                                addUserToWaitlist(event);
                                getLocation(event.getId());
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Proceed to join the waitlist directly
            addUserToWaitlist(event);
        }
    }

    /**
     * Adds the current user to the waitlist of a specified event in Firebase.
     * Utilizes the enhanced FirebaseService to ensure both Event and User documents are updated.
     *
     * @param event Event object the user wants to join the waitlist for.
     */
    private void addUserToWaitlist(Event event) {
        UserSession userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Toast.makeText(requireContext(), "No active session. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userID = userSession.getUserId();
        String userType = userSession.getUserType().toString();
        String userId = userID + "_" + userType;
        Log.d(TAG, "Attempting to add user with ID: " + userId + " to event: " + event.getId());

        firebaseService.joinEventWaitlist(event.getId(), userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Successfully added to waitlist", Toast.LENGTH_SHORT).show();
                // Optionally, remove the event from the local list to reflect the change
                eventList.remove(event);
                eventAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to join waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to join waitlist for event ID: " + event.getId(), e);
            }
        });
    }

    // Method to get the user's location and update Firebase
    private void getLocation(String eventId) {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "Please enable location services", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Request location updates
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // Minimum time interval between updates in milliseconds
                1,    // Minimum distance between updates in meters
                new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        Log.d(TAG, "Latitude: " + latitude + ", Longitude: " + longitude);

                        // Update Firebase
                        UserSession userSession = sessionManager.getUserSession();
                        if (userSession != null) {
                            String userID = userSession.getUserId();
                            String userType = userSession.getUserType().toString();
                            String userId = userID + "_" + userType;

                            updateLocationInFirebase(eventId, userId, latitude, longitude);
                        }
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                }
        );
    }

    private void updateLocationInFirebase(String eventId, String userId, double latitude, double longitude) {
        Map<String, Object> userLocation = new HashMap<>();
        userLocation.put("ID", userId);
        userLocation.put("latitude", latitude);
        userLocation.put("longitude", longitude);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId)
                .update("location." + userId, userLocation)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }

    // Handle the result of permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

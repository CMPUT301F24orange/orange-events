package com.example.orange.ui.join;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.databinding.FragmentJoinEventBinding;
import com.example.orange.data.model.UserSession;
import com.example.orange.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

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
     * Filters out events where the user is already a participant or on the waiting list.
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
        Log.d("JOIN_EVENT_FRAG", userId);

        firebaseService.getAllEvents(new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                eventList.clear();

                // Filter events to only show those the user is not involved in
                for (Event event : events) {
                    List<String> participants = event.getParticipants() != null ? event.getParticipants() : new ArrayList<>();
                    List<String> selectedParticipants = event.getSelectedParticipants() != null ? event.getSelectedParticipants() : new ArrayList<>();
                    List<String> waitingList = event.getWaitingList() != null ? event.getWaitingList() : new ArrayList<>();

                    if (!participants.contains(userId) && !selectedParticipants.contains(userId) && !waitingList.contains(userId)) {
                        eventList.add(event);
                    }
                }

                // Notify adapter to update the RecyclerView with the new list
                eventAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            // User confirmed to join waitlist
                            addUserToWaitlist(event);
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
        Log.d("JoinEventFragment", "Attempting to add user with ID: " + userId + " to event: " + event.getId());

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
                Log.e("JoinEventFragment", "Failed to join waitlist", e);
            }
        });
    }

    /**
     * Returns the SessionManager instance associated with this fragment.
     *
     * @return The SessionManager instance
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Returns the current list of events.
     *
     * @return List of Event objects
     */
    public List<Event> getEventList() {
        return eventList;
    }
}

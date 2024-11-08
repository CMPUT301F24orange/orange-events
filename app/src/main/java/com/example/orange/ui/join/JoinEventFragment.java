package com.example.orange.ui.join;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.databinding.FragmentJoinEventBinding;
import com.example.orange.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

/**
 * JoinEventFragment displays a list of events that the user is eligible to join.
 * Users can join the waitlist for events they are not already participating in.
 *
 * @author ...
 */
public class JoinEventFragment extends Fragment {
    private FragmentJoinEventBinding binding;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private EventAdapter eventAdapter;
    private List<Event> eventList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentJoinEventBinding.inflate(inflater, container, false);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadEvents() {
        String userId = sessionManager.getUserSession().getUserId();

        firebaseService.getAllEvents(new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                eventList.clear();

                // Filter events to only show those the user is not involved in
                for (Event event : events) {
                    if (!event.getParticipants().contains(userId) && !event.getWaitingList().contains(userId)) {
                        eventList.add(event);
                    }
                }
                // Notify adapter to update the RecyclerView with the new list
                eventAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
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
                            addEntrantToWaitlist(event);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Proceed to join the waitlist directly
            addEntrantToWaitlist(event);
        }
    }

    /**
     * Adds the current user to the waitlist of a specified event in Firebase.
     *
     * @param event Event object the user wants to join the waitlist for.
     */
    private void addEntrantToWaitlist(Event event) {
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.addToEventWaitlist(event.getId(), userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Added to waitlist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to add to waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void leaveWaitlist(Event event) {
        String userId = sessionManager.getUserSession().getUserId();

        firebaseService.removeFromEventWaitlist(event.getId(), userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Removed from waitlist", Toast.LENGTH_SHORT).show();
                loadEvents(); // Reload events to reflect changes
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to remove from waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}

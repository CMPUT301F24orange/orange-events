package com.example.orange.ui.join;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
 * @author Graham Flokstra
 * @author George
 */
public class JoinEventFragment extends Fragment {
    private FragmentJoinEventBinding binding;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private EventAdapter eventAdapter;
    private List<Event> eventList;

    /**
     * Called to initialize the fragment's view, including setting up the RecyclerView
     * to display events the user can join and loading available events.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
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

    /**
     * Cleans up resources by nullifying the binding when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Loads a list of all events from Firebase and filters out events where the user is already
     * a participant or on the waitlist. Updates the displayed list with events the user can join.
     */
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
     * Adds the current user to the waitlist of a specified event in Firebase.
     * Displays a success or failure message based on the result.
     *
     * @param event Event object the user wants to join the waitlist for.
     */
    public void joinEvent(Event event) {
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

    /**
     * Removes the current user from the waitlist of a specified event in Firebase.
     * If successful, the event list is reloaded to reflect changes.
     *
     * @param event Event object the user wants to leave the waitlist for.
     */
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

    /**
     * Provides access to the current SessionManager instance for other classes.
     *
     * @return The session manager associated with the current user.
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }
}

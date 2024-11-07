package com.example.orange.ui.organizer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;
import java.util.List;

/**
 * ViewMyEventsFragment displays all events created by the current organizer.
 * Organizers can view each event and check its waitlist.
 *
 * @author Graham Flokstra, George
 */
public class ViewMyEventsFragment extends Fragment {
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private LinearLayout organizerEventsContainer;

    /**
     * Initializes the fragment's view and loads the events created by the organizer.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_my_organizer_events, container, false);

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
        organizerEventsContainer = view.findViewById(R.id.organizer_events_container);

        loadOrganizerEvents();

        return view;
    }

    /**
     * Loads events created by the current organizer from Firebase and displays them in the container.
     */
    private void loadOrganizerEvents() {
        UserSession userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Toast.makeText(requireContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceId = userSession.getdeviceId();
        UserType userType = userSession.getUserType();

        firebaseService.getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    String organizerId = user.getId(); // Use the correct user ID
                    loadEventsForOrganizer(organizerId);
                } else {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error retrieving user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEventsForOrganizer(String organizerId) {
        firebaseService.getOrganizerEvents(organizerId, new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                displayEvents(events);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load your events", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Dynamically displays each event in the organizer's events list.
     *
     * @param events List of Event objects created by the organizer.
     */
    private void displayEvents(List<Event> events) {
        organizerEventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Event event : events) {
            View eventView = inflater.inflate(R.layout.item_view_organizer_event, organizerEventsContainer, false);

            TextView eventTitle = eventView.findViewById(R.id.organizer_event_title);
            TextView eventDate = eventView.findViewById(R.id.organizer_event_date);
            Button viewWaitlistButton = eventView.findViewById(R.id.view_waitlist_button);

            eventTitle.setText(event.getTitle());
            eventDate.setText("Date: " + (event.getEventDate() != null ? event.getEventDate().toDate().toString() : "N/A"));

            viewWaitlistButton.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getId()); // Pass the event ID
                Navigation.findNavController(requireView()).navigate(R.id.action_view_my_events_to_view_event_waitlist, bundle);
            });viewWaitlistButton.setOnClickListener(v -> showWaitlist(event));

            organizerEventsContainer.addView(eventView);
        }
    }

    /**
     * Displays the waitlist for a specified event in an AlertDialog.
     *
     * @author George
     * @param event Event object whose waitlist should be displayed.
     */
    private void showWaitlist(Event event) {
        List<String> waitlist = event.getWaitingList();
        if (waitlist == null || waitlist.isEmpty()) {
            Toast.makeText(requireContext(), "No users on the waitlist", Toast.LENGTH_SHORT).show();
        } else {
            // Create a dialog to show the waitlist
            StringBuilder waitlistStr = new StringBuilder("Waitlist:\n");
            for (String userId : waitlist) {
                waitlistStr.append(userId).append("\n");
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Waitlist for Event: " + event.getTitle())
                    .setMessage(waitlistStr.toString())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }



}

package com.example.orange.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MyEventsFragment is responsible for displaying a list of events
 * the current user has joined. It fetches the user's events from Firebase
 * and displays relevant information for each event, including the event status
 * and date. Users can also leave the waitlist or event as appropriate.
 *
 * @author Graham Flokstra
 */
public class AdminEventListFragment extends Fragment {
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private LinearLayout eventsContainer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Called to initialize the fragment's view.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_event_list, container, false);

        // Initialize Firebase service and session manager
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Set up the container for displaying events
        eventsContainer = view.findViewById(R.id.admin_events_container);

        // Load and display the user's events
        loadAdminEvents();

        return view;
    }

    /**
     * Loads the current user's events from Firebase and calls displayEvents to render them.
     *
     * @author Graham Flokstra
     */
    private void loadAdminEvents() {

        firebaseService.getAllEvents(new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                displayEvents(events);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a list of events the user has joined, rendering relevant
     * information about each event's status and allowing the user to leave
     * the event or queue if applicable.
     *
     * @author Graham Flokstra
     * @param events List of Event objects representing the user's events.
     */
    private void displayEvents(List<Event> events) {
        eventsContainer.removeAllViews();

        for (Event event : events) {
            View eventView = getLayoutInflater().inflate(R.layout.item_admin_event_list, eventsContainer, false);

            ImageView eventImage = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            Button deleteButton = eventView.findViewById(R.id.delete_button);

            eventTitle.setText(event.getTitle());

            Date currentDate = new Date();

            // Display the relevant date and action based on event's current status
            if (event.getRegistrationDeadline() != null && currentDate.before(event.getRegistrationDeadline().toDate())) {
                eventDate.setText("Waitlist closes: " + dateFormat.format(event.getRegistrationDeadline().toDate()));
                lotteryStatus.setText("Registration Open");
                deleteButton.setOnClickListener(v -> deleteEvent(event.getId()));
            } else if (event.getLotteryDrawDate() != null && currentDate.before(event.getLotteryDrawDate().toDate())) {
                eventDate.setText("Lottery draw: " + dateFormat.format(event.getLotteryDrawDate().toDate()));
                lotteryStatus.setText("Awaiting Lottery Draw");
                deleteButton.setOnClickListener(v -> deleteEvent(event.getId()));
            } else if (event.getEventDate() != null) {
                    eventDate.setText("Event Date: " + dateFormat.format(event.getEventDate().toDate()));
                deleteButton.setOnClickListener(v -> deleteEvent(event.getId()));
            } else {
                // Handle case where no date is available
                eventDate.setText("No date available");
                lotteryStatus.setText("Status Unknown");
                deleteButton.setOnClickListener(v -> deleteEvent(event.getId()));
            }

            // Add the event view to the container
            eventsContainer.addView(eventView);
        }
    }

    /**
     * Deletes an event entirely from the database with no trace left.
     *
     * @param eventId Unique ID of the event to be deleted.
     */
    private void deleteEvent(String eventId) {
        firebaseService.deleteEvent(eventId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Event successfully deleted.", Toast.LENGTH_SHORT).show();
                loadAdminEvents();  // Refresh the events list to reflect deletion
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to delete event.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}

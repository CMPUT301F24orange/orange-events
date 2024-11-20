package com.example.orange.ui.events;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.example.orange.data.model.ImageData;
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
 */
public class MyEventsFragment extends Fragment {
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private LinearLayout eventsContainer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Called to initialize the fragment's view.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        // Initialize Firebase service and session manager
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Set up the container for displaying events
        eventsContainer = view.findViewById(R.id.events_container);

        // Load and display the user's events
        loadUserEvents();

        return view;
    }

    /**
     * Loads the current user's events from Firebase and calls displayEvents to render them.
     */
    private void loadUserEvents() {
        String userId = sessionManager.getUserSession().getUserId();

        firebaseService.getUserEvents(userId, new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                displayEvents(events, userId);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load your events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a list of events the user has joined, rendering relevant
     * information about each event's status and allowing the user to leave
     * the event or queue if applicable.
     *
     * @param events List of Event objects representing the user's events.
     * @param userId The unique ID of the current user.
     */
    private void displayEvents(List<Event> events, String userId) {
        eventsContainer.removeAllViews();

        for (Event event : events) {
            View eventView = getLayoutInflater().inflate(R.layout.item_view_my_events, eventsContainer, false);

            ImageView eventImage = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            Button actionButton = eventView.findViewById(R.id.action_button);

            eventTitle.setText(event.getTitle());

            // Load and display the event image if available
            String eventImageId = event.getEventImageId();
            if (eventImageId != null) {
                firebaseService.getImageById(eventImageId, new FirebaseCallback<ImageData>() {
                    @Override
                    public void onSuccess(ImageData imageData) {
                        if (imageData != null && imageData.getImageData() != null) {
                            byte[] imageBytes = imageData.getImageData().toBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            eventImage.setImageBitmap(bitmap);
                        } else {
                            eventImage.setImageResource(R.drawable.ic_image); // Placeholder if image data is null
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        eventImage.setImageResource(R.drawable.ic_image); // Placeholder if failed to load image
                    }
                });
            } else {
                eventImage.setImageResource(R.drawable.ic_image); // Placeholder if no image is available
            }

            Date currentDate = new Date();

            // Display the relevant date and action based on event's current status
            if (event.getRegistrationDeadline() != null && currentDate.before(event.getRegistrationDeadline().toDate())) {
                eventDate.setText("Waitlist closes: " + dateFormat.format(event.getRegistrationDeadline().toDate()));
                lotteryStatus.setText("In Waitlist");
                actionButton.setText("Leave Queue");
                actionButton.setOnClickListener(v -> leaveQueue(event.getId(), userId));
            } else if (event.getLotteryDrawDate() != null && currentDate.before(event.getLotteryDrawDate().toDate())) {
                eventDate.setText("Lottery draw: " + dateFormat.format(event.getLotteryDrawDate().toDate()));
                lotteryStatus.setText("Awaiting Lottery Draw");
                actionButton.setVisibility(View.GONE);
            } else if (event.getEventDate() != null) {
                if (event.getSelectedParticipants() != null && event.getSelectedParticipants().contains(userId)) {
                    eventDate.setText("Event Date: " + dateFormat.format(event.getEventDate().toDate()));
                    lotteryStatus.setText("Selected for Event");
                    actionButton.setText("Leave Event");
                    actionButton.setOnClickListener(v -> leaveEvent(event.getId(), userId));
                } else {
                    eventDate.setText("Lottery complete: Not selected");
                    lotteryStatus.setText("Not Selected");
                    actionButton.setText("Leave Queue");
                    actionButton.setOnClickListener(v -> leaveQueue(event.getId(), userId));
                }
            } else {
                // Handle case where no date is available
                eventDate.setText("No date available");
                lotteryStatus.setText("Status Unknown");
                actionButton.setVisibility(View.GONE);
            }

            // Add the event view to the container
            eventsContainer.addView(eventView);
        }
    }

    /**
     * Removes the current user from an event's waitlist.
     *
     * @param eventId Unique ID of the event.
     * @param userId  Unique ID of the user.
     */
    private void leaveQueue(String eventId, String userId) {
        firebaseService.removeFromEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "You have left the queue.", Toast.LENGTH_SHORT).show();
                loadUserEvents();  // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to leave queue.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Removes the current user from the list of participants for an event.
     *
     * @param eventId Unique ID of the event.
     * @param userId  Unique ID of the user.
     */
    private void leaveEvent(String eventId, String userId) {
        firebaseService.removeFromEventParticipants(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "You have left the event.", Toast.LENGTH_SHORT).show();
                loadUserEvents();  // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to leave event.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

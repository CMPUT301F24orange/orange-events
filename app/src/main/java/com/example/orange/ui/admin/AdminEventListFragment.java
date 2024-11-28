package com.example.orange.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AdminEventListFragment is responsible for displaying all the events
 * that are currently stored in the database. It also shows some event
 * details. Each event also contains a delete button to delete the
 * event from the database entirely.
 *
 * @author Radhe Patel
 */
public class AdminEventListFragment extends Fragment {
    private FirebaseService firebaseService;
    private LinearLayout eventsContainer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Called to initialize the fragment's view.
     *
     * @author Radhe Patel
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

        // Set up the container for displaying events
        eventsContainer = view.findViewById(R.id.admin_events_container);

        // Load and display the user's events
        loadAdminEvents();

        return view;
    }

    /**
     * Loads all events from Firebase and calls displayEvents to render them.
     *
     * @author Radhe Patel
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
     * Displays a list of all events , rendering relevant
     * information about each event's status and allowing the
     * admin to delete the event if necessary.
     *
     * @author Radhe Patel
     * @param events List of Event objects representing all events in the database
     */
    private void displayEvents(List<Event> events) {
        eventsContainer.removeAllViews();

        for (Event event : events) {
            View eventView = getLayoutInflater().inflate(R.layout.item_admin_event_list, eventsContainer, false);

            ImageView eventPoster = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            ImageButton deleteButton = eventView.findViewById(R.id.delete_button);
            ImageButton deletePosterButton = eventView.findViewById(R.id.poster_delete_button);
            ImageButton deleteQRButton = eventView.findViewById(R.id.qr_delete_button);

            String posterImageId = event.getEventImageId();
            if (posterImageId != null) {
                firebaseService.getImageById(posterImageId, new FirebaseCallback<ImageData>() {
                    @Override
                    public void onSuccess(ImageData imageData) {
                        if (imageData != null && imageData.getImageData() != null) {
                            byte[] imageBytes = imageData.getImageData().toBytes();
                            Bitmap profileBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            eventPoster.setImageBitmap(profileBitmap);
                        } else {
                            // Handle case where image data is null
                            eventPoster.setImageResource(R.drawable.ic_image);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle failure
                        eventPoster.setImageResource(R.drawable.ic_image);
                    }
                });
            } else {
                // No poster image, set placeholder
                eventPoster.setImageResource(R.drawable.ic_image);
            }

            eventTitle.setText(event.getTitle());

            Date currentDate = new Date();

            // Display the relevant date and action based on event's current status
            if (event.getRegistrationDeadline() != null && currentDate.before(event.getRegistrationDeadline().toDate())) {
                eventDate.setText("Waitlist closes: " + dateFormat.format(event.getRegistrationDeadline().toDate()));
                lotteryStatus.setText("Registration Open");
            } else if (event.getLotteryDrawDate() != null && currentDate.before(event.getLotteryDrawDate().toDate())) {
                eventDate.setText("Lottery draw: " + dateFormat.format(event.getLotteryDrawDate().toDate()));
                lotteryStatus.setText("Awaiting Lottery Draw");
            } else if (event.getEventDate() != null) {
                    eventDate.setText("Event Date: " + dateFormat.format(event.getEventDate().toDate()));
            } else {
                // Handle case where no date is available
                eventDate.setText("No date available");
                lotteryStatus.setText("Status Unknown");

            }

            // set the delete buttons listeners
            deleteButton.setOnClickListener(v -> delEvent(event.getId()));
            deletePosterButton.setOnClickListener(v -> deletePosterAdmin(event.getId(), eventPoster));
            deleteQRButton.setOnClickListener(v -> deleteQR(event.getId()));

            // Add the event view to the container
            eventsContainer.addView(eventView);
        }
    }

    /**
     * Deletes an event entirely from the database with no trace left.
     *
     * @author Radhe Patel
     * @param eventId Unique ID of the event to be deleted.
     */
    public void delEvent(String eventId) {
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

    /**
     * Deletes an event poster from the database and updates the UI
     *
     * @author Radhe Patel
     * @param eventId Unique ID of the event poster to be deleted.
     * @param posterImageView The ImageView to reset the placeholder image.
     */
    private void deletePosterAdmin(String eventId, ImageView posterImageView){

        firebaseService.deletePosterAdmin(eventId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Poster successfully deleted.", Toast.LENGTH_SHORT).show();
                posterImageView.setImageResource(R.drawable.ic_image); //Reset to the original poster picture
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to delete profile picture.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteQR(String eventId) {

        firebaseService.deleteQR(eventId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Hashed QR code data successfully deleted.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to delete hashed QR code data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}

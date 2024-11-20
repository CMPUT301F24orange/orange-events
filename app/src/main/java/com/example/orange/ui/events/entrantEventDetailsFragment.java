package com.example.orange.ui.events;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.ImageData;
import com.example.orange.utils.SessionManager;


/**
 * This fragment displays details of an event and allows a user to join or leave the event's waitlist.
 * This class is called only after the QR code has been scanned, being linked by the eventId.
 *
 * @author
 */
public class entrantEventDetailsFragment extends Fragment {

    public Button joinEventButton;
    public Button leaveEventButton;

    private String eventId;
    public FirebaseService firebaseService; // Service to interact with Firebase
    public SessionManager sessionManager; // Manages user session

    /**
     * Called when the fragment is created. Sets up the fragment's UI and loads event details.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_entrant_event_details, container, false);

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Get event ID from arguments and load event details if available
        eventId = getArguments() != null ? getArguments().getString("event_id") : null;
        if (eventId != null) {
            loadEventDetails(eventId, view); // Load event details from Firebase
        } else {
            Toast.makeText(requireContext(), "No event ID found", Toast.LENGTH_SHORT).show();
        }

        joinEventButton = view.findViewById(R.id.joinWaitlistButton);
        leaveEventButton = view.findViewById(R.id.leaveWaitlistButton);
        joinEventButton.setOnClickListener(v -> joinEvent(eventId));
        leaveEventButton.setOnClickListener(v -> leaveEvent(eventId));

        return view;
    }

    /**
     * Loads event details from Firebase and updates the UI.
     *
     * @param eventId The ID of the event to load
     * @param view    The view to update with event details
     */
    public void loadEventDetails(String eventId, View view) {
        firebaseService.getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                if (result != null) {
                    // Display event image
                    ImageView eventImage = view.findViewById(R.id.eventImage);
                    String eventImageId = result.getEventImageId();
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

                    ((TextView) view.findViewById(R.id.eventName)).setText(result.getTitle());
                    ((TextView) view.findViewById(R.id.eventDescriptionText)).setText(result.getDescription());
                    ((TextView) view.findViewById(R.id.eventDateText)).setText(result.getStartDate() != null ? result.getStartDate().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.registrationOpensText)).setText(result.getRegistrationOpens() != null ? result.getRegistrationOpens().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.registrationDeadlineText)).setText(result.getRegistrationDeadline() != null ? result.getRegistrationDeadline().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.eventLimitText)).setText(String.valueOf(result.getCapacity()));
                    ((TextView) view.findViewById(R.id.waitlistLimitText)).setText(result.getWaitlistLimit() != null ? String.valueOf(result.getWaitlistLimit()) : "N/A");
                    ((TextView) view.findViewById(R.id.lotteryDayText)).setText(result.getLotteryDrawDate() != null ? result.getLotteryDrawDate().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.eventPriceText)).setText(result.getPrice() != null ? result.getPrice().toString() : "N/A");
                } else {
                    Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show(); // Show error if event not found
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FirebaseError", "Error fetching event details", e); // Log error if event details fail to load
                Toast.makeText(requireContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Adds the user to the event waitlist.
     *
     * @param eventId The ID of the event to join
     */
    public void joinEvent(String eventId) {
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.addToEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
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
     * Removes the user from the event waitlist.
     *
     * @param eventId The ID of the event to leave
     */
    public void leaveEvent(String eventId) {
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.removeFromEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Removed from waitlist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to remove from waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

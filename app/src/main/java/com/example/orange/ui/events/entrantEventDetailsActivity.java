package com.example.orange.ui.events;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.ImageData;
import com.example.orange.utils.SessionManager;

/**
 * Activity to display details of an event and allow users to join or leave the event's waitlist.
 */
public class entrantEventDetailsActivity extends AppCompatActivity {

    private Button joinEventButton;
    private Button leaveEventButton;

    private String eventId;
    private FirebaseService firebaseService; // Service to interact with Firebase
    private SessionManager sessionManager; // Manages user session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_entrant_event_details); // Reusing the same layout

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(this);

        // Get event ID from the Intent
        eventId = getIntent().getStringExtra("event_id");
        if (eventId != null) {
            loadEventDetails(eventId); // Load event details from Firebase
        } else {
            Toast.makeText(this, "No event ID found", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no event ID is provided
        }

        joinEventButton = findViewById(R.id.joinWaitlistButton);
        leaveEventButton = findViewById(R.id.leaveWaitlistButton);
        joinEventButton.setOnClickListener(v -> joinEvent(eventId));
        leaveEventButton.setOnClickListener(v -> leaveEvent(eventId));
    }

    /**
     * Loads event details from Firebase and updates the UI.
     *
     * @param eventId The ID of the event to load
     */
    private void loadEventDetails(String eventId) {
        firebaseService.getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                if (result != null) {
                    // Display event image
                    ImageView eventImage = findViewById(R.id.eventImage);
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

                    ((TextView) findViewById(R.id.eventName)).setText(result.getTitle());
                    ((TextView) findViewById(R.id.eventDescriptionText)).setText(result.getDescription());
                    ((TextView) findViewById(R.id.eventDateText)).setText(result.getStartDate() != null ? result.getStartDate().toDate().toString() : "N/A");
                    ((TextView) findViewById(R.id.registrationOpensText)).setText(result.getRegistrationOpens() != null ? result.getRegistrationOpens().toDate().toString() : "N/A");
                    ((TextView) findViewById(R.id.registrationDeadlineText)).setText(result.getRegistrationDeadline() != null ? result.getRegistrationDeadline().toDate().toString() : "N/A");
                    ((TextView) findViewById(R.id.eventLimitText)).setText(String.valueOf(result.getCapacity()));
                    ((TextView) findViewById(R.id.waitlistLimitText)).setText(result.getWaitlistLimit() != null ? String.valueOf(result.getWaitlistLimit()) : "N/A");
                    ((TextView) findViewById(R.id.lotteryDayText)).setText(result.getLotteryDrawDate() != null ? result.getLotteryDrawDate().toDate().toString() : "N/A");
                    ((TextView) findViewById(R.id.eventPriceText)).setText(result.getPrice() != null ? result.getPrice().toString() : "N/A");
                } else {
                    Toast.makeText(entrantEventDetailsActivity.this, "Event not found", Toast.LENGTH_SHORT).show(); // Show error if event not found
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FirebaseError", "Error fetching event details", e); // Log error if event details fail to load
                Toast.makeText(entrantEventDetailsActivity.this, "Failed to load event details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Adds the user to the event waitlist.
     *
     * @param eventId The ID of the event to join
     */
    private void joinEvent(String eventId) {
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.addToEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(entrantEventDetailsActivity.this, "Added to waitlist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(entrantEventDetailsActivity.this, "Failed to add to waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Removes the user from the event waitlist.
     *
     * @param eventId The ID of the event to leave
     */
    private void leaveEvent(String eventId) {
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.removeFromEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(entrantEventDetailsActivity.this, "Removed from waitlist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(entrantEventDetailsActivity.this, "Failed to remove from waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

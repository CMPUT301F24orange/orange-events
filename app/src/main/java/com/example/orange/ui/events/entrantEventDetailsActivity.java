package com.example.orange.ui.events;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.orange.MainActivity;
import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.ImageData;
import com.example.orange.utils.SessionManager;

/**
 * Activity to display details of an event and allow users to join or leave the event's waitlist.
 *
 * @author Brandon Ramirez
 */
public class entrantEventDetailsActivity extends AppCompatActivity {

    private Button joinEventButton;
    private Button leaveEventButton;

    private String eventId;
    private FirebaseService firebaseService; // Service to interact with Firebase
    private SessionManager sessionManager; // Manages user session

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery_event_details);

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

        joinEventButton = findViewById(R.id.AcceptEventButton);
        leaveEventButton = findViewById(R.id.DeclineEventButton);
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

                    ((TextView) findViewById(R.id.eventName)).setText("Congratulations! You have been selected to join the " + result.getTitle() + " event");
                    ((TextView) findViewById(R.id.eventDescriptionText)).setText(result.getDescription());
                    ((TextView) findViewById(R.id.eventDateText)).setText(result.getStartDate() != null ? result.getStartDate().toDate().toString() : "N/A");
                    ((TextView) findViewById(R.id.eventLimitText)).setText(String.valueOf(result.getCapacity()));
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
        //TODO: intsead of adding to the waitlist add list of accepted users
        firebaseService.addToEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(entrantEventDetailsActivity.this, "Accepted the offer", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(entrantEventDetailsActivity.this, "Failed to accept offer", Toast.LENGTH_SHORT).show();
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
        //TODO: intsead of adding to the waitlist add list of declined users
        firebaseService.removeFromEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(entrantEventDetailsActivity.this, "Declined the offer", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(entrantEventDetailsActivity.this, "Failed to decline offer", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

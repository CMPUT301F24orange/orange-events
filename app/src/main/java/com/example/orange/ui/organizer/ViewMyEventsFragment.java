package com.example.orange.ui.organizer;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * ViewMyEventsFragment displays all events created by the current organizer.
 * Organizers can view each event and check its waitlist.
 *
 * @author Graham Flokstra, George
 */
public class ViewMyEventsFragment extends Fragment {
    private static final String TAG = "ViewMyEventsFragment";
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private LinearLayout organizerEventsContainer;

    private Event selectedEvent; // To keep track of which event is being updated
    private Uri selectedImageUri;
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
     * Activity result launcher for handling image selection from device storage.
     * Launches the system's media picker and handles the selected image.
     *
     * @author Graham Flokstra
     */
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Process the image and update event
                    processEventImage(selectedEvent);
                } else {
                    Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
                }
            }
    );

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
            View eventView = inflater.inflate(R.layout.item_view_my_events, organizerEventsContainer, false);

            ImageView eventImage = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            Button actionButton = eventView.findViewById(R.id.action_button);
            Button changeImageButton = eventView.findViewById(R.id.change_image_button);

            // Set the data
            eventTitle.setText(event.getTitle());
            eventDate.setText("Date: " + (event.getEventDate() != null ? event.getEventDate().toDate().toString() : "N/A"));

            // Load event image if available
            Blob eventImageData = event.getEventImageData();
            if (eventImageData != null) {
                byte[] imageData = eventImageData.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                eventImage.setImageBitmap(bitmap);
            } else {
                eventImage.setImageResource(R.drawable.ic_image); // Placeholder if no image is available
            }

            // Hide the lotteryStatus TextView as it's not needed for organizers
            lotteryStatus.setVisibility(View.GONE);

            // Set the actionButton text to "View Waitlist"
            actionButton.setText("View Waitlist");

            // Set the click listener to show the waitlist
            actionButton.setOnClickListener(v -> showWaitlist(event));

            // Set click listener for changeImageButton
            changeImageButton.setOnClickListener(v -> {
                selectedEvent = event; // Keep track of which event we're updating
                showImageOptions();
            });

            organizerEventsContainer.addView(eventView);
        }
    }

    private void showImageOptions() {
        String[] options = {"Change Image", "Remove Image", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Event Image Options");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Change Image
                openImagePicker();
            } else if (which == 1) {
                // Remove Image
                removeEventImage();
            } else {
                // Cancel
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void openImagePicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void processEventImage(Event event) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize image
            int maxSize = 500;
            float scale = Math.min(((float) maxSize / bitmap.getWidth()), ((float) maxSize / bitmap.getHeight()));
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Compress image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageData = baos.toByteArray();

            if (imageData.length > 1048576) {
                Toast.makeText(getContext(), "Image is too large to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update event image in Firebase
            updateEventImage(event.getId(), imageData);

        } catch (IOException e) {
            Toast.makeText(getContext(), "Error reading image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error processing image", e);
        }
    }

    private void updateEventImage(String eventId, byte[] imageData) {
        firebaseService.updateEventImage(eventId, imageData, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Event image updated", Toast.LENGTH_SHORT).show();
                loadOrganizerEvents(); // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to update event image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to update event image", e);
            }
        });
    }

    private void removeEventImage() {
        firebaseService.removeEventImage(selectedEvent.getId(), new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Event image removed", Toast.LENGTH_SHORT).show();
                loadOrganizerEvents(); // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to remove event image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to remove event image", e);
            }
        });
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

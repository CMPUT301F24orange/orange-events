package com.example.orange.ui.organizer;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.Locale;

/**
 * ViewMyEventsFragment displays all events created by the current organizer.
 * Organizers can view each event and check its waitlist.
 *
 * @author Graham Flokstra, George, Brandon Ramirez
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
     * Retrieves the current user session and fetches events associated with the organizer's ID.
     * Displays error messages if the session is invalid or if data retrieval fails.
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

    /**
     * Retrieves and displays events for a specific organizer from Firebase.
     *
     * @author Graham Flokstra
     * @param organizerId The unique identifier of the organizer whose events should be loaded
     */
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
     * Creates and populates view elements for each event, including:
     * - Event image
     * - Title
     * - Relevant dates (registration deadline, lottery draw, or event date)
     * - Waitlist count
     * - Action buttons for viewing waitlist and managing event image
     *
     * @author Graham Flokstra
     * @param events List of Event objects created by the organizer.
     */
    private void displayEvents(List<Event> events) {
        organizerEventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        for (Event event : events) {
            View eventView = inflater.inflate(R.layout.item_view_organizer_event, organizerEventsContainer, false);

            Button GenerateButton = eventView.findViewById(R.id.generate_QR_button);
            ImageView eventImage = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            Button actionButton = eventView.findViewById(R.id.action_button);
            Button changeImageButton = eventView.findViewById(R.id.change_image_button);
            Button drawParticipantsButton = eventView.findViewById(R.id.draw_participants_button);

            // Set the data
            eventTitle.setText(event.getTitle());

            // Load event image if available
            Blob eventImageData = event.getEventImageData();
            if (eventImageData != null) {
                byte[] imageData = eventImageData.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                eventImage.setImageBitmap(bitmap);
            } else {
                eventImage.setImageResource(R.drawable.ic_image); // Placeholder if no image is available
            }


            GenerateButton.setOnClickListener(v-> generateQR(event));
            // Display the relevant date based on event's current status
            if (event.getRegistrationDeadline() != null && currentDate.before(event.getRegistrationDeadline().toDate())) {
                eventDate.setText("Waitlist closes: " + dateFormat.format(event.getRegistrationDeadline().toDate()));
            } else if (event.getLotteryDrawDate() != null && currentDate.before(event.getLotteryDrawDate().toDate())) {
                eventDate.setText("Lottery draw: " + dateFormat.format(event.getLotteryDrawDate().toDate()));
            } else if (event.getEventDate() != null) {
                eventDate.setText("Event Date: " + dateFormat.format(event.getEventDate().toDate()));
            } else {
                // Handle case where no date is available
                eventDate.setText("No date available");
            }

            // Show waitlist count
            int waitlistCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            lotteryStatus.setText("Waitlist Count: " + waitlistCount);

            // Set the actionButton text to "View Waitlist"
            actionButton.setText("View Waitlist");

            // Set the click listener to show the waitlist
            actionButton.setOnClickListener(v -> showWaitlist(event));

            // Set click listener for changeImageButton
            changeImageButton.setOnClickListener(v -> {
                selectedEvent = event; // Keep track of which event we're updating
                showImageOptions();
            });

// Set click listener for the "Draw Participants" button
            drawParticipantsButton.setOnClickListener(v -> {
                // Show a dialog for the organizer to specify the number of attendees
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Specify Number of Attendees");

                // Input field for the number
                final EditText input = new EditText(requireContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);

                // Set up dialog buttons
                builder.setPositiveButton("OK", (dialog, which) -> {
                    String inputText = input.getText().toString();
                    int numToSelect = inputText.isEmpty() ? -1 : Integer.parseInt(inputText);

                    // Call the backend with the specified number
                    firebaseService.drawFromWaitlist(event.getId(), numToSelect > 0 ? numToSelect : null, new FirebaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), "Participants drawn successfully", Toast.LENGTH_SHORT).show();
                            loadOrganizerEvents(); // Refresh events list
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "Failed to draw participants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.show();
            });


            organizerEventsContainer.addView(eventView);
        }
    }

    /**
     * Displays a dialog with options to change or remove the event image.
     * Options include:
     * - Change Image: Opens image picker
     * - Remove Image: Removes current event image
     * - Cancel: Dismisses the dialog
     *
     * @author Graham Flokstra
     */
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

    /**
     * Launches the system's image picker to select a new event image.
     * Uses ActivityResultContracts.PickVisualMedia to handle image selection.
     */
    private void openImagePicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Processes the selected image for an event.
     * - Resizes the image to a maximum dimension of 500px
     * - Compresses the image to JPEG format with 50% quality
     * - Checks if the resulting file size is within the 1MB limit
     * - Updates the event image in Firebase if all checks pass
     *
     * @author Graham Flokstra
     * @param event The event whose image should be updated
     */
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


    /**
     * Updates the event image in Firebase with the provided image data.
     *
     * @author Graham Flokstra
     * @param eventId The unique identifier of the event to update
     * @param imageData The processed image data as a byte array
     */
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

    /**
     * Removes the current image from the selected event.
     * Updates Firebase and refreshes the event list upon successful removal.
     *
     * @author Graham Flokstra
     */
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
    /**
     * Generates the QR code for each event that an organizer has.
     *
     * @author Brandon Ramirez
     * @param event current event being passed
     */
    public void generateQR(Event event) {
        try {
            // Prepare event details for QR content
            String qrContent = "Event ID: " + event.getId() + "\n"
                    + "Event Name: " + event.getTitle() + "\n"
                    + "Date: " + (event.getEventDate() != null ? event.getEventDate().toDate().toString() : "N/A") + "\n"
                    + "Description: " + event.getDescription();

            // Generate QR code bitmap
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400);

            String hash = generateHash(qrContent);

            if (hash != null) {
                firebaseService.storeEventHash(event.getId(), hash);
            }
            // Save QR bitmap to cache and get the URI
            Uri qrUri = saveQRToCache(bitmap);
            // Pass both qr_bitmap and eventId to DisplayQRFragment
            Bundle args = new Bundle();
            args.putParcelable("qr_uri", qrUri);
            args.putString("event_id", event.getId());

            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_displayqr, args);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Generates the hash data for each qr code then places into firebase
     *
     * @author Brandon Ramirez
     * @param data qr info
     */
    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Turns the qr code generated into a png file to save space and not have to transfer a large file
     *
     * @author Brandon Ramirez
     * @param qrBitmap the bitmap of the qr code
     */
    private Uri saveQRToCache(Bitmap qrBitmap) {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs(); // Ensure the directory exists
            File file = new File(cachePath, "qr_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            return FileProvider.getUriForFile(requireContext(), "com.example.orange.fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

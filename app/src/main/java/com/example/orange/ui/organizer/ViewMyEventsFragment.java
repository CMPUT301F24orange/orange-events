package com.example.orange.ui.organizer;

import android.app.AlertDialog;
import android.content.Context;
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
// Removed unused imports
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.orange.MainActivity;
import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.ImageData;
import com.example.orange.data.model.Notification;
import com.example.orange.data.model.NotificationType;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.ui.notifications.EntrantNotifications;
import com.example.orange.ui.notifications.FirebaseNotifications;
import com.example.orange.databinding.FragmentViewMyOrganizerEventsBinding;
import com.example.orange.utils.SessionManager;
import com.google.firebase.firestore.Blob;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ViewMyEventsFragment displays all events created by the current organizer.
 * Organizers can view each event and check its waitlist.
 *
 */
public class ViewMyEventsFragment extends Fragment {
    private static final String TAG = "ViewMyEventsFragment";
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private FragmentViewMyOrganizerEventsBinding binding;

    private Event selectedEvent; // To keep track of which event is being updated
    private Uri selectedImageUri;

    /**
     * Activity result launcher for handling image selection from device storage.
     * Launches the system's media picker and handles the selected image.
     *
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
     * Initializes the fragment's view and loads the events created by the organizer.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentViewMyOrganizerEventsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        loadOrganizerEvents();
        return view;
    }

    /**
     * Clean up any references to the binding when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

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
     * @param events List of Event objects created by the organizer.
     */
    /**
     * Dynamically displays each event in the organizer's events list.
     * Creates and populates view elements for each event, including:
     * - Event image
     * - Title
     * - Relevant dates (registration deadline, lottery draw, or event date)
     * - Waitlist count
     * - Action buttons for viewing waitlist and managing event image
     *
     * @param events List of Event objects created by the organizer.
     */
    private void displayEvents(List<Event> events) {
        binding.organizerEventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        for (Event event : events) {
            View eventView = inflater.inflate(R.layout.item_view_organizer_event, binding.organizerEventsContainer, false);

            // Initialize views using findViewById for the inflated eventView
            AppCompatImageButton generateButton = eventView.findViewById(R.id.generate_QR_button);
            ImageView eventImage = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            ImageButton actionButton = eventView.findViewById(R.id.view_waitlist_button);
            ImageButton changeImageButton = eventView.findViewById(R.id.change_image_button);
            ImageButton drawParticipantsButton = eventView.findViewById(R.id.draw_participants_button);

            // New buttons
            ImageButton viewSelectedParticipantsButton = eventView.findViewById(R.id.view_selected_participants_button);
            ImageButton viewCancelledParticipantsButton = eventView.findViewById(R.id.view_cancelled_participants_button);
            ImageButton viewParticipatingButton = eventView.findViewById(R.id.view_participating_button);
            LinearLayout secondButtonRow = eventView.findViewById(R.id.second_button_row);

            // Set the data
            eventTitle.setText(event.getTitle());

            // Load event image if available
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

            generateButton.setOnClickListener(v -> generateQR(event));

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

            // Set the click listener to show the waitlist
            actionButton.setOnClickListener(v -> showWaitlist(event));

            // Set click listener for changeImageButton
            changeImageButton.setOnClickListener(v -> {
                selectedEvent = event; // Keep track of which event we're updating
                showImageOptions();
            });

            // Set click listener for drawParticipantsButton
            drawParticipantsButton.setOnClickListener(v -> {
                drawFromWaitlist(event);
            });

            // Determine visibility of second button row based on list sizes
            boolean hasSelectedParticipants = event.getSelectedParticipants() != null && !event.getSelectedParticipants().isEmpty();
            boolean hasCancelledParticipants = event.getCancelledList() != null && !event.getCancelledList().isEmpty();
            boolean hasParticipating = event.getParticipants() != null && !event.getParticipants().isEmpty();

            if (hasSelectedParticipants || hasCancelledParticipants || hasParticipating) {
                secondButtonRow.setVisibility(View.VISIBLE);
                // Set click listeners for new buttons
                if (hasSelectedParticipants) {
                    viewSelectedParticipantsButton.setVisibility(View.VISIBLE);
                    viewSelectedParticipantsButton.setOnClickListener(v -> showSelectedParticipants(event));
                } else {
                    viewSelectedParticipantsButton.setVisibility(View.GONE);
                }

                if (hasCancelledParticipants) {
                    viewCancelledParticipantsButton.setVisibility(View.VISIBLE);
                    viewCancelledParticipantsButton.setOnClickListener(v -> showCancelledParticipants(event));
                } else {
                    viewCancelledParticipantsButton.setVisibility(View.GONE);
                }

                if (hasParticipating) {
                    viewParticipatingButton.setVisibility(View.VISIBLE);
                    viewParticipatingButton.setOnClickListener(v -> showParticipating(event));
                } else {
                    viewParticipatingButton.setVisibility(View.GONE);
                }
            } else {
                secondButtonRow.setVisibility(View.GONE);
            }

            binding.organizerEventsContainer.addView(eventView);
        }
    }

    /**
     * Implements the participant drawing functionality.
     * Selects users from the waitlist randomly and moves them to the participants list.
     * Additionally, creates notifications for both selected and unselected users.
     *
     * @author Graham Flokstra
     * @param event The event from which to draw participants.
     */
    private void drawFromWaitlist(Event event) {
        if (event.isFull()) {
            Toast.makeText(requireContext(), "Event is already full.", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentParticipants = event.getParticipants() != null ? event.getParticipants().size() : 0;
        int selectedParticipants = event.getSelectedParticipants() != null ? event.getSelectedParticipants().size() : 0;
        int capacity = event.getCapacity() != null ? event.getCapacity() : Integer.MAX_VALUE;
        int slotsAvailable = capacity - currentParticipants - selectedParticipants;

        if (slotsAvailable <= 0) {
            Toast.makeText(requireContext(), "No available slots to draw participants.", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseService.getEventWaitlist(event.getId(), new FirebaseCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> waitlist) {
                if (waitlist == null || waitlist.isEmpty()) {
                    Toast.makeText(requireContext(), "No users on the waitlist to draw.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Shuffle the waitlist to ensure random selection
                Collections.shuffle(waitlist);

                // Determine the number of users to draw
                int usersToDraw = Math.min(slotsAvailable, waitlist.size());
                List<String> selectedUsers = waitlist.subList(0, usersToDraw);
                List<String> unselectedUsers = waitlist.subList(usersToDraw, waitlist.size());

                // Perform Firestore transaction for atomic update
                firebaseService.moveUsersToSelectedParticipants(event.getId(), selectedUsers, new FirebaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Participants drawn successfully.");
                        Toast.makeText(requireContext(), "Participants drawn successfully.", Toast.LENGTH_SHORT).show();

                        // Create notifications for both selected and unselected users
                        EntrantNotifications entrantNotifications = new EntrantNotifications();
                        if(!selectedUsers.isEmpty()){
                            for (String userId : selectedUsers) {
                                firebaseService.getUserById(userId, new FirebaseCallback<User>() {
                                    @Override
                                    public void onSuccess(User user) {
                                        entrantNotifications.sendToPhone(getContext(),"You Have Won The Lottery!", "You have just been selected to join "+event.getTitle() +". Choose whether to accept to decline the offer.", user, event.getId());
                                        Notification notification = new Notification(event.getId(), userId, NotificationType.SELECTED_TO_PARTICIPATE);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.d("Error", "Failed to get user");
                                    }
                                });
                            }
                        }
                        if(!unselectedUsers.isEmpty()){
                            for (String userId : unselectedUsers) {
                                firebaseService.getUserById(userId, new FirebaseCallback<User>() {
                                    @Override
                                    public void onSuccess(User user) {
                                        entrantNotifications.sendToPhone(getContext(),"Not your lucky day today :(", "You have not been selected to join "+event.getTitle(), user, event.getId());
                                        Notification notification = new Notification(event.getId(), userId, NotificationType.NOT_SELECTED);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.d("Error", "Failed to get user");
                                    }
                                });
                            }
                        }
                        firebaseService.createDrawNotifications(event.getId(), selectedUsers, unselectedUsers, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "Notifications created successfully for drawn participants.");
                                Toast.makeText(requireContext(), "Notifications sent to users.", Toast.LENGTH_SHORT).show();
                                loadOrganizerEvents(); // Refresh event data to reflect changes
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Failed to create notifications.", e);
                                Toast.makeText(requireContext(), "Failed to send notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(), "Failed to draw participants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error drawing participants from waitlist", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to retrieve waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error retrieving waitlist", e);
            }
        });
    }



    /**
     * Displays a dialog with options to change or remove the event image.
     * Options include:
     * - Change Image: Opens image picker
     * - Remove Image: Removes current event image
     * - Cancel: Dismisses the dialog
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
     * @param event The event whose image should be updated
     */
    private void processEventImage(Event event) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize image
            int maxSize = 500;
            float scale = Math.min(((float) maxSize / bitmap.getWidth()),
                    ((float) maxSize / bitmap.getHeight()));
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Compress image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageData = baos.toByteArray();

            if (imageData.length > 1048576) { // 1MB limit
                Toast.makeText(getContext(), "Image is too large to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            Blob imageBlob = Blob.fromBytes(imageData);

            // Upload image to Firebase
            firebaseService.createImage(imageBlob, new FirebaseCallback<String>() {
                @Override
                public void onSuccess(String imageId) {
                    // Update the event's eventImageId
                    event.setEventImageId(imageId);

                    // Update the event in Firebase
                    firebaseService.updateEvent(event, new FirebaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), "Event image updated", Toast.LENGTH_SHORT).show();
                            loadOrganizerEvents(); // Refresh the events list
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to update event", e);
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to upload image", e);
                }
            });

        } catch (IOException e) {
            Toast.makeText(getContext(), "Error reading image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error processing image", e);
        }
    }

    /**
     * Removes the current image from the selected event.
     * Updates Firebase and refreshes the event list upon successful removal.
     */
    private void removeEventImage() {
        String imageIdToDelete = selectedEvent.getEventImageId();
        selectedEvent.setEventImageId(null);

        firebaseService.updateEvent(selectedEvent, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (imageIdToDelete != null) {
                    // Delete the image from Firebase
                    firebaseService.deleteImage(imageIdToDelete, new FirebaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), "Event image removed", Toast.LENGTH_SHORT).show();
                            loadOrganizerEvents(); // Refresh the events list
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "Failed to delete image from Firebase", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to delete image from Firebase", e);
                        }
                    });
                } else {
                    Toast.makeText(requireContext(), "Event image removed", Toast.LENGTH_SHORT).show();
                    loadOrganizerEvents(); // Refresh the events list
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to remove event image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to update event", e);
            }
        });
    }

    /**
     * Displays the waitlist for a specified event in an AlertDialog.
     *
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
     * Displays the selected participants for a specified event in an AlertDialog.
     *
     * @param event Event object whose selected participants should be displayed.
     */
    private void showSelectedParticipants(Event event) {
        List<String> selectedParticipants = event.getSelectedParticipants();
        if (selectedParticipants == null || selectedParticipants.isEmpty()) {
            Toast.makeText(requireContext(), "No selected participants.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a dialog to show the selected participants
        StringBuilder selectedStr = new StringBuilder("Selected Participants:\n");
        for (String userId : selectedParticipants) {
            selectedStr.append(userId).append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Selected Participants for Event: " + event.getTitle())
                .setMessage(selectedStr.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Displays the cancelled participants for a specified event in an AlertDialog.
     *
     * @param event Event object whose cancelled participants should be displayed.
     */
    private void showCancelledParticipants(Event event) {
        List<String> cancelledParticipants = event.getCancelledList();
        if (cancelledParticipants == null || cancelledParticipants.isEmpty()) {
            Toast.makeText(requireContext(), "No cancelled participants.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a dialog to show the cancelled participants
        StringBuilder cancelledStr = new StringBuilder("Cancelled Participants:\n");
        for (String userId : cancelledParticipants) {
            cancelledStr.append(userId).append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelled Participants for Event: " + event.getTitle())
                .setMessage(cancelledStr.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Displays the participating users for a specified event in an AlertDialog.
     *
     * @param event Event object whose participating users should be displayed.
     */
    private void showParticipating(Event event) {
        List<String> participating = event.getParticipants();
        if (participating == null || participating.isEmpty()) {
            Toast.makeText(requireContext(), "No participants.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a dialog to show the participating users
        StringBuilder participatingStr = new StringBuilder("Participants:\n");
        for (String userId : participating) {
            participatingStr.append(userId).append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Participants for Event: " + event.getTitle())
                .setMessage(participatingStr.toString())
                .setPositiveButton("OK", null)
                .show();
    }


    /**
     * Generates the QR code for each event that an organizer has.
     *
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
     * Generates the hash data for each QR code then places into Firebase
     *
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
     * Turns the QR code generated into a PNG file to save space and not have to transfer a large file
     *
     * @param qrBitmap the bitmap of the QR code
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

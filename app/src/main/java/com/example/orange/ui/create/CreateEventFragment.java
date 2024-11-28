package com.example.orange.ui.create;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.ImageData;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.databinding.FragmentCreateEventBinding;
import com.example.orange.utils.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Fragment responsible for creating new events within the Orange application.
 * This fragment provides a comprehensive form interface allowing organizers to input
 * all necessary event details including title, description, dates, capacity,
 * registration details, and an optional event image.
 */
public class CreateEventFragment extends Fragment {

    private static final String TAG = "CreateEventFragment";

    private FragmentCreateEventBinding binding;

    private FirebaseService firebaseService;
    private SessionManager sessionManager;

    private Uri selectedImageUri;

    /**
     * Activity result launcher for handling image selection from device storage.
     * Launches the system's media picker and handles the selected image.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    binding.addImageButton.setImageURI(uri);
                    selectedImageUri = uri;
                }
            }
    );

    /**
     * Creates and initializes the fragment's view hierarchy.
     *
     * @param inflater LayoutInflater object to inflate views
     * @param container If non-null, parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, fragment is being re-constructed from previous saved state
     * @return The View for the fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout using View Binding
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Initialize FirebaseService and SessionManager
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Initialize views and set up click listeners
        setupClickListeners();

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
     * Sets up click listeners for all interactive elements in the fragment.
     */
    private void setupClickListeners() {
        binding.createEventButton.setOnClickListener(v -> createEvent());
        binding.uploadImageButton.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        binding.deleteImageButton.setOnClickListener(v -> {
            binding.addImageButton.setImageResource(R.drawable.ic_image); // Placeholder image
            selectedImageUri = null;
        });

        // Optional: Show/hide waitlistLimitEditText based on checkbox
        binding.waitlistLimitCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.waitlistLimitEditText.setVisibility(View.VISIBLE);
            } else {
                binding.waitlistLimitEditText.setVisibility(View.GONE);
                binding.waitlistLimitEditText.setText(""); // Clear text when hidden
            }
        });
    }

    /**
     * Handles the event creation process. Validates all input fields, processes the selected image
     * if any, and creates a new event in Firebase. Upon successful creation, navigates to the
     * view-my-events screen.
     */
    private void createEvent() {
        Event event = buildEventFromInputs();
        if (event == null) return;

        UserSession userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Toast.makeText(requireContext(), "Error: No user session found", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceId = userSession.getdeviceId();
        UserType userType = userSession.getUserType();

        firebaseService.getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getFacilityId() != null) {
                    String organizerId = user.getId();
                    event.setOrganizerId(organizerId);
                    event.setFacilityId(user.getFacilityId());

                    if (selectedImageUri != null) {
                        // Process and upload the image
                        processAndUploadEventImage(event, organizerId);
                    } else {
                        // No image selected, proceed to save event
                        saveEventToFirebase(event, organizerId);
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Organizer's facility not found. Please update your facility profile.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(),
                        "Error retrieving organizer data: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Processes and uploads the event image.
     *
     * @param event       The event object to attach the image ID.
     * @param organizerId The ID of the organizer creating the event.
     */
    private void processAndUploadEventImage(Event event, String organizerId) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize image
            int maxSize = 500;
            float scale = Math.min(((float) maxSize / bitmap.getWidth()),
                    ((float) maxSize / bitmap.getHeight()));
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

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
                    // Set the image ID in the event
                    event.setEventImageId(imageId);
                    // Now save the event with organizerId and facilityId
                    saveEventToFirebase(event, organizerId);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            Toast.makeText(getContext(), "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Builds an Event object from user input fields.
     *
     * @return Event object containing all input data, or null if validation fails
     */
    private Event buildEventFromInputs() {
        String title = binding.titleEditText.getText().toString().trim();
        String description = binding.descriptionEditText.getText().toString().trim();
        Integer capacity = parseIntegerField(binding.capacityEditText.getText().toString().trim());
        Double price = parseDoubleField(binding.eventPriceEditText.getText().toString().trim());
        Timestamp startDate = parseDate(binding.startDateInput.getText().toString());
        Timestamp endDate = parseDate(binding.endDateInput.getText().toString());
        Timestamp registrationOpens = parseDate(binding.registrationOpensEditText.getText().toString());
        Timestamp registrationDeadline = parseDate(binding.registrationDeadlineEditText.getText().toString());
        Timestamp lotteryDay = parseDate(binding.lotteryDayEditText.getText().toString());
        Integer waitlistLimit = binding.waitlistLimitCheckbox.isChecked() ?
                parseIntegerField(binding.waitlistLimitEditText.getText().toString().trim()) : null;
        Boolean geolocation = binding.geolocationCheckbox.isChecked();

        // Perform validation
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (TextUtils.isEmpty(description)) {
            Toast.makeText(getContext(), "Description is required", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (capacity == null || capacity <= 0) {
            Toast.makeText(getContext(), "Valid capacity is required", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (startDate == null || endDate == null || registrationOpens == null || registrationDeadline == null || lotteryDay == null) {
            Toast.makeText(getContext(), "All dates must be valid", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (price == null || price < 0) {
            Toast.makeText(getContext(), "Valid event price is required", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (binding.waitlistLimitCheckbox.isChecked() && (waitlistLimit == null || waitlistLimit < 0)) {
            Toast.makeText(getContext(), "Valid waitlist limit is required", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Additional validation logic can be added here as needed

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setCapacity(capacity);
        event.setPrice(price);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setRegistrationOpens(registrationOpens);
        event.setRegistrationDeadline(registrationDeadline);
        event.setLotteryDrawDate(lotteryDay);
        event.setWaitlistLimit(waitlistLimit);
        event.setGeolocationEvent(geolocation);

        return event;
    }

    /**
     * Saves the event to Firebase and handles the response.
     *
     * @param event       The event to be saved
     * @param organizerId The ID of the organizer creating the event
     */
    private void saveEventToFirebase(Event event, String organizerId) {
        firebaseService.createEvent(event, organizerId, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String eventId) {
                Toast.makeText(requireContext(), "Event created successfully", Toast.LENGTH_SHORT).show();
                // Navigate to the "My Events" screen
                Navigation.findNavController(requireView()).navigate(R.id.navigation_view_my_events);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(),
                        "Failed to create event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Parses a string into an Integer, returning null if parsing fails.
     *
     * @param value String value to parse
     * @return Parsed Integer or null if parsing fails
     */
    private Integer parseIntegerField(String value) {
        try {
            return TextUtils.isEmpty(value) ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a string into a Double, returning null if parsing fails.
     *
     * @param value String value to parse
     * @return Parsed Double or null if parsing fails
     */
    private Double parseDoubleField(String value) {
        try {
            return TextUtils.isEmpty(value) ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a date string into a Timestamp object.
     *
     * @param dateString Date string in format "yyyy/MM/dd"
     * @return Timestamp object or null if parsing fails
     */
    private Timestamp parseDate(String dateString) {
        try {
            return new Timestamp(new SimpleDateFormat("yyyy/MM/dd",
                    Locale.getDefault()).parse(dateString));
        } catch (ParseException e) {
            return null;
        }
    }
}

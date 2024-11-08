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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment responsible for creating new events within the Orange application.
 * This fragment provides a comprehensive form interface allowing organizers to input
 * all necessary event details including title, description, dates, capacity,
 * registration details, and an optional event image.
 *
 * @author Graham Flokstra, George, Dhairya
 */
public class CreateEventFragment extends Fragment {

    private static final String TAG = "CreateEventFragment";

    private EditText titleEditText;
    private EditText descriptionEditText;
    private EditText capacityEditText;
    private EditText startDateEditText;
    private EditText endDateEditText;
    private EditText registrationOpensEditText;
    private EditText registrationDeadlineEditText;
    private EditText lotteryDayEditText;
    private EditText eventPriceEditText;
    private EditText waitlistLimitEditText;

    private CheckBox waitlistLimitCheckbox;
    private CheckBox geolocation_checkbox;
    private Button createEventButton;
    private Button uploadImageButton;
    private Button deleteImageButton;

    private ImageView eventImage;
    private Uri selectedImageUri;

    private FirebaseService firebaseService;
    private SessionManager sessionManager;

    /**
     * Activity result launcher for handling image selection from device storage.
     * Launches the system's media picker and handles the selected image.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    eventImage.setImageURI(uri);
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
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        initializeServices();
        initializeViews(view);
        setupClickListeners();

        return view;
    }

    /**
     * Initializes Firebase and session management services.
     */
    private void initializeServices() {
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
    }

    /**
     * Initializes all view components of the fragment.
     *
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        capacityEditText = view.findViewById(R.id.capacityEditText);
        startDateEditText = view.findViewById(R.id.start_date_input);
        endDateEditText = view.findViewById(R.id.end_date_input);
        registrationOpensEditText = view.findViewById(R.id.registration_opens_edit_text);
        registrationDeadlineEditText = view.findViewById(R.id.registration_deadline_edit_text);
        lotteryDayEditText = view.findViewById(R.id.lottery_day_edit_text);
        eventPriceEditText = view.findViewById(R.id.event_price_edit_text);
        waitlistLimitEditText = view.findViewById(R.id.waitlist_limit_edit_text);
        waitlistLimitCheckbox = view.findViewById(R.id.waitlist_limit_checkbox);
        geolocation_checkbox = view.findViewById(R.id.geolocation_checkbox);
        createEventButton = view.findViewById(R.id.createEventButton);
        eventImage = view.findViewById(R.id.add_image_button);
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        deleteImageButton = view.findViewById(R.id.delete_image_button);

    }

    /**
     * Sets up click listeners for all interactive elements in the fragment.
     */
    private void setupClickListeners() {
        createEventButton.setOnClickListener(v -> createEvent());
        uploadImageButton.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        deleteImageButton.setOnClickListener(v -> {
            eventImage.setImageResource(R.drawable.ic_image);
            selectedImageUri = null;
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
                        processEventImage(event);
                    }

                    saveEventToFirebase(event);
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
     * Builds an Event object from user input fields.
     *
     * @return Event object containing all input data, or null if validation fails
     */
    private Event buildEventFromInputs() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        Integer capacity = parseIntegerField(capacityEditText.getText().toString().trim());
        Double price = parseDoubleField(eventPriceEditText.getText().toString().trim());
        Timestamp startDate = parseDate(startDateEditText.getText().toString());
        Timestamp endDate = parseDate(endDateEditText.getText().toString());
        Timestamp registrationOpens = parseDate(registrationOpensEditText.getText().toString());
        Timestamp registrationDeadline = parseDate(registrationDeadlineEditText.getText().toString());
        Timestamp lotteryDay = parseDate(lotteryDayEditText.getText().toString());
        Integer waitlistLimit = waitlistLimitCheckbox.isChecked() ?
                parseIntegerField(waitlistLimitEditText.getText().toString().trim()) : null;
        Boolean geolocation = geolocation_checkbox.isChecked();

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
     * Processes the user data and creates the event in Firebase.
     *
     * @param organizerId ID of the event organizer
     * @param event Event object to be created
     */
    private void processUserAndCreateEvent(String organizerId, Event event) {
        firebaseService.getUserById(organizerId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getFacilityId() != null) {
                    event.setOrganizerId(organizerId);
                    event.setFacilityId(user.getFacilityId());

                    if (selectedImageUri != null) {
                        processEventImage(event);
                    }

                    saveEventToFirebase(event);
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
     * Processes and compresses the selected event image.
     *
     * @param event Event object to attach the processed image to
     */
    private void processEventImage(Event event) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
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

            if (imageData.length > 1048576) {
                Toast.makeText(getContext(), "Image is too large to upload",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            event.setEventImageData(Blob.fromBytes(imageData));
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves the event to Firebase and handles the response.
     *
     * @param event The event to be saved
     */
    private void saveEventToFirebase(Event event) {
        firebaseService.createEvent(event, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String eventId) {
                Toast.makeText(requireContext(), "Event created successfully",
                        Toast.LENGTH_SHORT).show();
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
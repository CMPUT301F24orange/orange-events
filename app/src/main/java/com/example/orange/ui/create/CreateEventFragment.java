package com.example.orange.ui.create;

import android.content.Intent;
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
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;
import com.google.firebase.Timestamp;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CreateEventFragment allows organizers to create new events by filling out
 * details such as title, description, dates, and capacity. Upon submission,
 * the event is stored in Firebase, and the user is redirected to view their created events.
 *
 * @author Graham Flokstra
 * @author George
 * @author Dhairya Prajapati
 */
public class CreateEventFragment extends Fragment {
    private static final String TAG = "CreateEventFragment";
    private EditText titleEditText, descriptionEditText, capacityEditText, startDateEditText, endDateEditText;
    private EditText registrationOpensEditText, registrationDeadlineEditText, lotteryDayEditText, eventPriceEditText, waitlistLimitEditText;
    private CheckBox waitlistLimitCheckbox;
    private Button createEventButton, uploadImageButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private Uri selectedImageUri;
    private ImageView eventImageView;

    /**
     * Activity launcher for selecting an image from the device's gallery
     * @author Dhairya Prajapati
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Display the selected image in the ImageView
                        eventImageView.setImageURI(selectedImageUri);
                    }
                }
            }
    );

    /**
     * Called to create and initialize the fragment's UI, including setting up
     * all necessary fields for event creation and adding a listener to the submit button.
     *
     * @author Graham Flokstra
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        // Initialize Firebase and session manager services
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Initialize view elements for user input
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
        createEventButton = view.findViewById(R.id.createEventButton);

        // Set up click listener to handle event creation
        createEventButton.setOnClickListener(v -> createEvent());

        // Set up views and click listener to handle image uploading
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        eventImageView = view.findViewById(R.id.add_image_button);
        uploadImageButton.setOnClickListener(v -> openImagePicker());

        return view;
    }

    /**
     * Gathers input data from the form, validates it, and creates an Event object.
     * If the event is successfully created in Firebase, the user is redirected
     * to the view-my-events page. Displays a toast message upon success or failure.
     *
     * @author Graham Flokstra
     * @author George
     * @author Dhairya Prajapati
     */
    private void createEvent() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        Integer capacity = parseIntegerField(capacityEditText.getText().toString().trim());
        Double price = parseDoubleField(eventPriceEditText.getText().toString().trim());
        Timestamp startDate = parseDate(startDateEditText.getText().toString());
        Timestamp endDate = parseDate(endDateEditText.getText().toString());
        Timestamp registrationOpens = parseDate(registrationOpensEditText.getText().toString());
        Timestamp registrationDeadline = parseDate(registrationDeadlineEditText.getText().toString());
        Timestamp lotteryDay = parseDate(lotteryDayEditText.getText().toString());
        Integer waitlistLimit = waitlistLimitCheckbox.isChecked() ? parseIntegerField(waitlistLimitEditText.getText().toString().trim()) : null;

        // Retrieve the organizer ID from the session
        String organizerId = sessionManager.getUserSession().getUserId();

        if (organizerId == null) {
            Toast.makeText(requireContext(), "Error: No organizer ID found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construct the Event object with validated fields and organizer ID
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
        event.setOrganizerId(organizerId);  // Set the organizer ID

        // Attempt to store the event in Firebase
        firebaseService.createEvent(event, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String eventId) {
//                if (selectedImageUri != null){
//                    uploadImageToFirestore(eventId, selectedImageUri);
//                }
//                else{
//                Toast.makeText(requireContext(), "Event created successfully", Toast.LENGTH_SHORT).show();
//                Navigation.findNavController(requireView()).navigate(R.id.navigation_view_my_events);
//                }
                Toast.makeText(requireContext(), "Event created successfully", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.navigation_view_my_events);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }


    /**
     * Parses a String to an Integer, returning null if the string is empty or not a number.
     *
     * @author Graham Flokstra
     * @param value String value to parse into an Integer.
     * @return Parsed Integer or null if parsing fails.
     */
    private Integer parseIntegerField(String value) {
        try { return TextUtils.isEmpty(value) ? null : Integer.parseInt(value); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * Parses a String to a Double, returning null if the string is empty or not a number.
     *
     * @author Graham Flokstra
     * @param value String value to parse into a Double.
     * @return Parsed Double or null if parsing fails.
     */
    private Double parseDoubleField(String value) {
        try { return TextUtils.isEmpty(value) ? null : Double.parseDouble(value); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * Converts a date string in "yyyy/MM/dd" format to a Firebase Timestamp.
     * If parsing fails, returns null.
     *
     * @author Graham Flokstra
     * @param dateString Date in String format to parse into Timestamp.
     * @return Parsed Timestamp or null if parsing fails.
     */
    private Timestamp parseDate(String dateString) {
        try { return new Timestamp(new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(dateString)); }
        catch (ParseException e) { return null; }
    }

    /**
     * Opens the device's gallery for the user to pick a poster image for the event.
     * @author Dhairya Prajapati
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * Handles image upload to Firebase storage
     * @author Dhairya Prajapati
     * @param eventId The Id of the event being created by the user.
     * @param imageUri The URI of the image selected by the user.
     */
//    private void uploadImageToFirestore(String eventId, Uri imageUri) {
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//        StorageReference storageRef = storage.getReference();
//
//        // Create a reference to 'event_images/eventId.jpg'
//        String imagePath = "event_images/" + eventId + ".jpg";
//        StorageReference imageRef = storageRef.child(imagePath);
//
//        imageRef.putFile(imageUri)
//                .addOnSuccessListener(taskSnapshot -> {
//                    // Get the download URL
//                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
//                        // Update the event document with the image URL
//                        firebaseService.updateEventImageUrl(eventId, downloadUri.toString(), new FirebaseCallback<Void>() {
//                            @Override
//                            public void onSuccess(Void result) {
//                                showSuccessAndNavigate();
//                            }
//
//                            @Override
//                            public void onFailure(Exception e) {
//                                Toast.makeText(requireContext(), "Failed to update event image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    });
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
}

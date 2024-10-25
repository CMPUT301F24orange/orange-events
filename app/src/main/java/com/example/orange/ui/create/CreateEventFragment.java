package com.example.orange.ui.create;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

import java.util.Date;

/**
 * This fragment facilitates the creation of events by a UserType of 'Organizer'
 *
 * @author George
 */
public class CreateEventFragment extends Fragment {
    private static final String TAG = "CreateEventFragment";
    private EditText titleEditText, descriptionEditText, capacityEditText;
    private Button createEventButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        // Initialize services
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Verify user is an organizer
        if (!verifyOrganizerAccess()) {
            Navigation.findNavController(view).navigate(R.id.navigation_home);
            return view;
        }

        // Initialize views
        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        capacityEditText = view.findViewById(R.id.capacityEditText);
        createEventButton = view.findViewById(R.id.createEventButton);

        // Set up click listener
        createEventButton.setOnClickListener(v -> createEvent());

        return view;
    }

    /**
     * This function verifies if the user is an organizer
     *
     * @return Boolean
     */
    private boolean verifyOrganizerAccess() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return false;
        }

        UserType userType = sessionManager.getUserSession().getUserType();
        if (userType != UserType.ORGANIZER) {
            Toast.makeText(requireContext(), "Only organizers can create events", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * This function creates the event.
     */
    private void createEvent() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String capacityStr = capacityEditText.getText().toString().trim();

        if (!validateInputs(title, description)) {
            return;
        }

        Integer capacity = parseCapacity(capacityStr);
        if (capacity == null && !TextUtils.isEmpty(capacityStr)) {
            return;
        }

        String organizerId = sessionManager.getUserSession().getUserId();
        if (organizerId == null) {
            Toast.makeText(requireContext(), "Error: No organizer ID found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify organizer exists and get their data
        firebaseService.getUserByDeviceIdAndType(
                sessionManager.getUserSession().getdeviceId(),
                UserType.ORGANIZER,
                new FirebaseCallback<User>() {
                    @Override
                    public void onSuccess(User organizer) {
                        if (organizer != null) {
                            // Create and save the event
                            Event event = new Event(
                                    title,
                                    description,
                                    new Timestamp(new Date()),
                                    capacity,
                                    organizer.getId()
                            );

                            saveEventAndUpdateOrganizer(event, organizer);
                        } else {
                            Toast.makeText(requireContext(), "Error: Organizer not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(), "Error verifying organizer: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Saves the event to the proper organizer profile and places it in that organizers organizedEvents list
     *
     * @param event
     * @param organizer
     */
    private void saveEventAndUpdateOrganizer(Event event, User organizer) {
        firebaseService.createEvent(event, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String eventId) {
                if (getActivity() == null) return;

                // Add event to organizer's created events
                organizer.addEventOrganizing(eventId);

                // Update organizer in Firebase
                firebaseService.updateUser(organizer, new FirebaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        requireActivity().runOnUiThread(() -> {
                            Log.d(TAG, "Event created successfully with ID: " + eventId);
                            Toast.makeText(requireContext(), "Event created successfully", Toast.LENGTH_SHORT).show();
                            navigateToMyCreatedEvents();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Failed to update organizer's event list", e);
                            Toast.makeText(requireContext(), "Event created but failed to update organizer's list",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMyCreatedEvents();
                        });
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Failed to create event", e);
                    Toast.makeText(requireContext(), "Failed to create event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Input validator for create events page.
     *
     * @param title
     * @param description
     * @return
     */
    private boolean validateInputs(String title, String description) {
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
            titleEditText.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            Toast.makeText(requireContext(), "Description is required", Toast.LENGTH_SHORT).show();
            descriptionEditText.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Checks if capacity input is empty or not.
     *
     * @param capacityStr
     * @return Integer
     */
    private Integer parseCapacity(String capacityStr) {
        if (TextUtils.isEmpty(capacityStr)) {
            return null;
        }

        try {
            return Integer.parseInt(capacityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid capacity value", Toast.LENGTH_SHORT).show();
            capacityEditText.requestFocus();
            return null;
        }
    }

    /**
     * Used to redirect user to MyCreatedEvents page after creation.
     */
    private void navigateToMyCreatedEvents() {
        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_view_my_events);
    }
}
package com.example.orange.ui.facility;

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
import com.example.orange.data.model.Facility;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

/**
 * FacilityProfileFragment is responsible for managing the creation and editing of facility information
 * within the Orange application. This fragment provides a user interface for organizers to input and
 * modify facility details such as name and address.
 *
 * @author Graham Flokstra
 */
public class FacilityProfileFragment extends Fragment {

    private EditText facilityNameEditText;
    private EditText facilityAddressEditText;
    private Button saveButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private User currentUser;
    private Facility currentFacility;
    private static final String TAG = "FacilityProfileFragment";

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_facility_profile, container, false);

        initializeServices();
        initializeViews(view);
        setupClickListeners();

        return view;
    }

    /**
     * Initializes the Firebase and SessionManager services.
     */
    private void initializeServices() {
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
    }

    /**
     * Initializes the UI views and disables the save button until data is loaded.
     *
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        facilityNameEditText = view.findViewById(R.id.facility_name);
        facilityAddressEditText = view.findViewById(R.id.facility_address);
        saveButton = view.findViewById(R.id.facility_save_button);
        saveButton.setEnabled(false);

        loadUserData();
    }

    /**
     * Sets up click listeners for interactive elements in the UI.
     */
    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveFacility());
    }

    /**
     * Loads the current user's data from Firebase using the device ID and user type
     * stored in the session. If successful, enables the save button and loads any
     * associated facility data.
     */
    private void loadUserData() {
        UserSession userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Toast.makeText(requireContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceId = userSession.getdeviceId();
        UserType userType = userSession.getUserType();
        Log.d(TAG, "Loaded deviceId: " + deviceId + ", userType: " + userType);

        firebaseService.getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUser = user;
                    saveButton.setEnabled(true);
                    if (user.getFacilityId() != null && !user.getFacilityId().isEmpty()) {
                        Log.d(TAG, "User has facilityId: " + user.getFacilityId());
                        loadFacilityData(user.getFacilityId());
                    } else {
                        Log.d(TAG, "User has no facilityId");
                    }
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loads facility data from Firebase using the provided facility ID.
     * If successful, populates the UI fields with the facility information.
     *
     * @param facilityId The unique identifier of the facility to load
     */
    private void loadFacilityData(String facilityId) {
        Log.d(TAG, "Loading facility data for facilityId: " + facilityId);
        firebaseService.getFacilityById(facilityId, new FirebaseCallback<Facility>() {
            @Override
            public void onSuccess(Facility facility) {
                if (facility != null) {
                    currentFacility = facility;
                    facilityNameEditText.setText(facility.getName());
                    facilityAddressEditText.setText(facility.getAddress());
                    Log.d(TAG, "Facility data loaded: Name=" + facility.getName() + ", Address=" + facility.getAddress());
                } else {
                    Toast.makeText(requireContext(), "Facility data not found", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Facility data not found for facilityId: " + facilityId);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error loading facility data", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading facility data", e);
            }
        });
    }

    /**
     * Saves the facility information to Firebase. If no facility exists, creates a new one.
     * If a facility exists, updates the existing record. Validates input fields before saving.
     */
    private void saveFacility() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User data not loaded yet. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String facilityName = facilityNameEditText.getText().toString().trim();
        String facilityAddress = facilityAddressEditText.getText().toString().trim();

        if (TextUtils.isEmpty(facilityName) || TextUtils.isEmpty(facilityAddress)) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentFacility == null) {
            createNewFacility(facilityName, facilityAddress);
        } else {
            updateExistingFacility(facilityName, facilityAddress);
        }
    }

    /**
     * Creates a new facility with the provided name and address.
     *
     * @param facilityName The name of the facility
     * @param facilityAddress The address of the facility
     */
    private void createNewFacility(String facilityName, String facilityAddress) {
        Facility facility = new Facility(facilityName, facilityAddress);
        firebaseService.createFacility(facility, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String facilityId) {
                currentFacility = facility;
                Log.d(TAG, "Facility created with ID: " + facilityId);

                currentUser.setFacilityId(facilityId);
                updateUser();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error creating facility", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates an existing facility with new name and address information.
     *
     * @param facilityName The updated name of the facility
     * @param facilityAddress The updated address of the facility
     */
    private void updateExistingFacility(String facilityName, String facilityAddress) {
        currentFacility.setName(facilityName);
        currentFacility.setAddress(facilityAddress);
        firebaseService.updateFacility(currentFacility, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Facility updated", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error updating facility", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the current user's information in Firebase, specifically updating
     * the facility ID association.
     */
    private void updateUser() {
        firebaseService.updateUser(currentUser, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Facility saved and linked to your profile", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error updating user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
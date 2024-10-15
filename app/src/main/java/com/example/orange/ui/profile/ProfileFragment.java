package com.example.orange.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.UserSession;
import com.example.orange.utils.SessionManager;

import androidx.fragment.app.Fragment;

import com.example.orange.R;
import com.example.orange.data.model.User;

/**
 * ProfileFragment is responsible for displaying and managing the user's profile information.
 * It allows users to view and edit their name, email, and phone number, and to update their profile data in Firebase if they so choose.
 */
public class ProfileFragment extends Fragment {
    private EditText editTextName, editTextEmail, editTextPhone;
    private ImageView profileImage;
    private Button uploadImageButton, saveButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private User currentUser;

    /**
     * Called to have the fragment instantiate its user interface view. This method inflates the profile
     * fragment layout and initializes the necessary views and services.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null if not provided.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPhone = view.findViewById(R.id.editTextPhone);
        profileImage = view.findViewById(R.id.profile_image);
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        saveButton = view.findViewById(R.id.save_button);

        // Initialize FirebaseService and SessionManager
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Load user data from Firebase
        loadUserData();

        // Set save button listener
        saveButton.setOnClickListener(v -> saveUserProfile());

        return view;
    }

    /**
     * Loads the current user's profile data from Firebase using the user ID stored in the session.
     * It retrieves the user's data and populates the profile fields (name, email, phone) in the fragment.
     */
    private void loadUserData() {
        UserSession userSession = sessionManager.getUserSession();
        if (userSession != null) {
            String userId = userSession.getUserId();
            // Retrieve user by ID from Firebase
            firebaseService.getUserById(userId, new FirebaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        currentUser = user;
                        // Populate user data into the profile fields
                        editTextName.setText(user.getUsername());
                        editTextEmail.setText(user.getEmail());  // Ensure email exists in the User model
                        editTextPhone.setText(user.getPhone());  // Ensure phone exists in the User model
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Saves the current user's updated profile information (name, email, phone) to Firebase.
     * The updated data is saved under the user's unique ID.
     */
    private void saveUserProfile() {
        if (currentUser != null) {
            // Update the User object with new data from input fields
            currentUser.setUsername(editTextName.getText().toString());
            currentUser.setEmail(editTextEmail.getText().toString());
            currentUser.setPhone(editTextPhone.getText().toString());  // If phone is part of your user model

            // Save updated user data to Firebase
            firebaseService.updateUser(currentUser.getId(), currentUser, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

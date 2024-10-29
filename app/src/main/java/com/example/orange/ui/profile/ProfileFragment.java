package com.example.orange.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.example.orange.data.model.UserSession;
import com.example.orange.utils.SessionManager;
import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This fragment facilitates the viewing and editing of a user profile.
 *
 * @author graham flokstra
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private EditText editTextName, editTextEmail, editTextPhone;
    private ImageView profileImage;
    private Button uploadImageButton, saveButton, deleteImageButton, logoutButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private UserSession userSession;
    private User currentUser;
    private Uri selectedImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize services
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Initialize views
        initializeViews(view);

        // Verify session and load data
        verifySessionAndLoadData();

        return view;
    }

    /**
     * Verifies we are logged in and fetches that users data.
     */
    private void verifySessionAndLoadData() {
        // Get current session
        userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Log.e(TAG, "No user session found");
            navigateToHome("No active session");
            return;
        }

        // Get the user type and device ID
        UserType userType = userSession.getUserType();
        String deviceId = userSession.getdeviceId();

        // Verify Firebase session first
        firebaseService.getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    // Now that we have verified the user exists, load their full profile
                    loadUserData(user.getId());
                } else {
                    Log.e(TAG, "User not found in Firebase");
                    navigateToHome("User not found");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to verify user", e);
                navigateToHome("Error verifying user");
            }
        });
    }

    /**
     * Initializes the views
     *
     * @param view
     */
    private void initializeViews(View view) {
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPhone = view.findViewById(R.id.editTextPhone);
        profileImage = view.findViewById(R.id.profile_image);
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        saveButton = view.findViewById(R.id.save_button);
        deleteImageButton = view.findViewById(R.id.delete_image_button);
        logoutButton = view.findViewById(R.id.logout_button);

        // Disable buttons until user data is loaded
        setButtonsEnabled(false);

        // Set button listeners
        saveButton.setOnClickListener(v -> saveUserProfile(selectedImageUri));
        uploadImageButton.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        deleteImageButton.setOnClickListener(v -> deleteProfileImage());
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    /**
     * Enables buttons once our user is found.
     *
     * @param enabled
     */
    private void setButtonsEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        uploadImageButton.setEnabled(enabled);
        deleteImageButton.setEnabled(enabled);
    }

    /**
     * Loads user data for placement into the profile page
     *
     * @param userId
     */
    private void loadUserData(String userId) {
        Log.d(TAG, "Loading user data for ID: " + userId);

        firebaseService.getUserById(userId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUser = user;
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Setting user data to views");
                        editTextName.setText(user.getUsername());
                        editTextEmail.setText(user.getEmail());
                        editTextPhone.setText(user.getPhone());

                        if (user.getProfileImageData() != null) {
                            byte[] imageData = user.getProfileImageData().toBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                            profileImage.setImageBitmap(bitmap);
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile);
                        }

                        // Enable buttons after data is loaded
                        setButtonsEnabled(true);
                    });
                } else {
                    Log.e(TAG, "User data is null");
                    navigateToHome("Failed to load user data");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading user data", e);
                navigateToHome("Error loading profile");
            }
        });
    }

    /**
     * Returns user back to home.
     *
     * @param message
     */
    private void navigateToHome(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
            });
        }
    }

    /**
     * Saves user data to the correct user profile.
     *
     * @param imageUri
     */
    private void saveUserProfile(Uri imageUri) {
        if (currentUser == null || userSession == null) {
            Log.e(TAG, "No user data or session available");
            Toast.makeText(getContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure we have the device ID set
        String deviceId = userSession.getdeviceId();
        currentUser.setDeviceId(deviceId);  // Make sure device ID is set
        currentUser.setId(deviceId);        // Use device ID as document ID
        currentUser.setUserType(userSession.getUserType()); // Ensure user type is set

        Log.d(TAG, "Saving profile for user: " + deviceId);

        // Update user data
        currentUser.setUsername(editTextName.getText().toString().trim());
        currentUser.setEmail(editTextEmail.getText().toString().trim());
        currentUser.setPhone(editTextPhone.getText().toString().trim());

        if (imageUri != null) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Resize image
                int maxSize = 500;
                float scale = Math.min(((float)maxSize / bitmap.getWidth()), ((float)maxSize / bitmap.getHeight()));
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

                currentUser.setProfileImageData(Blob.fromBytes(imageData));
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error reading image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Save to Firebase using the updated user object
        firebaseService.updateUser(currentUser, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    selectedImageUri = null;
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error updating profile", e);
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Deletes users profile image.
     */
    private void deleteProfileImage() {
        if (currentUser != null && userSession != null) {
            currentUser.setProfileImageData(null);
            firebaseService.updateUser(currentUser, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Profile image deleted", Toast.LENGTH_SHORT).show();
                        profileImage.setImageResource(R.drawable.ic_profile);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    /**
     * Logs user out of userSession.
     */
    private void handleLogout() {
        firebaseService.logOut();
        sessionManager.logoutUser();
        Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
    }

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    profileImage.setImageURI(uri);
                    selectedImageUri = uri;
                }
            }
    );
}

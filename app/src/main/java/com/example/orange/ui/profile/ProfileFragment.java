package com.example.orange.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.ImageData;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserType;
import com.example.orange.data.model.UserSession;
import com.example.orange.utils.SessionManager;
import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ProfileFragment manages user profile functionality within the Orange application.
 * This fragment handles the display and editing of user information, including profile
 * images, personal details, and type-specific features for different user roles
 * (Entrant vs Organizer).
 *
 * @author Graham Flokstra
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;

    private ImageView profileImage;

    private Button uploadImageButton;
    private Button saveButton;
    private Button deleteImageButton;
    private Button logoutButton;
    private Button facilityButton;

    private CheckBox receiveNotificationsCheckbox;

    private FirebaseService firebaseService;

    private SessionManager sessionManager;
    private UserSession userSession;
    private User currentUser;

    private Uri selectedImageUri;

    /**
     * Activity result launcher for handling image selection from the device's media store.
     * When an image is selected, it updates the profile image view and stores the URI
     * for later processing.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    profileImage.setImageURI(uri);
                    selectedImageUri = uri;
                }
            }
    );

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater           LayoutInflater object to inflate views
     * @param container          If non-null, parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, fragment is being re-constructed from previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeServices();
        initializeViews(view);
        verifySessionAndLoadData();

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
     * Initializes all view components and sets up their click listeners.
     * Also handles initial visibility states based on user type.
     *
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        // Initialize view references
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPhone = view.findViewById(R.id.editTextPhone);
        profileImage = view.findViewById(R.id.profile_image);
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        saveButton = view.findViewById(R.id.save_button);
        deleteImageButton = view.findViewById(R.id.delete_image_button);
        logoutButton = view.findViewById(R.id.logout_button);
        receiveNotificationsCheckbox = view.findViewById(R.id.receive_notifications_checkbox);
        facilityButton = view.findViewById(R.id.facility_button);

        // Set initial button states
        setButtonsEnabled(false);

        // Set initial visibility
        facilityButton.setVisibility(View.GONE);
        receiveNotificationsCheckbox.setVisibility(View.GONE);

        // Set up click listeners
        setupClickListeners();
    }

    /**
     * Sets up click listeners for all interactive elements in the fragment.
     */
    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveUserProfile(selectedImageUri));
        uploadImageButton.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        deleteImageButton.setOnClickListener(v -> deleteProfileImage());
        logoutButton.setOnClickListener(v -> handleLogout());
        facilityButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_navigation_profile_to_facilityProfileFragment));
    }

    /**
     * Verifies the current user session and loads user data if session is valid.
     * Navigates to home screen if session is invalid.
     */
    private void verifySessionAndLoadData() {
        userSession = sessionManager.getUserSession();
        if (userSession == null) {
            Log.e(TAG, "No user session found");
            navigateToHome("No active session");
            return;
        }

        UserType userType = userSession.getUserType();
        String deviceId = userSession.getdeviceId();

        firebaseService.getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
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
     * Enables or disables interactive buttons based on user data availability.
     *
     * @param enabled true to enable buttons, false to disable
     */
    private void setButtonsEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        uploadImageButton.setEnabled(enabled);
        deleteImageButton.setEnabled(enabled);
    }

    /**
     * Loads user data from Firebase and populates the UI.
     * Handles different UI configurations based on user type.
     *
     * @param userId The ID of the user whose data should be loaded
     */
    private void loadUserData(String userId) {
        firebaseService.getUserById(userId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUser = user;
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        populateUserInterface(user);
                        configureUserTypeSpecificUI(user);
                        setButtonsEnabled(true);
                    });
                } else {
                    navigateToHome("Failed to load user data");
                }
            }

            @Override
            public void onFailure(Exception e) {
                navigateToHome("Error loading profile");
            }
        });
    }

    /**
     * Populates the user interface with the provided user data.
     *
     * @param user The user whose data should be displayed
     */
    private void populateUserInterface(User user) {
        editTextName.setText(user.getUsername());
        editTextEmail.setText(user.getEmail());
        editTextPhone.setText(user.getPhone());

        if (user.getProfileImageId() != null) {
            // Fetch the image data using the image ID
            firebaseService.getImageById(user.getProfileImageId(), new FirebaseCallback<ImageData>() {
                @Override
                public void onSuccess(ImageData imageData) {
                    if (imageData != null && imageData.getImageData() != null) {
                        byte[] imageBytes = imageData.getImageData().toBytes();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        profileImage.setImageBitmap(bitmap);
                    } else {
                        // Handle case where image data is null
                        profileImage.setImageBitmap(createInitialsBitmap(user.getUsername()));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // Handle failure
                    profileImage.setImageBitmap(createInitialsBitmap(user.getUsername()));
                }
            });
        } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            profileImage.setImageBitmap(createInitialsBitmap(user.getUsername()));
        }
    }

    /**
     * Configures UI elements specific to user type (Entrant or Organizer).
     *
     * @param user The user whose type determines the UI configuration
     */
    private void configureUserTypeSpecificUI(User user) {
        if (user.getUserType() == UserType.ENTRANT) {
            receiveNotificationsCheckbox.setVisibility(View.VISIBLE);
            facilityButton.setVisibility(View.GONE);
            receiveNotificationsCheckbox.setChecked(user.isReceiveNotifications());
        } else if (user.getUserType() == UserType.ORGANIZER) {
            receiveNotificationsCheckbox.setVisibility(View.GONE);
            facilityButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Navigates to the home screen with a toast message.
     *
     * @param message Message to display in toast
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
     * Saves the user profile with updated information and optional new profile image.
     *
     * @param imageUri URI of the new profile image, if one was selected
     */
    private void saveUserProfile(Uri imageUri) {
        if (currentUser == null || userSession == null) {
            Toast.makeText(getContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        updateUserFields();

        if (imageUri != null) {
            // Process and upload the new profile image
            processAndUploadProfileImage(imageUri);
        } else {
            // No new image, update the user profile
            updateUserProfile();
        }
    }

    /**
     * Updates user fields from UI input.
     */
    private void updateUserFields() {
        currentUser.setDeviceId(userSession.getdeviceId());
        currentUser.setUserType(userSession.getUserType());
        currentUser.setUsername(editTextName.getText().toString().trim());
        currentUser.setEmail(editTextEmail.getText().toString().trim());
        currentUser.setPhone(editTextPhone.getText().toString().trim());

        if (currentUser.getUserType() == UserType.ENTRANT) {
            currentUser.setReceiveNotifications(receiveNotificationsCheckbox.isChecked());
        }
    }

    /**
     * Processes and uploads the profile image.
     *
     * @param imageUri URI of the image to process
     */
    private void processAndUploadProfileImage(Uri imageUri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            int maxSize = 500;
            float scale = Math.min(((float) maxSize / bitmap.getWidth()),
                    ((float) maxSize / bitmap.getHeight()));
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageData = baos.toByteArray();

            if (imageData.length > 1048576) {
                Toast.makeText(getContext(), "Image is too large to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            Blob imageBlob = Blob.fromBytes(imageData);

            // Upload image to Firebase
            firebaseService.createImage(imageBlob, new FirebaseCallback<String>() {
                @Override
                public void onSuccess(String imageId) {
                    // Set the image ID in the user
                    currentUser.setProfileImageId(imageId);
                    // Now update the user profile
                    updateUserProfile();
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
     * Updates the user profile in Firebase.
     */
    private void updateUserProfile() {
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
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Deletes the user's profile image and updates UI accordingly.
     */
    private void deleteProfileImage() {
        if (currentUser != null && userSession != null) {
            String imageIdToDelete = currentUser.getProfileImageId();
            currentUser.setProfileImageId(null);

            firebaseService.updateUser(currentUser, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (imageIdToDelete != null) {
                        // Delete the image from Firebase
                        firebaseService.deleteImage(imageIdToDelete, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (getActivity() == null) return;

                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Profile image deleted", Toast.LENGTH_SHORT).show();
                                    profileImage.setImageBitmap(createInitialsBitmap(currentUser.getUsername()));
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                if (getActivity() == null) return;

                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Failed to delete image from Firebase", Toast.LENGTH_SHORT).show()
                                );
                            }
                        });
                    } else {
                        if (getActivity() == null) return;

                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Profile image deleted", Toast.LENGTH_SHORT).show();
                            profileImage.setImageBitmap(createInitialsBitmap(currentUser.getUsername()));
                        });
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to delete image reference", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    /**
     * Handles user logout by navigating to the home screen and clearing the session.
     */
    private void handleLogout() {
        firebaseService.logOut();
        sessionManager.logoutUser();
        if (getActivity() != null) {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

    /**
     * Creates a bitmap containing the user's initials in a circular background.
     * This is used as a default profile picture when no image is uploaded.
     *
     * @param name The user's full name from which to extract initials
     * @return Bitmap containing the user's initials in a styled format
     */
    private Bitmap createInitialsBitmap(String name) {
        int size = 120;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.LTGRAY);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(48f);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        String initials = getInitials(name);

        Rect bounds = new Rect();
        paint.getTextBounds(initials, 0, initials.length(), bounds);
        float x = size / 2f;
        float y = size / 2f - bounds.exactCenterY();

        canvas.drawText(initials, x, y, paint);
        return bitmap;
    }

    /**
     * Extracts initials from a user's name. Takes the first letter of each word
     * in the name, up to a maximum of two letters.
     *
     * @param name The full name to extract initials from
     * @return String containing the user's initials (max 2 characters)
     */
    private String getInitials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.length() > 2 ? initials.substring(0, 2) : initials.toString();
    }
}

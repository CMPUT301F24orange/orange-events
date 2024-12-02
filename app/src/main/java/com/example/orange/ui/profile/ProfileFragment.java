package com.example.orange.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.Manifest;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ProfileFragment manages user profile functionality within the Orange application.
 *
 * @author Graham Flokstra
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;

    private ImageView profileImage;

    private ImageButton uploadImageButton;
    private ImageButton saveButton;
    private ImageButton deleteImageButton;
    private ImageButton logoutButton;
    private ImageButton facilityButton;

    private CheckBox receiveNotificationsCheckbox;

    private FirebaseService firebaseService;

    private SessionManager sessionManager;
    private UserSession userSession;
    private User currentUser;

    private Uri selectedImageUri;
    /**
     * Launcher to handle image picking using the new Activity Result API.
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
     * Listener for changes in the receiveNotificationsCheckbox state.
     * It handles enabling or disabling notifications based on the checkbox state.
     */
    private CompoundButton.OnCheckedChangeListener notificationsListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (currentUser != null && userSession != null) {
                if (isChecked) {
                    // Request notification permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        // Permissions are granted at install time for lower SDK versions
                        enableNotifications();
                    }
                } else {
                    // Disable notifications
                    disableNotifications();
                }
            }
        }
    };

    /**
     * Launcher to handle the result of the notification permission request.
     * It enables notifications if permission is granted, or reverts the checkbox state if denied.
     */
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, proceed to enable notifications
                    enableNotifications();
                } else {
                    // Permission denied, revert checkbox and inform the user
                    Toast.makeText(getContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
                    receiveNotificationsCheckbox.setOnCheckedChangeListener(null);
                    receiveNotificationsCheckbox.setChecked(false);
                    receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                }
            });

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
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
     * Initializes the FirebaseService and SessionManager instances.
     */
    private void initializeServices() {
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
    }

    /**
     * Initializes all view components and sets up their initial states and listeners.
     *
     * @param view The root view of the fragment's layout.
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

        // Set up the checkbox listener
        receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
    }

    /**
     * Sets up click listeners for various buttons within the fragment.
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
     * Verifies the user session and loads user data from Firebase Firestore.
     * If no session is found or user data cannot be loaded, navigates back to the home screen.
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
                    currentUser = user;
                    loadUserData(user.getId());
                    // Only update FCM token if notifications are enabled
                    if (currentUser.isReceiveNotifications()) {
                        updateFCMToken(user.getId());
                    }
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
     * Updates the Firebase Cloud Messaging (FCM) token for the user in Firestore.
     *
     * @param userId The unique identifier of the user.
     */
    private void updateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (currentUser != null && token != null && !token.isEmpty()) {
                        currentUser.setFcmToken(token);
                        firebaseService.setUserFCMToken(userId, token, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "FCM token updated successfully for user: " + userId);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Failed to update FCM token for user: " + userId, e);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve FCM token", e));
    }

    /**
     * Enables or disables buttons based on the provided state.
     *
     * @param enabled If true, buttons are enabled; otherwise, they are disabled.
     */
    private void setButtonsEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        uploadImageButton.setEnabled(enabled);
        deleteImageButton.setEnabled(enabled);
    }

    /**
     * Loads the user's data from Firestore and populates the UI components.
     *
     * @param userId The unique identifier of the user.
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
     * Populates the user interface with the user's data.
     *
     * @param user The User object containing user data.
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
     * Configures UI elements based on the user's type (ENTRANT or ORGANIZER).
     *
     * @param user The User object containing user data.
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
     * Navigates the user back to the home screen with an optional message.
     *
     * @param message The message to display to the user before navigation.
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
     * Saves the user's profile information, including handling image uploads and notification preferences.
     *
     * @param imageUri The URI of the selected profile image, if any.
     */
    private void saveUserProfile(Uri imageUri) {
        if (currentUser == null || userSession == null) {
            Toast.makeText(getContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update other user fields
        updateUserFields();

        if (imageUri != null) {
            // Process and upload the new profile image
            processAndUploadProfileImage(imageUri);
        } else {
            // No new image, update the user profile
            updateUserProfile();
        }

        // Handle notification preferences after other updates
        handleNotificationPreferences();
    }

    /**
     * Updates the user's basic information fields based on the input fields.
     */
    private void updateUserFields() {
        currentUser.setUsername(editTextName.getText().toString().trim());
        currentUser.setDeviceId(userSession.getdeviceId());
        currentUser.setUserType(userSession.getUserType());

        currentUser.setEmail(editTextEmail.getText().toString().trim());
        currentUser.setPhone(editTextPhone.getText().toString().trim());

        if (currentUser.getUserType() == UserType.ENTRANT) {
            // Notifications are handled separately; no need to set here
            // currentUser.setReceiveNotifications(receiveNotificationsCheckbox.isChecked());
        }
    }

    /**
     * Processes the selected profile image by resizing and compressing it before uploading to Firebase.
     *
     * @param imageUri The URI of the selected image.
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

            if (imageData.length > 1048576) { // 1MB
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
     * Updates the user's profile information in Firestore.
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
     * Deletes the user's current profile image from Firebase and updates the UI accordingly.
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
     * Handles the logout process by clearing the session and navigating to the home screen.
     */
    private void handleLogout() {
        firebaseService.logOut();
        sessionManager.logoutUser();
        if (getActivity() != null) {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

    /**
     * Creates a bitmap image with the user's initials, used as a placeholder when no profile image is available.
     *
     * @param name The full name of the user.
     * @return A Bitmap containing the user's initials.
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
     * Extracts the initials from the user's full name.
     *
     * @param name The full name of the user.
     * @return A string containing up to two uppercase initials.
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

    /**
     * Handles enabling notifications by updating the FCM token and the user's notification preference in Firestore.
     * If enabling fails, it reverts the checkbox state and notifies the user.
     */
    private void enableNotifications() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (currentUser != null && token != null && !token.isEmpty()) {
                        currentUser.setFcmToken(token);
                        currentUser.setReceiveNotifications(true);
                        firebaseService.setUserFCMToken(currentUser.getId(), token, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(getContext(), "Notifications enabled", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Failed to enable notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // Revert the checkbox state without triggering listeners
                                receiveNotificationsCheckbox.setOnCheckedChangeListener(null);
                                receiveNotificationsCheckbox.setChecked(false);
                                receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                            }
                        });
                        // Also update the receiveNotifications field in Firestore
                        firebaseService.updateUserReceiveNotifications(currentUser.getId(), true, new FirebaseCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                // Successfully updated receiveNotifications in Firestore
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Failed to update notification preference: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to retrieve FCM token", Toast.LENGTH_SHORT).show();
                    // Revert the checkbox state without triggering listeners
                    receiveNotificationsCheckbox.setOnCheckedChangeListener(null);
                    receiveNotificationsCheckbox.setChecked(false);
                    receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                });
    }

    /**
     * Handles disabling notifications by removing the FCM token and updating the user's notification preference in Firestore.
     * If disabling fails, it reverts the checkbox state and notifies the user.
     */
    private void disableNotifications() {
        if (currentUser != null) {
            currentUser.setFcmToken(null);
            currentUser.setReceiveNotifications(false);
            firebaseService.removeUserFCMToken(currentUser.getId(), new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Notifications disabled", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to disable notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Revert the checkbox state without triggering listeners
                    receiveNotificationsCheckbox.setOnCheckedChangeListener(null);
                    receiveNotificationsCheckbox.setChecked(true);
                    receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                }
            });
            // Also update the receiveNotifications field in Firestore
            firebaseService.updateUserReceiveNotifications(currentUser.getId(), false, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Successfully updated receiveNotifications in Firestore
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to update notification preference: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Handles the process of updating notification preferences based on the checkbox state.
     * It either enables or disables notifications accordingly.
     */
    private void handleNotificationPreferences() {
        boolean wantsNotifications = receiveNotificationsCheckbox.isChecked();

        if (wantsNotifications) {
            // User wants to enable notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Request notification permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Permissions are granted at install time for lower SDK versions
                enableNotifications();
            }
        } else {
            // User wants to disable notifications
            disableNotifications();
        }
    }
}
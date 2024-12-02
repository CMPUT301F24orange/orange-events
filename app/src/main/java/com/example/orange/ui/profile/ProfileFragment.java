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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.CompoundButton;

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
import com.example.orange.ui.notifications.EntrantNotifications;
import com.example.orange.utils.SessionManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.Blob;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ProfileFragment manages user profile functionality within the Orange application.
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

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    profileImage.setImageURI(uri);
                    selectedImageUri = uri;
                }
            }
    );

    // Define the listener as a class-level variable for reusability
    private CompoundButton.OnCheckedChangeListener notificationsListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (currentUser != null && userSession != null) {
                // Disable the listener to prevent infinite loops during programmatic changes
                receiveNotificationsCheckbox.setOnCheckedChangeListener(null);

                if (isChecked) {
                    // User wants to receive notifications
                    FirebaseMessaging.getInstance().getToken()
                            .addOnSuccessListener(token -> {
                                if (token != null && !token.isEmpty()) {
                                    currentUser.setFcmToken(token);
                                    firebaseService.setUserFCMToken(currentUser.getId(), token, new FirebaseCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            Toast.makeText(getContext(), "Notifications enabled", Toast.LENGTH_SHORT).show();
                                            // Re-enable the listener
                                            receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(getContext(), "Failed to enable notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            // Revert the checkbox state
                                            receiveNotificationsCheckbox.setChecked(false);
                                            // Re-enable the listener
                                            receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                                        }
                                    });
                                } else {
                                    Toast.makeText(getContext(), "Failed to retrieve FCM token", Toast.LENGTH_SHORT).show();
                                    // Revert the checkbox state
                                    receiveNotificationsCheckbox.setChecked(false);
                                    // Re-enable the listener
                                    receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to retrieve FCM token", Toast.LENGTH_SHORT).show();
                                // Revert the checkbox state
                                receiveNotificationsCheckbox.setChecked(false);
                                // Re-enable the listener
                                receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                            });
                } else {
                    // User does not want to receive notifications
                    currentUser.setFcmToken(null);
                    firebaseService.removeUserFCMToken(currentUser.getId(), new FirebaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(getContext(), "Notifications disabled", Toast.LENGTH_SHORT).show();
                            // Re-enable the listener
                            receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "Failed to disable notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // Revert the checkbox state
                            receiveNotificationsCheckbox.setChecked(true);
                            // Re-enable the listener
                            receiveNotificationsCheckbox.setOnCheckedChangeListener(notificationsListener);
                        }
                    });
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeServices();
        initializeViews(view);
        verifySessionAndLoadData();

        return view;
    }

    private void initializeServices() {
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
    }

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

    private void setButtonsEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        uploadImageButton.setEnabled(enabled);
        deleteImageButton.setEnabled(enabled);
    }

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

    private void navigateToHome(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
            });
        }
    }

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

    private void updateUserFields() {
        currentUser.setUsername(editTextName.getText().toString().trim());
        currentUser.setDeviceId(userSession.getdeviceId());
        currentUser.setUserType(userSession.getUserType());

        currentUser.setEmail(editTextEmail.getText().toString().trim());
        currentUser.setPhone(editTextPhone.getText().toString().trim());

        if (currentUser.getUserType() == UserType.ENTRANT) {
            // Notifications are handled immediately; no need to set here
            // currentUser.setReceiveNotifications(receiveNotificationsCheckbox.isChecked());
        }
    }

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

    private void handleLogout() {
        firebaseService.logOut();
        sessionManager.logoutUser();
        if (getActivity() != null) {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

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

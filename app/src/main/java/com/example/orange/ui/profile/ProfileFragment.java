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
 * Author: Graham Flokstra
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private EditText editTextName, editTextEmail, editTextPhone;
    private ImageView profileImage;
    private Button uploadImageButton, saveButton, deleteImageButton, logoutButton;
    private CheckBox receiveNotificationsCheckbox;
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
     * Verifies we are logged in and fetches that user's data.
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
     * Initializes the views.
     *
     * @param view The root view of the fragment.
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
        receiveNotificationsCheckbox = view.findViewById(R.id.receive_notifications_checkbox);

        setButtonsEnabled(false);

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
     * @param enabled Whether buttons should be enabled.
     */
    private void setButtonsEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        uploadImageButton.setEnabled(enabled);
        deleteImageButton.setEnabled(enabled);
    }

    /**
     * Loads user data for placement into the profile page.
     *
     * @param userId The user's ID.
     */
    private void loadUserData(String userId) {
        firebaseService.getUserById(userId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUser = user;
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        editTextName.setText(user.getUsername());
                        editTextEmail.setText(user.getEmail());
                        editTextPhone.setText(user.getPhone());

                        // Set profile image or initials drawable
                        if (user.getProfileImageData() != null) {
                            byte[] imageData = user.getProfileImageData().toBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                            profileImage.setImageBitmap(bitmap);
                        } else {
                            Bitmap initialsBitmap = createInitialsBitmap(user.getUsername());
                            profileImage.setImageBitmap(initialsBitmap);
                        }

                        receiveNotificationsCheckbox.setChecked(user.isReceiveNotifications());
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
     * Navigates the user back to the home screen.
     *
     * @param message Message to display upon navigation.
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
     * @param imageUri URI of the user's profile image.
     */
    private void saveUserProfile(Uri imageUri) {
        if (currentUser == null || userSession == null) {
            Toast.makeText(getContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceId = userSession.getdeviceId();
        currentUser.setDeviceId(deviceId);
        currentUser.setId(deviceId);
        currentUser.setUserType(userSession.getUserType());

        currentUser.setUsername(editTextName.getText().toString().trim());
        currentUser.setEmail(editTextEmail.getText().toString().trim());
        currentUser.setPhone(editTextPhone.getText().toString().trim());
        currentUser.setReceiveNotifications(receiveNotificationsCheckbox.isChecked());

        if (imageUri != null) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                int maxSize = 500;
                float scale = Math.min(((float) maxSize / bitmap.getWidth()), ((float) maxSize / bitmap.getHeight()));
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

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
        } else if (currentUser.getProfileImageData() == null) {
            Bitmap initialsBitmap = createInitialsBitmap(currentUser.getUsername());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            initialsBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            currentUser.setProfileImageData(Blob.fromBytes(baos.toByteArray()));
        }

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
     * Deletes user's profile image.
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
                        profileImage.setImageBitmap(createInitialsBitmap(currentUser.getUsername()));
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
     * Logs the user out of the current session.
     */
    private void handleLogout() {
        firebaseService.logOut();
        sessionManager.logoutUser();
        Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
    }

    /**
     * Generates a bitmap with initials for the user's name.
     *
     * @author Graham Flokstra
     * @link https://dev.to/pranavpandey/android-create-bitmap-from-a-view-3lck
     * @param name User's name.
     * @return Bitmap with initials.
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
     * Extracts initials from the user's name.
     *
     * @param name User's name.
     * @return Initials.
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

package com.example.orange.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.utils.ImageLoader;
import com.example.orange.utils.SessionManager;
import com.google.firebase.firestore.Blob;
import com.google.rpc.Help;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ProfileFragment is responsible for displaying and managing the user's profile information.
 * It allows users to view and edit their name, email, and phone number, and to update their profile data in Firebase.
 */
public class ProfileFragment extends Fragment {
    private EditText editTextName, editTextEmail, editTextPhone;
    private ImageView profileImage;
    private Button uploadImageButton, saveButton, deleteImageButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private User currentUser;
    private Uri selectedImageUri;  // Field to store the selected image Uri

    /**
     * Called to have the fragment instantiate its user interface view. This method inflates the profile
     * fragment layout and initializes the necessary views and services.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
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
        deleteImageButton = view.findViewById(R.id.delete_image_button);

        // Initialize services
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Load user data from Firebase
        loadUserData();

        // Set save button listener
        saveButton.setOnClickListener(v -> {
            // Pass the selected image Uri to saveUserProfile
            saveUserProfile(selectedImageUri);
        });

        // Set on click listener for image uploader
        uploadImageButton.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE) // Allow only images
                    .build());
        });

        // Set on click listener for delete image button
        deleteImageButton.setOnClickListener(v -> {
            deleteProfileImage();
        });

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
                        editTextEmail.setText(user.getEmail());
                        editTextPhone.setText(user.getPhone());

                        // Load profile image if available
                        // Load profile image if available
                        if (user.getProfileImageData() != null) {
                            ImageLoader.loadImage(getContext(), user.getProfileImageData().toBytes(), profileImage);
                        } else if (user.getProfileImageUrl() != null) {
                            // If profileImageData is null but profileImageUrl is set, load from URL
                            ImageLoader.loadImage(getContext(), user.getProfileImageUrl(), profileImage);
                        } else {
//                            profileImage.setImageResource(R.drawable.default_profile);
                        }

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
     * Saves the user's profile information and uploads the profile image if provided.
     * Updates the user's data in Firestore.
     *
     * @param imageUri The Uri of the selected image, or null if no image is selected.
     * @see Help.Link https://developer.android.com/reference/android/graphics/Bitmap
     * @see Help.Link https://stackoverflow.com/questions/4837715/how-to-resize-a-bitmap-in-android
     * @see Help.Link https://docs.oracle.com/javase/7/docs/api/java/io/ByteArrayOutputStream.html
     */
    private void saveUserProfile(Uri imageUri) {
        if (currentUser != null) {
            // Update the User object with new data from input fields
            currentUser.setUsername(editTextName.getText().toString());
            currentUser.setEmail(editTextEmail.getText().toString());
            currentUser.setPhone(editTextPhone.getText().toString());

            // Deprecated imageURI use because I thought it might not be within the scope of the project to use that
            // Don't want to remove this just yet incase we are allowed to use it
            if (imageUri != null) {
                try {
                    // Convert the image Uri to a bitmap
                    InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    // Optionally resize the bitmap to reduce size
                    //Can help us keep the data size small to allow us to store as Byte map in firestore
                    int maxHeight = 500;
                    int maxWidth = 500;
                    float scale = Math.min(((float)maxHeight / bitmap.getHeight()), ((float)maxWidth / bitmap.getWidth()));
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);
                    Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    // Compress the bitmap to reduce size
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // Adjust compression as needed
                    byte[] imageData = baos.toByteArray();

                    // Ensure the image size is within Firestore limits
                    if (imageData.length > 1048576) { // 1 MiB limit
                        Toast.makeText(getContext(), "Image is too large to upload", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Convert byte[] to Blob
                    Blob imageBlob = Blob.fromBytes(imageData);

                    currentUser.setProfileImageData(imageBlob);
                    // Remove profileImageUrl if any
                    currentUser.setProfileImageUrl(null);

                    // Update user profile in Firestore
                    firebaseService.updateUser(currentUser.getId(), currentUser, new FirebaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            // Refresh the profile image
                            ImageLoader.loadImage(getContext(), imageData, profileImage);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error reading image", Toast.LENGTH_SHORT).show();
                }
            } else {
                // No image selected, just update user data
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

    /**
     * Deletes the user's profile image and updates Firestore.
     */
    private void deleteProfileImage() {
        if (currentUser.getProfileImageData() != null) {
            currentUser.setProfileImageData(null);
            // Also remove profileImageUrl if any
            currentUser.setProfileImageUrl(null);

            firebaseService.updateUser(currentUser.getId(), currentUser, new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Profile image deleted successfully", Toast.LENGTH_SHORT).show();
//                    profileImage.setImageResource(R.drawable.blank);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to update user", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No profile image to delete", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Registers a photo picker activity launcher in single-select mode.
     * Allows the user to pick an image from their device.
     *
     * @see Help.Link https://developer.android.com/training/data-storage/shared/photopicker#java
     */
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    profileImage.setImageURI(uri);  // Set the image in the ImageView
                    selectedImageUri = uri;         // Store the selected image Uri
                }
            }
    );
}

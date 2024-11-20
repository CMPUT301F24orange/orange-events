package com.example.orange.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
// Other imports...
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.ImageData;
import com.example.orange.data.model.User;
// Removed Blob import since it's no longer used
// import com.google.firebase.firestore.Blob;

import java.util.List;

/**
 * AdminProfilesFragment is responsible for displaying all the users
 * that are currently stored in the database. Each user also contains a
 * delete button to delete the user from the database entirely.
 *
 * @author
 */
public class AdminProfilesFragment extends Fragment {

    private LinearLayout profilesListContainer;
    private FirebaseService firebaseService;

    /**
     * Called to initialize the fragment's view.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_admin_profiles, container, false);

        firebaseService = new FirebaseService();
        profilesListContainer = view.findViewById(R.id.admin_profiles_container);

        // Load the users from the database
        loadUsers();

        return view;
    }

    /**
     * Loads all users from Firebase and calls displayUsers to render them.
     */
    private void loadUsers() {
        firebaseService.getAllUsers(new FirebaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                displayUsers(users);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a list of all users, rendering the picture, name, email,
     * and phone number for each user and allowing the
     * admin to delete the user if necessary.
     *
     * @param users List of user objects representing all users in the database
     */
    private void displayUsers(List<User> users) {
        profilesListContainer.removeAllViews();

        for (User user : users) {
            View userView = getLayoutInflater().inflate(R.layout.item_admin_profile_list, profilesListContainer, false);
            ImageView userProfilePicture = userView.findViewById(R.id.profile_image);
            TextView userName = userView.findViewById(R.id.profile_name);
            TextView userEmail = userView.findViewById(R.id.profile_email);
            TextView userPhone = userView.findViewById(R.id.profile_phone);
            Button deleteButton = userView.findViewById(R.id.profile_delete_button);

            String profileImageId = user.getProfileImageId();
            if (profileImageId != null) {
                firebaseService.getImageById(profileImageId, new FirebaseCallback<ImageData>() {
                    @Override
                    public void onSuccess(ImageData imageData) {
                        if (imageData != null && imageData.getImageData() != null) {
                            byte[] imageBytes = imageData.getImageData().toBytes();
                            Bitmap profileBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            userProfilePicture.setImageBitmap(profileBitmap);
                        } else {
                            // Handle case where image data is null
                            userProfilePicture.setImageResource(R.drawable.ic_profile);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle failure
                        userProfilePicture.setImageResource(R.drawable.ic_profile);
                    }
                });
            } else {
                // No profile image, set placeholder or generate initials
                userProfilePicture.setImageResource(R.drawable.ic_profile);
            }

            userName.setText(user.getUsername());
            userEmail.setText(user.getEmail());
            userPhone.setText(user.getPhone());
            deleteButton.setOnClickListener(v -> delUser(user.getId()));

            // Add the user view to the container
            profilesListContainer.addView(userView);
        }
    }

    /**
     * Deletes a user entirely from the database with no trace left.
     *
     * @param userId Unique ID of the user to be deleted.
     */
    public void delUser(String userId) {
        firebaseService.deleteUserAndRelatedFacilities(userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "User and related facilities successfully deleted.", Toast.LENGTH_SHORT).show();
                loadUsers();  // Refresh the users list to reflect deletion
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to delete user and related facilities.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

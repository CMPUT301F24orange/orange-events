package com.example.orange.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.User;
import com.google.firebase.firestore.Blob;


import java.util.List;

/**
 * AdminProfilesFragment is responsible for displaying all the users
 * that are currently stored in the database.
 * Each user also contains a delete button to delete the user from the database entirely.
 * @author Radhe Patel
 */
public class AdminProfilesFragment extends Fragment {

    private LinearLayout profilesListContainer;
    private FirebaseService firebaseService;

    /**
     * Called to initialize the fragment's view.
     *
     * @author Radhe Patel
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
     * @author Radhe Patel
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
     * Displays a list of all users, rendering the picture, name, email, and phone number
     * for each user to allow the admin to delete the user if necessary.
     * @author Radhe Patel
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

            Blob profilePictureData = user.getProfileImageData();
            if (profilePictureData != null) {
                Bitmap profileBitmap = convertBlobToBitmap(profilePictureData);
                if (profileBitmap != null) {
                    userProfilePicture.setImageBitmap(profileBitmap);
                }
            }

            userName.setText(user.getUsername());
            userEmail.setText(user.getEmail());
            userPhone.setText(user.getPhone());
            deleteButton.setOnClickListener(v -> delUser(user.getId()));

            // Add the event view to the container
            profilesListContainer.addView(userView);
        }
    }

    /**
     * Deletes a user entirely from the database with no trace left.
     * @author Radhe Patel
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

    /**
     * Converts the image data that of type Blob to a bitmap
     * that is usable and can be projected onto an ImageView
     *
     * @author Radhe Patel
     * @param blob profile picture data of the user
     */
    public Bitmap convertBlobToBitmap(Blob blob) {
        if (blob != null) {
            byte[] blobBytes = blob.toBytes();
            return BitmapFactory.decodeByteArray(blobBytes, 0, blobBytes.length);
        }
        return null;
    }

}


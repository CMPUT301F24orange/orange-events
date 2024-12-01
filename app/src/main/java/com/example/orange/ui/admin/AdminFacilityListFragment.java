package com.example.orange.ui.admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Facility;

import java.util.Date;
import java.util.List;

/**
 * AdminFacilityListFragment is responsible for displaying all the facilities
 * that are currently stored in the database. Each facility also contains a
 * delete button to delete the facility from the database entirely.
 *
 * @author Radhe Patel
 */
public class AdminFacilityListFragment extends Fragment {

    private LinearLayout facilityListContainer;
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
        View view = inflater.inflate(R.layout.fragment_admin_facility_list, container, false);

        firebaseService = new FirebaseService();
        facilityListContainer = view.findViewById(R.id.admin_facilities_container);

        // Load the facilities from the database
        loadFacilities();

        return view;
    }

    /**
     * Loads all facilities from Firebase and calls displayFacilities to render them.
     *
     * @author Radhe Patel
     */
    private void loadFacilities() {
        firebaseService.getAllFacilities(new FirebaseCallback<List<Facility>>() {
            @Override
            public void onSuccess(List<Facility> facilities) {
                displayFacilities(facilities);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a list of all facilities , rendering the name and address
     *  for each facility and allowing the
     * admin to delete the facility if necessary.
     *
     * @author Radhe Patel
     * @param facilities List of Event objects representing all events in the database
     */
    private void displayFacilities(List<Facility> facilities) {
        facilityListContainer.removeAllViews();

        for (Facility facility : facilities) {
            View facilityView = getLayoutInflater().inflate(R.layout.item_admin_facility_list, facilityListContainer, false);

            TextView facilityName = facilityView.findViewById(R.id.facility_name);
            TextView facilityAddress = facilityView.findViewById(R.id.facility_address);
            ImageButton deleteButton = facilityView.findViewById(R.id.facility_remove_button);

            facilityName.setText(facility.getName());

            facilityAddress.setText(facility.getAddress());

            deleteButton.setOnClickListener(v -> delFacility(facility.getId()));

            // Add the event view to the container
            facilityListContainer.addView(facilityView);
        }
    }

    /**
     * Deletes a facility entirely from the database with no trace left.
     *
     * @author Radhe Patel, Graham Flokstra
     * @param facilityId Unique ID of the event to be deleted.
     */
    public void delFacility(String facilityId) {
        firebaseService.deleteFacilityAndRelatedEvents(facilityId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Facility and related events successfully deleted.", Toast.LENGTH_SHORT).show();
                loadFacilities();  // Refresh the facilities list to reflect deletion
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to delete facility and related events.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}


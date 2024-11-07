package com.example.orange.ui.admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.orange.R;

/**
 * AdminFacilityListFragment is responsible for displaying all the facilities
 * that are currently stored in the database. Each facility also contains a
 * delete button to delete the facility from the database entirely.
 *
 * @author Radhe Patel
 */
public class AdminFacilityListFragment extends Fragment {

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_facility_list, container, false);
        return view;
    }
}
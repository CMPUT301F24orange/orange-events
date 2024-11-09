package com.example.orange.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.orange.MainActivity;
import com.example.orange.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * AdminFragment is responsible for initializing the admin view,
 * which includes initializing the bottom navigation bar, the top toolbar
 * and setting up the buttons for each.
 *
 * @author Radhe Patel
 */
public class AdminFragment extends Fragment {

    private NavController navController;
    private BottomNavigationView navView;


    /**
     * Initializes the NavController and the bottom navigation bar.
     * then it updates the bottom navigation bar to the admin view by
     * calling the updateBottomNavForAdmin method.
     *
     * @author Radhe Patel
     * @param context The context to which the fragment is being attached.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Access the NavController and BottomNavigationView from the parent activity
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navView = requireActivity().findViewById(R.id.nav_view);

        // Configure the admin bottom navigation menu
        updateBottomNavForAdmin();
    }

    /**
     * Sets both the icons and text for the navigation bar on the bottom for the admin view.
     * Also sends the user back to the MainActivity (app launch view) if the home button
     * is pressed.
     * @author Radhe Patel
     */
    private void updateBottomNavForAdmin() {
        navController.navigate(R.id.navigation_home);

        // Clear existing menu and inflate the admin-specific menu
        navView.getMenu().clear();
        navView.inflateMenu(R.menu.bottom_nav_menu_admin);

        // Set up item selection listener for admin menu
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.admin_navigation_home) {
                // Explicitly navigate back to MainActivity
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish(); // Finish the current activity to avoid stacking
                return true;
            } else if (itemId == R.id.admin_navigation_view_events) {
                navController.navigate(R.id.navigation_admin_view_events);
                return true;
            } else if (itemId == R.id.admin_navigation_view_facilities) {
                navController.navigate(R.id.navigation_admin_view_facilities);
                return true;
            } else if (itemId == R.id.navigation_admin_profiles) {
                navController.navigate(R.id.navigation_admin_profiles);
                return true;
            }
            return false;
        });
    }

}

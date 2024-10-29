package com.example.orange;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.model.UserType;
import com.example.orange.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.orange.databinding.ActivityMainBinding;
import com.google.firebase.FirebaseApp;

/**
 * The main activity
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(this);
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        binding.navView.inflateMenu(R.menu.bottom_nav_menu);

        if (sessionManager.isLoggedIn()) {
            UserType userType = sessionManager.getUserSession().getUserType();
            updateMenuVisibility(userType);
        } else {
            setupInitialNavigation();
        }
    }

    /**
     * Creates the option menu at the top of the page.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (sessionManager.isLoggedIn()) {
            getMenuInflater().inflate(R.menu.toolbar_menu, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Sets the profile button in the top right corner of the app
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.navigation_profile) {
            navController.navigate(R.id.navigation_profile);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up the bottom navigation.
     */
    private void setupInitialNavigation() {
        Log.d(TAG, "Setting up initial navigation");
        // Show only initial menu items
        updateMenuForInitialState();
        setInitialClickListener();
        navController.navigate(R.id.navigation_home);
    }

    /**
     * Sets all on click listeners.
     */
    private void setInitialClickListener() {
        binding.navView.setOnItemSelectedListener(item -> {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            Log.d(TAG, "Initial menu item clicked: " + item.getTitle());

            int itemId = item.getItemId();
            if (itemId == R.id.navigation_join_event) {
                handleUserSignIn(deviceId, UserType.ENTRANT);
                return true;
            } else if (itemId == R.id.navigation_create_event) {
                handleUserSignIn(deviceId, UserType.ORGANIZER);
                return true;
            } else if (itemId == R.id.navigation_home) {
                handleLogout();
                return true;
            }
            return false;
        });
    }

    /**
     * Updates menu to only show proper selection based on userSession
     */
    private void updateMenuForInitialState() {
        Log.d(TAG, "Updating menu for initial state");
        Menu menu = binding.navView.getMenu();
        if (menu != null) {
            // Hide all items first
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }

            // Show only initial items
            MenuItem joinItem = menu.findItem(R.id.navigation_join_event);
            MenuItem homeItem = menu.findItem(R.id.navigation_home);
            MenuItem createItem = menu.findItem(R.id.navigation_create_event);

            if (joinItem != null) joinItem.setVisible(true);
            if (homeItem != null) homeItem.setVisible(true);
            if (createItem != null) createItem.setVisible(true);
        }
    }

    /**
     * After selection it updates menu visibility based on user type
     * @param userType
     */
    private void updateMenuVisibility(UserType userType) {
        Log.d(TAG, "Updating menu visibility for user type: " + userType);
        Menu menu = binding.navView.getMenu();
        if (menu != null) {
            // First hide all items
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }

            // Show home for all users
            MenuItem homeItem = menu.findItem(R.id.navigation_home);
            if (homeItem != null) homeItem.setVisible(true);

            if (userType == UserType.ENTRANT) {
                MenuItem myEventsItem = menu.findItem(R.id.navigation_my_events);
                MenuItem joinItem = menu.findItem(R.id.navigation_join_event);

                if (myEventsItem != null) myEventsItem.setVisible(true);
                if (joinItem != null) joinItem.setVisible(true);
            } else if (userType == UserType.ORGANIZER) {
                MenuItem viewEventsItem = menu.findItem(R.id.navigation_view_my_events);
                MenuItem createItem = menu.findItem(R.id.navigation_create_event);

                if (viewEventsItem != null) viewEventsItem.setVisible(true);
                if (createItem != null) createItem.setVisible(true);
            }

            // Set the appropriate click listener
            setLoggedInClickListener(userType);
        }
    }

    /**
     * Listener for user login
     * @param userType
     */
    private void setLoggedInClickListener(UserType userType) {
        binding.navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Logged in menu item clicked: " + item.getTitle());

            if (itemId == R.id.navigation_home) {
                handleLogout();
            } else {
                navController.navigate(itemId);
            }
            return true;
        });
    }

    /**
     * Function to handle login/signup and create the user session
     *
     * @param deviceId
     * @param userType
     */
    private void handleUserSignIn(String deviceId, UserType userType) {
        Log.d(TAG, "Handling sign in for device: " + deviceId + " as " + userType);
        firebaseService.signInUser(deviceId, userType, new FirebaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                sessionManager.createLoginSession(deviceId, userType, deviceId);
                runOnUiThread(() -> {
                    updateMenuVisibility(userType);
                    invalidateOptionsMenu(); // This will show the profile icon
                    if (userType == UserType.ENTRANT) {
                        navController.navigate(R.id.navigation_join_event);
                    } else {
                        navController.navigate(R.id.navigation_create_event);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Sign in failed", e);
            }
        });
    }

    /**
     * Logs user out of session.
     */
    private void handleLogout() {
        Log.d(TAG, "Handling logout");
        firebaseService.logOut();
        sessionManager.logoutUser();
        invalidateOptionsMenu(); // This will hide the profile icon
        setupInitialNavigation();
    }
}
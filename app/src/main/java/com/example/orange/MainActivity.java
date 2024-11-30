package com.example.orange;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.model.User;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.example.orange.ui.notifications.AccessToken;
import com.example.orange.ui.notifications.EntrantNotifications;
import com.example.orange.utils.SessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.orange.databinding.ActivityMainBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;


/**
 * The main activity
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_NOTIFICATION  = 99 ;
    private ActivityMainBinding binding;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Allow Notifications
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
        }
        EntrantNotifications.createChannel(this);
        EntrantNotifications.sendNotification(this, "title", "Message");

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == RC_NOTIFICATION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "ALLOWED", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "DENIED", Toast.LENGTH_SHORT).show();
            }
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
        } else {
            getMenuInflater().inflate(R.menu.toolbar_admin_button, menu);
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
        } else if (item.getItemId() == R.id.navigation_admin) {
            // Navigate to admin screen
            navController.navigate(R.id.navigation_admin);

            // Clear current menu and inflate the admin menu
            binding.toolbar.getMenu().clear();
            getMenuInflater().inflate(R.menu.toolbar_admin, binding.toolbar.getMenu());
            return true;
        }
        else if(item.getItemId() == R.id.navigation_camera){
            navController.navigate(R.id.navigation_camera);
            return true;
        }else if (item.getItemId() == R.id.navigation_admin_profiles){
        navController.navigate(R.id.navigation_admin_profiles);

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
            // First clear all listeners
            binding.navView.setOnItemSelectedListener(null);

            // Hide all items first
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }

            // Show initial items and ensure they're enabled
            MenuItem joinItem = menu.findItem(R.id.navigation_join_event);
            MenuItem homeItem = menu.findItem(R.id.navigation_home);
            MenuItem createItem = menu.findItem(R.id.navigation_create_event);

            if (joinItem != null) {
                joinItem.setVisible(true);
                joinItem.setEnabled(true);
            }
            if (homeItem != null) {
                homeItem.setVisible(true);
                homeItem.setEnabled(true);
            }
            if (createItem != null) {
                createItem.setVisible(true);
                createItem.setEnabled(true);
            }

            // Ensure the view is refreshed
            binding.navView.invalidate();

            // Set the click listener after updating visibility
            setInitialClickListener();
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
    public void handleLogout() {
        Log.d(TAG, "Handling logout");

        // First clear the session data
        firebaseService.logOut();
        sessionManager.logoutUser();

        // Then update the UI
        invalidateOptionsMenu(); // This will hide the profile icon

        // Force menu refresh
        binding.navView.getMenu().clear();
        binding.navView.inflateMenu(R.menu.bottom_nav_menu);

        // Finally set up navigation and navigate home
        setupInitialNavigation();

        // Ensure the view is refreshed
        binding.navView.invalidate();
    }
}
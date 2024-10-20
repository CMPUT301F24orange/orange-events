package com.example.orange;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.orange.databinding.ActivityMainBinding;
import com.example.orange.utils.SessionManager;
import com.example.orange.data.model.UserSession;
import com.example.orange.data.model.UserType;
import com.google.android.material.navigation.NavigationView;

/**
 * MainActivity is the main entry point of the application.
 * It sets up the navigation drawer, handles user sessions, and manages the main UI components.
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private SessionManager sessionManager;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    int test = 1;
    /**
     * Initializes the activity, sets up the UI components, and configures navigation.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        drawer = binding.drawerLayout;
        navigationView = binding.navView;

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Set up the AppBarConfiguration
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_login, R.id.nav_events, R.id.nav_profile, R.id.nav_admin)
                .setOpenableLayout(drawer)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        navController = navHostFragment.getNavController();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Set up the navigation menu based on user type
        setupNavigationMenu();

        // Hide the drawer and toolbar on the login screen
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_login) {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
            } else {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                }
                // Refresh the navigation menu when changing destinations
                setupNavigationMenu();
            }
        });
    }

    /**
     * Sets up the navigation menu based on the user's session and type.
     * This method is called to refresh the menu when the user's session changes.
     */
    public void setupNavigationMenu() {
        Menu nav_Menu = navigationView.getMenu();
        UserSession userSession = sessionManager.getUserSession();

        // Hide all menu items first
        for (int i = 0; i < nav_Menu.size(); i++) {
            nav_Menu.getItem(i).setVisible(false);
        }

        if (userSession != null) {
            UserType userType = userSession.getUserType();

            // Show common menu items
            nav_Menu.findItem(R.id.nav_home).setVisible(true);
            nav_Menu.findItem(R.id.nav_profile).setVisible(true);
            nav_Menu.findItem(R.id.nav_events).setVisible(true);

            // Show specific menu items based on user type
            switch (userType) {
                case ORGANIZER:
                    // nav_Menu.findItem(R.id.nav_create_event).setVisible(true);
                    break;
                case ADMIN:
                    // nav_Menu.findItem(R.id.nav_create_event).setVisible(true);
                    nav_Menu.findItem(R.id.nav_admin).setVisible(true);
                    break;
            }
        } else {
            // If no user is logged in, only show login
            nav_Menu.findItem(R.id.nav_login).setVisible(true);
        }
    }

    /**
     * Initializes the contents of the Activity's standard options menu.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * This method is called whenever the user chooses to navigate Up within your application's activity hierarchy from the action bar.
     *
     * @return true if Up navigation completed successfully and this Activity was finished, false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


}
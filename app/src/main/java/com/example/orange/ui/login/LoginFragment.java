package com.example.orange.ui.login;

    import android.os.Bundle;
    import android.provider.Settings;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.fragment.app.Fragment;
    import androidx.navigation.Navigation;

    import com.example.orange.R;
    import com.example.orange.data.firebase.FirebaseCallback;
    import com.example.orange.data.model.User;
    import com.example.orange.data.model.UserType;
    import com.example.orange.data.firebase.FirebaseService;
    import com.example.orange.utils.SessionManager;
    import com.example.orange.MainActivity;

/**
 * LoginFragment handles user authentication and registration.
 * It provides options for users to log in as an entrant, organizer, or admin.
 * If a user doesn't exist, it creates a new user account.
 */
public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    private EditText usernameEditText;
    private Button entrantButton, organizerButton, adminButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private MainActivity mainActivity;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_login, container, false);

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
        mainActivity = (MainActivity) requireActivity();

        entrantButton = root.findViewById(R.id.entrantButton);
        organizerButton = root.findViewById(R.id.organizerButton);
        adminButton = root.findViewById(R.id.adminButton);

        entrantButton.setOnClickListener(v -> loginUser(UserType.ENTRANT));
        organizerButton.setOnClickListener(v -> loginUser(UserType.ORGANIZER));
        adminButton.setOnClickListener(v -> loginUser(UserType.ADMIN));

        return root;
    }

    private String getDeviceId() {
        return Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }


    /**
     * Initiates the login process for a user with the specified user type.
     * If the user doesn't exist, it creates a new user account.
     *
     * @param userType The type of user attempting to log in (ENTRANT, ORGANIZER, or ADMIN).
     */
    private void loginUser(UserType userType) {
        String deviceId = getDeviceId();


        firebaseService.getUserByDeviceIdAndType(deviceId, userType, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (user == null) {
                        createNewUser(deviceId, userType);
                    } else {
                        finishLogin(user);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Login failed", e);
                    Toast.makeText(requireContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Creates a new user account with the given username and user type.
     * If the user selects entrant then the username is auto filled as the users unique deviceId
     *
     * @param deviceId The deviceId for the new user.
     * @param userType The type of the new user (ENTRANT, ORGANIZER, or ADMIN).
     */
    private void createNewUser(String deviceId, UserType userType) {
        User newUser = new User(deviceId, userType);
        firebaseService.createUser(newUser, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    newUser.setId(userId);
                    finishLogin(newUser);
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Failed to create user", e);
                    Toast.makeText(requireContext(), "Failed to create user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Completes the login process by creating a login session,
     * updating the UI, and navigating to the home screen.
     *
     * @param user The User object representing the logged-in user.
     */
    private void finishLogin(User user) {
        sessionManager.createLoginSession(user.getDeviceId(), user.getUserType(), user.getId());
        mainActivity.setupNavigationMenu();
        navigateToHome();
    }

    /**
     * Navigates to the home screen after successful login.
     */
    private void navigateToHome() {
        Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_homeFragment);
    }
}
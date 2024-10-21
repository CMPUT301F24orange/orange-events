package com.example.orange.ui.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.utils.SessionManager;
import com.google.firebase.Timestamp;

import java.util.Date;

public class CreateEventFragment extends Fragment {
    private EditText titleEditText, descriptionEditText, capacityEditText;
    private Button createEventButton;
    private FirebaseService firebaseService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        capacityEditText = view.findViewById(R.id.capacityEditText);
        createEventButton = view.findViewById(R.id.createEventButton);

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        createEventButton.setOnClickListener(v -> createEvent());

        return view;
    }

    private void createEvent() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String capacityStr = capacityEditText.getText().toString().trim();

        // Check if title or description is empty
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)) {
            Toast.makeText(requireContext(), "Title and description are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle empty capacity
        Integer capacity = null;
        if (!TextUtils.isEmpty(capacityStr)) {
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid capacity value", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create event object with a nullable capacity
        String organizerId = sessionManager.getUserSession().getUserId();
        Event event = new Event(title, description, new Timestamp(new Date()), capacity, organizerId);
        Log.d("CreateEventFragment", "Attempting to create event: " + event);

        // Save event to Firestore
        firebaseService.createEvent(event, new FirebaseCallback<String>() {
            @Override
            public void onSuccess(String eventId) {
                Log.d("CreateEventFragment", "Event created successfully with ID: " + eventId);
                Toast.makeText(requireContext(), "Event created successfully", Toast.LENGTH_SHORT).show();

                // Navigate back to home, clearing the back stack
                Navigation.findNavController(requireView()).navigate(R.id.action_createEventFragment_to_homeFragment);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("CreateEventFragment", "Failed to create event", e);
                Toast.makeText(requireContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

package com.example.orange.ui.organizer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.utils.SessionManager;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * ViewMyEventsFragment displays all events created by the current organizer.
 * Organizers can view each event and check its waitlist.
 */
public class ViewMyEventsFragment extends Fragment {
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private LinearLayout organizerEventsContainer;

    /**
     * Initializes the fragment's view and loads the events created by the organizer.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_my_organizer_events, container, false);

        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
        organizerEventsContainer = view.findViewById(R.id.organizer_events_container);

        loadOrganizerEvents();

        return view;
    }

    /**
     * Loads events created by the current organizer from Firebase and displays them in the container.
     */
    private void loadOrganizerEvents() {
        String organizerId = sessionManager.getUserSession().getUserId();

        firebaseService.getOrganizerEvents(organizerId, new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                displayEvents(events);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load your events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Dynamically displays each event in the organizer's events list.
     *
     * @param events List of Event objects created by the organizer.
     */
    private void displayEvents(List<Event> events) {
        organizerEventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Event event : events) {
            View eventView = inflater.inflate(R.layout.item_view_organizer_event, organizerEventsContainer, false);

            TextView eventTitle = eventView.findViewById(R.id.organizer_event_title);
            TextView eventDate = eventView.findViewById(R.id.organizer_event_date);
            Button viewWaitlistButton = eventView.findViewById(R.id.view_waitlist_button);
            Button GenerateButton = eventView.findViewById(R.id.generate_QR_button);

            eventTitle.setText(event.getTitle());
            eventDate.setText("Date: " + (event.getEventDate() != null ? event.getEventDate().toDate().toString() : "N/A"));

            viewWaitlistButton.setOnClickListener(v -> showWaitlist(event));
            GenerateButton.setOnClickListener(v-> GenerateQR(event));
            organizerEventsContainer.addView(eventView);
        }
    }

    /**
     * Displays the waitlist for a specified event in an AlertDialog.
     *
     * @param event Event object whose waitlist should be displayed.
     */
    private void showWaitlist(Event event) {
        List<String> waitlist = event.getWaitingList();
        if (waitlist.isEmpty()) {
            Toast.makeText(requireContext(), "No users on the waitlist", Toast.LENGTH_SHORT).show();
        } else {
            StringBuilder waitlistStr = new StringBuilder("Waitlist:\n");
            for (String userId : waitlist) {
                waitlistStr.append(userId).append("\n");
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Event Waitlist")
                    .setMessage(waitlistStr.toString())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
    public void GenerateQR(Event event) {
        try {
            // Prepare event details including eventId for QR content
            String qrContent = "Event ID: " + event.getId() + "\n"
                    + "Event Name: " + event.getTitle() + "\n"
                    + "Date: " + (event.getEventDate() != null ? event.getEventDate().toDate().toString() : "N/A") + "\n"
                    + "Description: " + event.getDescription();

            // Generate the QR code bitmap
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400);

            // Pass both qr_bitmap and eventId to DisplayQRFragment
            Bundle args = new Bundle();
            args.putParcelable("qr_bitmap", bitmap);
            args.putString("event_id", event.getId());

            // Navigate to the DisplayQRFragment with the bundle
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_displayqr, args);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

}

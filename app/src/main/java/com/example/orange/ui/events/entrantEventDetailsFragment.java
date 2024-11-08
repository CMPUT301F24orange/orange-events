package com.example.orange.ui.events;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.utils.SessionManager;
import com.google.firebase.firestore.Blob;

public class entrantEventDetailsFragment extends Fragment {
    public Button joinEventButton;
    public Button leaveEventButton;
    private String eventId;
    public FirebaseService firebaseService;
    public SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrant_event_details, container, false);
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());
        eventId = getArguments() != null ? getArguments().getString("event_id") : null;
        if (eventId != null) {
            loadEventDetails(eventId, view);
        } else {
            Toast.makeText(requireContext(), "No event ID found", Toast.LENGTH_SHORT).show();
        }
        joinEventButton = view.findViewById(R.id.joinWaitlistButton);
        leaveEventButton = view.findViewById(R.id.leaveWaitlistButton);
        joinEventButton.setOnClickListener(v-> joinEvent(eventId));
        leaveEventButton.setOnClickListener(v-> leaveEvent(eventId));
        return view;
    }

    public void loadEventDetails(String eventId, View view) {
        firebaseService.getEventById(eventId, new FirebaseCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                if (result != null) {
                    ImageView eventImage = view.findViewById(R.id.eventImage);
                    Blob eventImageData = result.getEventImageData();
                    if (eventImageData != null) {
                        byte[] imageData = eventImageData.toBytes();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                        eventImage.setImageBitmap(bitmap);
                    } else {
                        eventImage.setImageResource(R.drawable.ic_image); // Placeholder if no image is available
                    }
                    ((TextView) view.findViewById(R.id.eventName)).setText(result.getTitle());
                    ((TextView) view.findViewById(R.id.eventDescriptionText)).setText(result.getDescription());
                    ((TextView) view.findViewById(R.id.eventDateText)).setText(result.getStartDate() != null ? result.getStartDate().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.registrationOpensText)).setText(result.getRegistrationOpens() != null ? result.getRegistrationOpens().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.registrationDeadlineText)).setText(result.getRegistrationDeadline() != null ? result.getRegistrationDeadline().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.eventLimitText)).setText(String.valueOf(result.getCapacity()));
                    ((TextView) view.findViewById(R.id.waitlistLimitText)).setText(String.valueOf(result.getWaitlistLimit()));
                    ((TextView) view.findViewById(R.id.lotteryDayText)).setText(result.getLotteryDrawDate() != null ? result.getLotteryDrawDate().toDate().toString() : "N/A");
                    ((TextView) view.findViewById(R.id.eventPriceText)).setText(result.getPrice() != null ? result.getPrice().toString() : "N/A");
                } else {
                    Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("FirebaseError", "Error fetching event details", e);
                Toast.makeText(requireContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void joinEvent(String eventId) {
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.addToEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Added to waitlist", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to add to waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void leaveEvent(String eventId){
        String userId = sessionManager.getUserSession().getUserId();
        firebaseService.removeFromEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Removed from waitlist", Toast.LENGTH_SHORT).show();// Reload events to reflect changes
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to remove from waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

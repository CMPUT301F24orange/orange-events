package com.example.orange.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import java.util.List;

public class ViewEventWaitlistFragment extends Fragment {
    private FirebaseService firebaseService;
    private String eventId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_event_waitlist, container, false);
        firebaseService = new FirebaseService();
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        loadWaitlist(view);
        return view;
    }

    private void loadWaitlist(View view) {
        if (eventId != null) {
            firebaseService.getEventWaitlist(eventId, new FirebaseCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> waitlist) {
                    TextView waitlistTextView = view.findViewById(R.id.waitlist_text);
                    Button drawParticipantsButton = view.findViewById(R.id.draw_participants_button);

                    if (waitlist.isEmpty()) {
                        waitlistTextView.setText("No users on the waitlist");
                    } else {
                        StringBuilder waitlistStr = new StringBuilder("Entrants on the waitlist:\n");
                        for (String userId : waitlist) {
                            waitlistStr.append(userId).append("\n");
                        }
                        waitlistTextView.setText(waitlistStr.toString());
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(requireContext(), "Failed to load waitlist", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}

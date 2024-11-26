package com.example.orange.ui.organizer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

        Button drawParticipantsButton = view.findViewById(R.id.draw_participants_button);
        drawParticipantsButton.setOnClickListener(v -> {
            if (eventId != null) {
                // Show dialog to input number of attendees
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Specify Number of Attendees");

                final EditText input = new EditText(requireContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String inputText = input.getText().toString();
                    int numToSelect = inputText.isEmpty() ? -1 : Integer.parseInt(inputText);

                    firebaseService.drawFromWaitlist(eventId, numToSelect > 0 ? numToSelect : null, new FirebaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), "Participants drawn successfully", Toast.LENGTH_SHORT).show();
                            loadWaitlist(view); // Refresh waitlist
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "Failed to draw participants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        });

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
                        drawParticipantsButton.setVisibility(View.GONE); // Hide button if no waitlist
                    } else {
                        StringBuilder waitlistStr = new StringBuilder("Entrants on the waitlist:\n");
                        for (String userId : waitlist) {
                            waitlistStr.append(userId).append("\n");
                        }
                        waitlistTextView.setText(waitlistStr.toString());
                        drawParticipantsButton.setVisibility(View.VISIBLE); // Show button if waitlist exists
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

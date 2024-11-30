package com.example.orange.ui.events;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
// Import AlertDialog for optional confirmation dialogs
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.ImageData;
import com.example.orange.data.model.Notification;
import com.example.orange.data.model.NotificationType;
import com.example.orange.data.model.User;
import com.example.orange.ui.notifications.EntrantNotifications;
import com.example.orange.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MyEventsFragment is responsible for displaying a list of events
 * the current user has joined. It fetches the user's events from Firebase
 * and displays relevant information for each event, including the event status.
 * Users can also leave the waitlist or event as appropriate.
 */
public class MyEventsFragment extends Fragment {
    private FirebaseService firebaseService;
    private SessionManager sessionManager;
    private EntrantNotifications entrantNotifications;
    private LinearLayout eventsContainer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Called to initialize the fragment's view.
     *
     * @param inflater           LayoutInflater to inflate the fragment's layout.
     * @param container          Parent view that the fragment's UI should be attached to.
     * @param savedInstanceState Previous state data if fragment is being re-created.
     * @return The created view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        // Initialize Firebase service and session manager
        firebaseService = new FirebaseService();
        sessionManager = new SessionManager(requireContext());

        // Initialize notifications
        entrantNotifications = new EntrantNotifications();

        // Set up the container for displaying events
        eventsContainer = view.findViewById(R.id.events_container);

        // Load and display the user's events
        loadUserEvents();

        return view;
    }

    /**
     * Loads the current user's events from Firebase and calls displayEvents to render them.
     */
    private void loadUserEvents() {
        String userID = sessionManager.getUserSession().getUserId();
        String userType = sessionManager.getUserSession().getUserType().toString();
        String userId = userID + "_" + userType;

        Log.d("View FRAG", userId);
        firebaseService.getUserEvents(userId, new FirebaseCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                displayEvents(events, userId);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load your events", Toast.LENGTH_SHORT).show();
                Log.e("MyEventsFragment", "Error loading user events", e);
            }
        });
    }

    /**
     * Displays a list of events the user has joined, rendering relevant
     * information about each event's status and allowing the user to leave
     * the event or queue if applicable.
     *
     * @param events List of Event objects representing the user's events.
     * @param userId The unique ID of the current user.
     */
    private void displayEvents(List<Event> events, String userId) {
        eventsContainer.removeAllViews();

        for (Event event : events) {
            View eventView = getLayoutInflater().inflate(R.layout.item_view_my_events, eventsContainer, false);

            ImageView eventImage = eventView.findViewById(R.id.event_image);
            TextView eventTitle = eventView.findViewById(R.id.event_title);
            TextView eventDate = eventView.findViewById(R.id.event_date);
            TextView lotteryStatus = eventView.findViewById(R.id.lottery_status);
            Button actionButton = eventView.findViewById(R.id.action_button);

            // New Buttons
            LinearLayout actionButtonsLayout = eventView.findViewById(R.id.action_buttons_layout);
            Button acceptButton = eventView.findViewById(R.id.accept_button);
            Button declineButton = eventView.findViewById(R.id.decline_button);

            eventTitle.setText(event.getTitle());

            // Load and display the event image if available
            String eventImageId = event.getEventImageId();
            if (eventImageId != null) {
                firebaseService.getImageById(eventImageId, new FirebaseCallback<ImageData>() {
                    @Override
                    public void onSuccess(ImageData imageData) {
                        if (imageData != null && imageData.getImageData() != null) {
                            byte[] imageBytes = imageData.getImageData().toBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            eventImage.setImageBitmap(bitmap);
                        } else {
                            eventImage.setImageResource(R.drawable.ic_image); // Placeholder if image data is null
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        eventImage.setImageResource(R.drawable.ic_image); // Placeholder if failed to load image
                        Log.e("MyEventsFragment", "Error loading event image", e);
                    }
                });
            } else {
                eventImage.setImageResource(R.drawable.ic_image); // Placeholder if no image is available
            }

            // Determine user's status in the event
            boolean isSelectedParticipant = event.getSelectedParticipants() != null && event.getSelectedParticipants().contains(userId);
            boolean isParticipant = event.getParticipants() != null && event.getParticipants().contains(userId);
            boolean isWaitlisted = event.getWaitingList() != null && event.getWaitingList().contains(userId);

            // Set event date
            String eventDateText = event.getStartDate() != null ? dateFormat.format(event.getStartDate().toDate()) : "No date available";
            eventDate.setText("Event Date: " + eventDateText);

            // Set lottery status based on user's status
            if (isSelectedParticipant) {
                lotteryStatus.setText("Selected for Event");
                actionButtonsLayout.setVisibility(View.VISIBLE); // Show Accept/Decline buttons
                actionButton.setVisibility(View.GONE); // Hide the generic action button

                // Handle Accept Button Click
                acceptButton.setOnClickListener(v -> {
                    acceptEventInvitation(event.getId(), userId);
                });

                // Handle Decline Button Click
                declineButton.setOnClickListener(v -> {
                    declineEventInvitation(event.getId(), userId);
                });
            } else if (isParticipant) {
                lotteryStatus.setText("Participant");
                actionButtonsLayout.setVisibility(View.GONE); // Hide Accept/Decline buttons
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText("Leave Event");
                actionButton.setOnClickListener(v -> leaveEvent(event.getId(), userId));
            } else if (isWaitlisted) {
                lotteryStatus.setText("In Waitlist");
                actionButtonsLayout.setVisibility(View.GONE); // Hide Accept/Decline buttons
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText("Leave Queue");
                actionButton.setOnClickListener(v -> leaveQueue(event.getId(), userId));
            } else {
                // Handle other cases if necessary
                lotteryStatus.setText("Status Unknown");
                actionButtonsLayout.setVisibility(View.GONE);
                actionButton.setVisibility(View.GONE);
            }

            // Add the event view to the container
            eventsContainer.addView(eventView);
        }
    }

    /**
     * Removes the current user from an event's waitlist.
     *
     * @param eventId Unique ID of the event.
     * @param userId  Unique ID of the user.
     */
    private void leaveQueue(String eventId, String userId) {
        firebaseService.removeFromEventWaitlist(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "You have left the queue.", Toast.LENGTH_SHORT).show();
                loadUserEvents();  // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to leave queue.", Toast.LENGTH_SHORT).show();
                Log.e("MyEventsFragment", "Error leaving queue", e);
            }
        });
    }

    /**
     * Removes the current user from the list of participants for an event.
     *
     * @param eventId Unique ID of the event.
     * @param userId  Unique ID of the user.
     */
    private void leaveEvent(String eventId, String userId) {
        firebaseService.removeFromEventParticipants(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "You have left the event.", Toast.LENGTH_SHORT).show();
                loadUserEvents();  // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to leave event.", Toast.LENGTH_SHORT).show();
                Log.e("MyEventsFragment", "Error leaving event", e);
            }
        });
    }

    /**
     * Handles the acceptance of an invitation by a user.
     *
     * @param eventId The ID of the event.
     * @param userId  The ID of the user.
     */
    private void acceptEventInvitation(String eventId, String userId) {
        firebaseService.acceptEventInvitation(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                firebaseService.getNotificationsForUser(userId, new FirebaseCallback<List<Notification>>() {
                    @Override
                    public void onSuccess(List<Notification> notis) {
                        for(Notification notifications : notis){
                            if((Objects.equals(notifications.getEventId(), eventId ) && notifications.getType() == NotificationType.SELECTED_TO_PARTICIPATE)){
                                notifications.accept();
                                firebaseService.updateNotification(notifications, new FirebaseCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        firebaseService.getEventById(eventId, new FirebaseCallback<Event>() {
                                            @Override
                                            public void onSuccess(Event event) {
                                                firebaseService.getUserById(event.getOrganizerId(), new FirebaseCallback<User>() {
                                                    @Override
                                                    public void onSuccess(User organizer) {
                                                        entrantNotifications.sendToPhone(requireContext(), "A user has accepted the offer to join your event", userId + " has accepted the offer!", organizer,notifications);
                                                        Notification notification = new Notification(eventId, organizer.getId(), NotificationType.ORGANIZER);
                                                        firebaseService.createNotification(notification, new FirebaseCallback<String>() {
                                                            @Override
                                                            public void onSuccess(String result) {
                                                                Log.d("ORANGE", "Notification created");
                                                            }

                                                            @Override
                                                            public void onFailure(Exception e) {
                                                                Log.d("ORANGE", "Failed to create notification");
                                                            }
                                                        });
                                                    }
                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        Log.d("ORANGE", "Failed to get user");
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.d("ORANGE", "Failed to get event");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.d("ORANGE", "Failed to update notification");
                                    }
                                });

                            }
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.d("Notifications", "Failed to grab notifications");
                    }
                });
                Toast.makeText(requireContext(), "You have accepted the invitation.", Toast.LENGTH_SHORT).show();
                loadUserEvents(); // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to accept the invitation.", Toast.LENGTH_SHORT).show();
                Log.e("MyEventsFragment", "Accept Invitation Error", e);
            }
        });
    }

    /**
     * Handles the decline of an invitation by a user.
     *
     * @param eventId The ID of the event.
     * @param userId  The ID of the user.
     */
    private void declineEventInvitation(String eventId, String userId) {
        firebaseService.declineEventInvitation(eventId, userId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                firebaseService.getNotificationsForUser(userId, new FirebaseCallback<List<Notification>>() {
                    @Override
                    public void onSuccess(List<Notification> notis) {
                        for(Notification notifications : notis){
                            if(Objects.equals(notifications.getEventId(), eventId)){
                                notifications.decline();
                                firebaseService.updateNotification(notifications, new FirebaseCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        firebaseService.getEventById(eventId, new FirebaseCallback<Event>() {
                                            @Override
                                            public void onSuccess(Event event) {
                                                firebaseService.getUserById(event.getOrganizerId(), new FirebaseCallback<User>() {
                                                    @Override
                                                    public void onSuccess(User organizer) {
                                                        entrantNotifications.sendToPhone(requireContext(), "A user has declined the offer to join your event", userId + " has declined the offer.", organizer,notifications);
                                                        Notification notification1 = new Notification();
                                                        notification1.setEventId(eventId);
                                                        event.fillSpotsFromWaitingList(requireContext(), notification1);
                                                        Notification notification = new Notification(eventId, organizer.getId(), NotificationType.ORGANIZER);
                                                        firebaseService.createNotification(notification, new FirebaseCallback<String>() {
                                                            @Override
                                                            public void onSuccess(String result) {
                                                                Log.d("ORANGE", "Notification created");
                                                            }

                                                            @Override
                                                            public void onFailure(Exception e) {
                                                                Log.d("ORANGE", "Failed to create notification");
                                                            }
                                                        });
                                                    }
                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        Log.d("ORANGE", "Failed to get user");
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.d("ORANGE", "Failed to get event");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.d("ORANGE", "Failed to update notification");
                                    }
                                });
                            }
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.d("Notifications", "Failed to grab notifications");
                    }
                });
                Toast.makeText(requireContext(), "You have declined the invitation.", Toast.LENGTH_SHORT).show();
                loadUserEvents(); // Refresh the events list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to decline the invitation.", Toast.LENGTH_SHORT).show();
                Log.e("MyEventsFragment", "Decline Invitation Error", e);
            }
        });
    }
}

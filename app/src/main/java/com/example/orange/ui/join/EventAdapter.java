package com.example.orange.ui.join;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> events;
    private final Context context;
    private final JoinEventFragment joinEventFragment;
    private final FirebaseService firebaseService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public EventAdapter(List<Event> events, Context context, JoinEventFragment fragment) {
        this.events = events;
        this.context = context;
        this.joinEventFragment = fragment;
        this.firebaseService = new FirebaseService();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_join_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView eventImage;
        TextView eventTitle;
        TextView eventDate;
        TextView lotteryStatus;
        ImageButton joinWaitlistButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_image);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
            lotteryStatus = itemView.findViewById(R.id.lottery_status);
            joinWaitlistButton = itemView.findViewById(R.id.join_waitlist_button);
        }

        void bind(Event event) {
            eventTitle.setText(event.getTitle());

            // Set the event date
            if (event.getRegistrationDeadline() != null) {
                Date deadline = event.getRegistrationDeadline().toDate();
                eventDate.setText("Waitlist closes: " + dateFormat.format(deadline));
            } else {
                eventDate.setText("No registration deadline");
            }

            lotteryStatus.setText("Available to Join");

            // Load the event image
            String eventImageId = event.getEventImageId();
            if (eventImageId != null && !eventImageId.isEmpty()) {
                firebaseService.getImageById(eventImageId, new FirebaseCallback<com.example.orange.data.model.ImageData>() {
                    @Override
                    public void onSuccess(com.example.orange.data.model.ImageData imageData) {
                        if (imageData != null && imageData.getImageData() != null) {
                            byte[] imageBytes = imageData.getImageData().toBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            eventImage.setImageBitmap(bitmap);
                        } else {
                            eventImage.setImageResource(R.drawable.ic_image); // Placeholder image
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        eventImage.setImageResource(R.drawable.ic_image); // Placeholder on failure
                    }
                });
            } else {
                eventImage.setImageResource(R.drawable.ic_image); // Placeholder if no image ID
            }

            // Set up the "Join Waitlist" button
            joinWaitlistButton.setOnClickListener(v -> {
                joinEventFragment.joinEvent(event);
            });
        }
    }
}

package com.example.orange.ui.join;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.orange.data.model.Event;
import com.example.orange.databinding.ItemEventBinding;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> eventList;
    private Context context;
    private JoinEventFragment fragment;
    private String userId; // Store the user's ID

    public EventAdapter(List<Event> eventList, Context context, JoinEventFragment fragment) {
        this.eventList = eventList;
        this.context = context;
        this.fragment = fragment;
        this.userId = fragment.getSessionManager().getUserSession().getUserId(); // Get user ID
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventBinding binding = ItemEventBinding.inflate(LayoutInflater.from(context), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.binding.eventTitle.setText(event.getTitle());
        holder.binding.eventDescription.setText(event.getDescription());

        boolean isOnWaitlist = event.getWaitingList().contains(userId);

        if (isOnWaitlist) {
            holder.binding.joinButton.setText("Leave Waitlist");
            holder.binding.eventWaitlistStatus.setText("You are on the waitlist");
            holder.binding.eventWaitlistStatus.setVisibility(View.VISIBLE);
        } else {
            holder.binding.joinButton.setText("Join Waitlist");
            holder.binding.eventWaitlistStatus.setVisibility(View.GONE);
        }

        holder.binding.joinButton.setOnClickListener(v -> {
            if (isOnWaitlist) {
                showLeaveWaitlistDialog(event);
            } else {
                fragment.joinEvent(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    private void showLeaveWaitlistDialog(Event event) {
        new AlertDialog.Builder(context)
                .setTitle("Leave Waitlist")
                .setMessage("Do you want to leave the waitlist for this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    fragment.leaveWaitlist(event);
                })
                .setNegativeButton("No", null)
                .show();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ItemEventBinding binding;

        EventViewHolder(ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

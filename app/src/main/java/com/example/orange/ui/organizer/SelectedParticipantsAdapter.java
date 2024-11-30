package com.example.orange.ui.organizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.User;

import java.util.List;

public class SelectedParticipantsAdapter extends RecyclerView.Adapter<SelectedParticipantsAdapter.ViewHolder> {

    public interface OnParticipantRemoveListener {
        void onRemove(String userId);
    }

    private List<String> participantIds;
    private OnParticipantRemoveListener removeListener;
    private Context context;
    private FirebaseService firebaseService;

    public SelectedParticipantsAdapter(Context context, List<String> participantIds, OnParticipantRemoveListener removeListener) {
        this.context = context;
        this.participantIds = participantIds;
        this.removeListener = removeListener;
        this.firebaseService = new FirebaseService();
    }

    @NonNull
    @Override
    public SelectedParticipantsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedParticipantsAdapter.ViewHolder holder, int position) {
        String userId = participantIds.get(position);
        // Fetch user details to display name instead of ID
        firebaseService.getUserById(userId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    holder.participantNameTextView.setText(user.getId()); // Ensure User class has getName()
                } else {
                    holder.participantNameTextView.setText("Unknown User");
                }
            }

            @Override
            public void onFailure(Exception e) {
                holder.participantNameTextView.setText("Error Loading User");
            }
        });

        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(userId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return participantIds.size();
    }

    public void updateList(List<String> newParticipantIds) {
        this.participantIds = newParticipantIds;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView participantNameTextView;
        ImageButton removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            participantNameTextView = itemView.findViewById(R.id.participant_name_text_view);
            removeButton = itemView.findViewById(R.id.remove_participant_button);
        }
    }
}

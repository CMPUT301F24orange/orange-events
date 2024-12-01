// ParticipantsAdapter.java
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

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ViewHolder> {

    public interface OnParticipantActionListener {
        void onRemove(String userId);
    }

    private List<String> participantIds;
    private OnParticipantActionListener actionListener;
    private Context context;
    private FirebaseService firebaseService;
    private boolean isRemovable; // Flag to show/hide remove button

    /**
     * Constructor for ParticipantsAdapter.
     *
     * @param context          The context.
     * @param participantIds   List of participant user IDs.
     * @param actionListener   Listener for participant actions (e.g., removal).
     * @param isRemovable      Flag indicating if participants can be removed.
     */
    public ParticipantsAdapter(Context context, List<String> participantIds, OnParticipantActionListener actionListener, boolean isRemovable) {
        this.context = context;
        this.participantIds = participantIds;
        this.actionListener = actionListener;
        this.isRemovable = isRemovable;
        this.firebaseService = new FirebaseService();
    }

    @NonNull
    @Override
    public ParticipantsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantsAdapter.ViewHolder holder, int position) {
        String userId = participantIds.get(position);
        // Fetch user details to display name instead of ID
        firebaseService.getUserById(userId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    holder.participantNameTextView.setText(user.getId()); // Use getName() if available
                } else {
                    holder.participantNameTextView.setText("Unknown User");
                }
            }

            @Override
            public void onFailure(Exception e) {
                holder.participantNameTextView.setText("Error Loading User");
            }
        });

        // Control the visibility of the remove button based on the isRemovable flag
        if (isRemovable) {
            holder.removeButton.setVisibility(View.VISIBLE);
            holder.removeButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRemove(userId);
                }
            });
        } else {
            holder.removeButton.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return participantIds.size();
    }

    /**
     * Updates the participant list and refreshes the RecyclerView.
     *
     * @param newParticipantIds The new list of participant IDs.
     */
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

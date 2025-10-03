package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.model.User;
import java.util.List;

public class SentRequestAdapter extends RecyclerView.Adapter<SentRequestAdapter.SentViewHolder> {

    private List<User> sentRequests;
    private OnCancelListener listener;

    public interface OnCancelListener {
        void onCancel(User user);
    }

    public SentRequestAdapter(List<User> sentRequests, OnCancelListener listener) {
        this.sentRequests = sentRequests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sent_request, parent, false);
        return new SentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SentViewHolder holder, int position) {
        User user = sentRequests.get(position);
        holder.tvSentUsername.setText(user.getUsername());
        holder.btnCancel.setOnClickListener(v -> listener.onCancel(user));
    }

    @Override
    public int getItemCount() {
        return sentRequests.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvSentUsername;
        Button btnCancel;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSentUsername = itemView.findViewById(R.id.tvSentUsername);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
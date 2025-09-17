package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.gps.Comment;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import java.util.List;
import android.widget.Button;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private List<Comment> commentList;
    private boolean isAdmin = true; // 관리자 여부 (임시)

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }
    
    public interface OnCommentDeleteListener {
        void onDelete(int position, Comment comment);
    }
    public interface OnCommentReplyListener {
        void onReply(int position, Comment comment);
    }
    private OnCommentDeleteListener deleteListener;
    private OnCommentReplyListener replyListener;
    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }
    public void setOnCommentReplyListener(OnCommentReplyListener listener) {
        this.replyListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvCommentAuthor.setText(comment.getAuthor());
        holder.tvCommentContent.setText(comment.getContent());
        holder.tvCommentTime.setText(comment.getTimeAgo());
        
        // 관리자만 삭제 버튼 표시
        if (isAdmin) {
            holder.btnDeleteComment.setVisibility(View.VISIBLE);
            holder.btnDeleteComment.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(holder.getAdapterPosition(), comment);
                }
            });
        } else {
            holder.btnDeleteComment.setVisibility(View.GONE);
        }
        
        // 답글 버튼 클릭 리스너
        holder.btnReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReply(holder.getAdapterPosition(), comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentAuthor, tvCommentContent, tvCommentTime;
        Button btnDeleteComment, btnReply;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
            btnReply = itemView.findViewById(R.id.btnReply);
        }
    }
} 
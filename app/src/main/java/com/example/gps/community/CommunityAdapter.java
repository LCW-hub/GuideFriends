package com.example.gps.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.model.Board;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private List<Board> postList;

    public CommunityAdapter(List<Board> postList) {
        this.postList = postList;
    }

    // 콜백 인터페이스
    public interface OnPostDeleteListener {
        void onDelete(int position, Board post);
    }
    public interface OnPostLikeListener {
        void onLike(int position, Board post);
    }
    public interface OnPostCommentListener {
        void onComment(int position, Board post);
    }

    private OnPostDeleteListener deleteListener;
    private OnPostLikeListener likeListener;
    private OnPostCommentListener commentListener;

    public void setOnPostDeleteListener(OnPostDeleteListener listener) {
        this.deleteListener = listener;
    }
    public void setOnPostLikeListener(OnPostLikeListener listener) {
        this.likeListener = listener;
    }
    public void setOnPostCommentListener(OnPostCommentListener listener) {
        this.commentListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Board post = postList.get(position);

        holder.tvEmoji.setText(post.getEmoji() != null ? post.getEmoji() : "");
        holder.tvAuthor.setText(post.getAuthor() != null ? post.getAuthor() : "");
        holder.tvTitle.setText(post.getTitle() != null ? post.getTitle() : "");
        holder.tvContent.setText(post.getContent() != null ? post.getContent() : "");
        holder.tvTimeAgo.setText(post.getTimeAgo() != null ? post.getTimeAgo() : "");
        holder.tvLikes.setText(String.valueOf(post.getLikes()));
        holder.tvComments.setText(String.valueOf(post.getComments()));

        // 삭제 버튼 클릭
        holder.btnDeletePost.setVisibility(View.VISIBLE);
        holder.btnDeletePost.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getAdapterPosition(), post);
            }
        });

        // 좋아요 버튼 클릭
        holder.btnLike.setOnClickListener(v -> {
            if (likeListener != null) {
                likeListener.onLike(holder.getAdapterPosition(), post);
            }
        });

        // 좋아요 상태 표시
        holder.btnLike.setText(post.getLikes() > 0 ? "❤️" : "🤍");

        // 댓글 버튼 클릭
        holder.tvComments.setOnClickListener(v -> {
            if (commentListener != null) {
                commentListener.onComment(holder.getAdapterPosition(), post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    // Board 리스트 업데이트
    public void setPosts(List<Board> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvAuthor, tvTitle, tvContent, tvTimeAgo, tvLikes, tvComments;
        Button btnDeletePost, btnLike;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
            btnLike = itemView.findViewById(R.id.btnLike);
        }
    }
}

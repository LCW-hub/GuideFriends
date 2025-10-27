package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.gps.R;
import com.example.gps.model.SearchResult;
import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<SearchResult> items = new ArrayList<>();
    private OnItemClickListener listener;

    // 아이템 클릭을 위한 인터페이스
    public interface OnItemClickListener {
        void onItemClick(SearchResult item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // ViewHolder 클래스 정의
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlaceIcon;
        TextView tvPlaceName;
        TextView tvPlaceAddress;
        TextView tvPlaceCategory;

        public ViewHolder(View itemView, final OnItemClickListener listener, final List<SearchResult> items) {
            super(itemView);
            ivPlaceIcon = itemView.findViewById(R.id.iv_place_icon);
            tvPlaceName = itemView.findViewById(R.id.tv_place_name);
            tvPlaceAddress = itemView.findViewById(R.id.tv_place_address);
            tvPlaceCategory = itemView.findViewById(R.id.tv_place_category);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(items.get(position));
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view, listener, items);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult item = items.get(position);
        holder.tvPlaceName.setText(item.getTitle());
        holder.tvPlaceAddress.setText(item.getAddress());
        holder.tvPlaceCategory.setText(item.getCategory());

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_map) // 로딩 중 기본 이미지
                    .error(R.drawable.ic_map)       // 에러 시 기본 이미지
                    .into(holder.ivPlaceIcon);
        } else {
            holder.ivPlaceIcon.setImageResource(R.drawable.ic_map); // 이미지가 없을 경우
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 검색 결과를 업데이트하는 메서드
    public void updateResults(List<SearchResult> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }
}
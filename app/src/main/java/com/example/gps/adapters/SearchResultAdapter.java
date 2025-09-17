package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gps.R;
import com.example.gps.model.SearchResult;
import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    
    private List<SearchResult> searchResults = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(SearchResult searchResult);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = searchResults.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void updateResults(List<SearchResult> newResults) {
        searchResults.clear();
        searchResults.addAll(newResults);
        notifyDataSetChanged();
    }

    public void clearResults() {
        searchResults.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlaceName;
        private TextView tvPlaceAddress;
        private TextView tvPlaceCategory;
        private ImageView ivPlaceIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tv_place_name);
            tvPlaceAddress = itemView.findViewById(R.id.tv_place_address);
            tvPlaceCategory = itemView.findViewById(R.id.tv_place_category);
            ivPlaceIcon = itemView.findViewById(R.id.iv_place_icon);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(searchResults.get(position));
                    }
                }
            });
        }

        public void bind(SearchResult result) {
            tvPlaceName.setText(result.getTitle());
            tvPlaceAddress.setText(result.getAddress());
            tvPlaceCategory.setText(result.getCategory());
            
            // 카테고리에 따른 아이콘 설정
            int iconResource = getIconForCategory(result.getCategory());
            ivPlaceIcon.setImageResource(iconResource);
        }

        private int getIconForCategory(String category) {
            if (category == null) return R.drawable.ic_my_location;
            
            String lowerCategory = category.toLowerCase();
            if (lowerCategory.contains("공원") || lowerCategory.contains("산책")) {
                return R.drawable.ic_trail_nature;
            } else if (lowerCategory.contains("카페") || lowerCategory.contains("음식")) {
                return R.drawable.ic_shop;
            } else if (lowerCategory.contains("병원") || lowerCategory.contains("약국")) {
                return R.drawable.ic_info;
            } else if (lowerCategory.contains("주차")) {
                return R.drawable.ic_my_location;
            } else {
                return R.drawable.ic_my_location;
            }
        }
    }
}

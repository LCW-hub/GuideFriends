package com.example.gps.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.model.SidebarMenuItem;

import java.util.List;

public class SidebarMenuAdapter extends RecyclerView.Adapter<SidebarMenuAdapter.MenuViewHolder> {

    private List<SidebarMenuItem> menuItems;

    public SidebarMenuAdapter(List<SidebarMenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sidebar_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        SidebarMenuItem item = menuItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivMenuIcon;
        private TextView tvMenuTitle;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMenuIcon = itemView.findViewById(R.id.iv_menu_icon);
            tvMenuTitle = itemView.findViewById(R.id.tv_menu_title);
        }

        public void bind(SidebarMenuItem item) {
            ivMenuIcon.setImageResource(item.getIconResId());
            tvMenuTitle.setText(item.getTitle());

            itemView.setOnClickListener(v -> {
                if (item.getOnClickListener() != null) {
                    item.getOnClickListener().run();
                }
            });
        }
    }
}


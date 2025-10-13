package com.example.gps.model;

public class SidebarMenuItem {
    private String title;
    private int iconResId;
    private Runnable onClickListener;

    public SidebarMenuItem(String title, int iconResId, Runnable onClickListener) {
        this.title = title;
        this.iconResId = iconResId;
        this.onClickListener = onClickListener;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public Runnable getOnClickListener() {
        return onClickListener;
    }

    public void setOnClickListener(Runnable onClickListener) {
        this.onClickListener = onClickListener;
    }
}


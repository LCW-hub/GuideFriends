package com.example.gps.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.example.gps.R;

public class MarkerDetailFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_TYPE = "type";

    public static MarkerDetailFragment newInstance(String title, String description, String imageUrl, String type) {
        MarkerDetailFragment fragment = new MarkerDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marker_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            String description = getArguments().getString(ARG_DESCRIPTION);
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);
            String type = getArguments().getString(ARG_TYPE);

            TextView titleText = view.findViewById(R.id.titleText);
            TextView descriptionText = view.findViewById(R.id.descriptionText);
            TextView typeText = view.findViewById(R.id.typeText);
            ImageView imageView = view.findViewById(R.id.imageView);

            titleText.setText(title);
            descriptionText.setText(description);
            typeText.setText(type);

            // 이미지 로드
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String assetPath = "file:///android_asset/images/" + imageUrl;
                Glide.with(this)
                    .load(assetPath)
                    .centerCrop()
                    .into(imageView);
            }
        }
    }
} 
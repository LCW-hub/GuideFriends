package com.example.gps.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.gps.R;
import com.example.gps.model.SearchResult;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SearchResultDetailFragment extends BottomSheetDialogFragment {

    private static final String ARG_SEARCH_RESULT = "search_result";

    public static SearchResultDetailFragment newInstance(SearchResult searchResult) {
        SearchResultDetailFragment fragment = new SearchResultDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SEARCH_RESULT, searchResult);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_result_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            SearchResult result = getArguments().getParcelable(ARG_SEARCH_RESULT);
            if (result != null) {
                // 레이아웃의 뷰들을 찾기
                ImageView ivPlaceImage = view.findViewById(R.id.iv_place_image);
                TextView tvTitle = view.findViewById(R.id.tv_place_title);
                TextView tvCategory = view.findViewById(R.id.tv_place_category);
                TextView tvAddress = view.findViewById(R.id.tv_place_address);
                Button btnDirections = view.findViewById(R.id.btn_get_directions);

                // 찾은 뷰에 데이터 채우기
                tvTitle.setText(result.getTitle());
                tvCategory.setText(result.getCategory());
                tvAddress.setText(result.getAddress());

                // --- [수정] 샘플 URL 대신 실제 이미지 URL 사용 ---
                String imageUrl = result.getImageUrl(); // result 객체에서 실제 이미지 URL 가져오기

                // 이미지 URL이 비어있지 않은 경우에만 Glide로 로딩
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl) // 👈 result에서 가져온 실제 URL 사용
                            .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지
                            .error(R.drawable.ic_launcher_foreground)       // 에러 시 이미지
                            .into(ivPlaceImage);
                } else {
                    // 이미지 URL이 없는 경우, 기본 이미지를 보여주거나 숨김 처리
                    ivPlaceImage.setImageResource(R.drawable.ic_launcher_foreground); // 예: 기본 아이콘 표시
                }
                // --- 여기까지 수정 ---

                // 길찾기 버튼 클릭 이벤트
                btnDirections.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "길찾기 기능은 아직 준비 중입니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
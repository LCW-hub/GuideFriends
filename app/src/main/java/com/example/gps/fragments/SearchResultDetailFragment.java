package com.example.gps.fragments;

import android.content.Context;
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

    // ⭐ 1. 인터페이스 정의: MapsActivity로 선택된 장소 데이터를 전달하기 위한 리스너
    public interface OnDestinationSelectedListener {
        void onDestinationSelected(SearchResult selectedResult);
    }
    private OnDestinationSelectedListener destinationSelectedListener;

    // MapsActivity에 프래그먼트가 붙을 때 리스너를 초기화
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // 상위 액티비티가 이 인터페이스를 구현했는지 확인
            destinationSelectedListener = (OnDestinationSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnDestinationSelectedListener");
        }
    }
    // ----------------------------------------------------------------------


    public static SearchResultDetailFragment newInstance(SearchResult searchResult) {
        SearchResultDetailFragment fragment = new SearchResultDetailFragment();
        Bundle args = new Bundle();
        // SearchResult 클래스는 Parcelable 또는 Serializable을 구현해야 Intent/Bundle에 담을 수 있습니다.
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
            // Parcelable로 캐스팅 (SearchResult 모델 클래스가 Parcelable을 구현했다고 가정)
            SearchResult result = getArguments().getParcelable(ARG_SEARCH_RESULT);
            if (result != null) {
                // 레이아웃의 뷰들을 찾기
                ImageView ivPlaceImage = view.findViewById(R.id.iv_place_image);
                TextView tvTitle = view.findViewById(R.id.tv_place_title);
                TextView tvCategory = view.findViewById(R.id.tv_place_category);
                TextView tvAddress = view.findViewById(R.id.tv_place_address);
                Button btnDirections = view.findViewById(R.id.btn_get_directions);
                Button btnSaveDestination = view.findViewById(R.id.btn_get_directions2); // btnDirections2를 저장 버튼으로 사용

                // 찾은 뷰에 데이터 채우기
                tvTitle.setText(result.getTitle());
                tvCategory.setText(result.getCategory());
                tvAddress.setText(result.getAddress());

                // 이미지 로딩 로직 (Glide 사용)
                String imageUrl = result.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(ivPlaceImage);
                } else {
                    ivPlaceImage.setImageResource(R.drawable.ic_launcher_foreground);
                }

                // 2. 길찾기 버튼 클릭 이벤트
                btnDirections.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "길찾기 기능은 아직 준비 중입니다.", Toast.LENGTH_SHORT).show();
                    // 🚨 여기에 길찾기 Intent나 API 호출 로직을 구현합니다.
                });

                // ⭐ 3. 그룹 장소 저장 버튼 클릭 이벤트 (btn_get_directions2)
                btnSaveDestination.setText("모임 장소로 지정"); // 버튼 텍스트를 용도에 맞게 변경 (R.id.btn_get_directions2의 레이아웃이 필요)
                btnSaveDestination.setOnClickListener(v -> {
                    // ⭐️ 리스너를 통해 상위 MapsActivity로 선택된 장소 데이터 전달
                    if (destinationSelectedListener != null) {
                        destinationSelectedListener.onDestinationSelected(result);
                        Toast.makeText(getContext(), result.getTitle() + "을(를) 모임 장소로 선택했습니다.", Toast.LENGTH_SHORT).show();
                        dismiss(); // 바텀 시트 닫기
                    }
                });
            }
        }
    }
}
package com.example.gps.api;

import com.example.gps.dto.FriendResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface FriendApiService {
    @GET("api/friends")
    Call<List<FriendResponse>> getFriends();
}
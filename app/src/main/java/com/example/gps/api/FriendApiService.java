package com.example.gps.api;

import com.example.gps.dto.FriendResponse;
import com.example.gps.model.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface FriendApiService {
    @GET("/api/friends/group-members")
    Call<List<User>> getGroupSelectableMembers();
}
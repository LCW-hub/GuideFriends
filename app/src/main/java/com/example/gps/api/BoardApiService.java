package com.example.gps.api;

import com.example.gps.model.Board;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface BoardApiService {
    @GET("boards")
    Call<List<Board>> getBoards();

    @POST("boards")
    Call<Board> createBoard(@Body Board board);

    @PUT("boards/{id}")
    Call<Board> updateBoard(@Path("id") Long id, @Body Board board);

    @DELETE("boards/{id}")
    Call<Void> deleteBoard(@Path("id") Long id);
}

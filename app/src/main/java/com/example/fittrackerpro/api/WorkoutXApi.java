package com.example.fittrackerpro.api;

import com.example.fittrackerpro.api.response.WxExercise;
import com.example.fittrackerpro.api.response.WxSearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WorkoutXApi {

    // Free plan: one filter at a time, and at most 10 results per request
    @GET("exercises")
    Call<WxSearchResponse> searchByName(@Query("name") String name, @Query("limit") int limit);

    // One exercise, including fresh media links
    @GET("exercises/{id}")
    Call<WxExercise> getExercise(@Path("id") String id);
}
package com.example.fittrackerpro.api;

import com.example.fittrackerpro.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WorkoutXClient {

    private static WorkoutXApi api;

    public static synchronized WorkoutXApi get() {
        if (api == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    // The interceptor adds the key header to every request.
                    // WorkoutX only accepts the key in a header, never in the URL.
                    .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                            .header("X-WorkoutX-Key", BuildConfig.WORKOUTX_API_KEY)
                            .build()))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

            api = new Retrofit.Builder()
                    .baseUrl("https://api.workoutxapp.com/v2/")
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WorkoutXApi.class);
        }
        return api;
    }
}
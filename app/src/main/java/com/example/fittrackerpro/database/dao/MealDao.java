package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.Meal;

import java.util.List;

@Dao
public interface MealDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Meal> meals);

    @Insert
    void insert(Meal meal);

    @Query("SELECT COUNT(*) FROM meals")
    int count();

    @Query("SELECT * FROM meals WHERE name LIKE '%' || :query || '%' ORDER BY name")
    LiveData<List<Meal>> search(String query);
}
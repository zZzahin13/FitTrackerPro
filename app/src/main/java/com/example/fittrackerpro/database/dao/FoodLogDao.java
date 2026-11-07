package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.FoodLog;
import com.example.fittrackerpro.database.entity.FoodLogItem;

import java.util.List;

@Dao
public interface FoodLogDao {

    // REPLACE lets "Undo" or cloud restore put a line back with its id
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodLog log);

    @Delete
    void delete(FoodLog log);

    @Query("UPDATE food_logs SET quantity = :quantity, totalCalories = :totalCalories, "
            + "isSynced = 0, updatedAt = :now WHERE logId = :logId")
    void updateQuantity(String logId, double quantity, double totalCalories, long now);

    // JOIN = each diary line is combined with the food it refers to
    @Query("SELECT l.*, m.name AS foodName, m.servingSize AS servingSize, "
            + "m.calories AS kcalPerServing, m.protein AS proteinPerServing, "
            + "m.carbohydrates AS carbsPerServing, m.fat AS fatPerServing "
            + "FROM food_logs l JOIN meals m ON m.mealId = l.mealId "
            + "WHERE l.userId = :userId AND l.date >= :rangeStart AND l.date < :rangeEnd "
            + "ORDER BY l.date")
    LiveData<List<FoodLogItem>> observeRange(String userId, long rangeStart, long rangeEnd);

    @Query("SELECT * FROM food_logs WHERE userId = :userId AND isSynced = 0")
    List<FoodLog> getUnsynced(String userId);

    @Query("UPDATE food_logs SET isSynced = 1 WHERE logId = :logId")
    void markSynced(String logId);
}

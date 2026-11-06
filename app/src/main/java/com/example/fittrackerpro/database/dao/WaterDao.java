package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.WaterLog;

@Dao
public interface WaterDao {

    @Insert
    void insert(WaterLog log);

    // IFNULL turns "no rows" into 0 instead of null
    @Query("SELECT IFNULL(SUM(amountMl), 0) FROM water_logs "
            + "WHERE userId = :userId AND date >= :rangeStart AND date < :rangeEnd")
    LiveData<Integer> observeTotal(String userId, long rangeStart, long rangeEnd);

    // Removes only the most recent entry of that day (used by the Undo button)
    @Query("DELETE FROM water_logs WHERE waterId = (SELECT waterId FROM water_logs "
            + "WHERE userId = :userId AND date >= :rangeStart AND date < :rangeEnd "
            + "ORDER BY date DESC LIMIT 1)")
    void deleteLatest(String userId, long rangeStart, long rangeEnd);
}
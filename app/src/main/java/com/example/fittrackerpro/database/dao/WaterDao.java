package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.WaterLog;

import java.util.List;

@Dao
public interface WaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Query("SELECT * FROM water_logs WHERE userId = :userId AND isSynced = 0")
    List<WaterLog> getUnsynced(String userId);

    @Query("UPDATE water_logs SET isSynced = 1 WHERE waterId = :waterId")
    void markSynced(String waterId);
}

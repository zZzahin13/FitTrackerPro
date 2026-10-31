package com.example.fittrackerpro.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fittrackerpro.database.entity.UserProfile;

@Dao
public interface UserProfileDao {

    // REPLACE means "insert, or overwrite the row with the same key"
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    LiveData<UserProfile> observeProfile(String userId);

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    UserProfile getProfileOnce(String userId);
}
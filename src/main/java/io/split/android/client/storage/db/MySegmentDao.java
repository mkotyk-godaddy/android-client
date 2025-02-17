package io.split.android.client.storage.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.split.android.client.dtos.MySegment;

@Dao
public interface MySegmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(MySegmentEntity mySegment);

    @Query("SELECT user_key, segment_list, updated_at FROM my_segments WHERE user_key = :userKey")
    MySegmentEntity getByUserKeys(String userKey);
}

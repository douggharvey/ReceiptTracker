package com.douglasharvey.receipttracker.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ReceiptDao {

    @Query("SELECT * FROM receipt_table ORDER BY receipt_date DESC ")
    LiveData<List<Receipt>> getReceiptsLiveData();

    @Query("SELECT * FROM receipt_table WHERE web_link IS null AND drive_id IS NOT NULL")
    LiveData<List<Receipt>> getBlankWebLinks();

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    long insert(Receipt receipt);

    @Query("DELETE FROM receipt_table WHERE id=:recordId")
    void delete(long recordId);

    @Query("UPDATE receipt_table SET web_link = :webLink WHERE id=:recordId")
    void updateWebLink (String webLink, long recordId);

}

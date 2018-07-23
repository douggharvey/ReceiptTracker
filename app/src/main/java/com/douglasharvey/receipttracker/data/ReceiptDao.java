package com.douglasharvey.receipttracker.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ReceiptDao {

    @Query("SELECT * FROM receipt_table ORDER BY receipt_date DESC ")
    LiveData<List<Receipt>> getReceiptsLiveData();

    @Query("SELECT * FROM receipt_table ORDER BY receipt_date ")
    List<Receipt> getReceipts();

    @Query("SELECT * FROM receipt_table WHERE webLink IS null ")
    List<Receipt> getBlankWebLinks();


    @Insert //todo consider switch to (onConflict = OnConflictStrategy.REPLACE)
    long insert(Receipt receipt);

    @Query("DELETE FROM receipt_table")
    void deleteAll();

    @Query("DELETE FROM receipt_table WHERE id=:recordId")
    void delete(long recordId);

    @Query("UPDATE receipt_table SET driveID = :driveId WHERE id=:recordId")
    void updateDriveId (String driveId, long recordId);

    @Query("UPDATE receipt_table SET webLink = :webLink WHERE id=:recordId")
    void updateWebLink (String webLink, long recordId);

}

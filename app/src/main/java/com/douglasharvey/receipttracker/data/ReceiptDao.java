package com.douglasharvey.receipttracker.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ReceiptDao {

    @Query("SELECT * FROM receipt_table ORDER BY ID ASC ")
    LiveData<List<Receipt>> getReceipts();

    @Insert
    void insert(Receipt receipt);

    @Query("DELETE FROM receipt_table")
    void deleteAll();
}

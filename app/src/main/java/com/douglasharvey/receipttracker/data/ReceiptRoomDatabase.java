package com.douglasharvey.receipttracker.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Receipt.class}, version = 5, exportSchema = false)
public abstract class ReceiptRoomDatabase extends RoomDatabase {
    public abstract ReceiptDao receiptDao();

    private static com.douglasharvey.receipttracker.data.ReceiptRoomDatabase INSTANCE;

    static ReceiptRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ReceiptRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ReceiptRoomDatabase.class, "receipt_database")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries() // TODO temporary
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
    

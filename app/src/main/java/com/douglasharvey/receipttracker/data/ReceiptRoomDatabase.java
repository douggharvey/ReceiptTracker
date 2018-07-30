package com.douglasharvey.receipttracker.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Receipt.class}, version = 8)
public abstract class ReceiptRoomDatabase extends RoomDatabase {
    public abstract ReceiptDao receiptDao();

    private static com.douglasharvey.receipttracker.data.ReceiptRoomDatabase INSTANCE;

    static ReceiptRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ReceiptRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ReceiptRoomDatabase.class, "receipt_database")
                            .addMigrations(Migrations.FROM_7_TO_8)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
    

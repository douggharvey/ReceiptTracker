package com.douglasharvey.receipttracker.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import timber.log.Timber;

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
                        //    .addCallback(sRoomDatabaseCallback) //todo needed?
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback(){

        @Override
        public void onOpen (@NonNull SupportSQLiteDatabase db){
            super.onOpen(db);
            Timber.d("onOpen: ");
          //  new PopulateDbAsync(INSTANCE).execute(); //todo can remove this
        }
    };

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final ReceiptDao receiptDao;

        PopulateDbAsync(ReceiptRoomDatabase db) {
            receiptDao = db.receiptDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            Timber.d("doInBackground: ");
            receiptDao.deleteAll();
            Receipt receipt = new Receipt();
            receipt.setCompany("test");
            receiptDao.insert(receipt);
            receipt = new Receipt();
            receipt.setCompany("test2");
            receiptDao.insert(receipt);
            return null;
        }
    }
}
    

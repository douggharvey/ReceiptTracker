package com.douglasharvey.receipttracker.data;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class ReceiptRepository {
    private ReceiptDao receiptDao;
    private LiveData<List<Receipt>> allReceipts;

    public ReceiptRepository(Application application) {
        ReceiptRoomDatabase db = ReceiptRoomDatabase.getDatabase(application);
        this.receiptDao = db.receiptDao();
        this.allReceipts = receiptDao.getReceipts();
    }

    LiveData<List<Receipt>> getAllReceipts() {
        return allReceipts;
    }

    public void insert (Receipt receipt) {
        new insertAsyncTask(receiptDao).execute(receipt);
    }

    private static class insertAsyncTask extends AsyncTask<Receipt, Void, Void> {

        private ReceiptDao asyncTaskDao;

        insertAsyncTask(ReceiptDao receiptDao) {
            asyncTaskDao = receiptDao;
        }

        @Override
        protected Void doInBackground(final Receipt... params) {
            asyncTaskDao.insert(params[0]);
            return null;
        }
    }
}



package com.douglasharvey.receipttracker.data;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

import timber.log.Timber;

public class ReceiptRepository {
    private ReceiptDao receiptDao;
    private LiveData<List<Receipt>> allReceipts;

    public ReceiptRepository(Application application) {
        ReceiptRoomDatabase db = ReceiptRoomDatabase.getDatabase(application);
        this.receiptDao = db.receiptDao();
        this.allReceipts = receiptDao.getReceiptsLiveData();
    }

    LiveData<List<Receipt>> getAllReceipts() {
        return allReceipts;
    }

    public List<Receipt> getReceipts() {
        return receiptDao.getReceipts();
    }

    public List<Receipt> getBlankWebLinks() {
        return receiptDao.getBlankWebLinks();
    }

    public void insert (Receipt receipt) {
        new insertAsyncTask(receiptDao).execute(receipt);
    }

    private static class insertAsyncTask extends AsyncTask<Receipt, Void, Long> {

        private ReceiptDao asyncTaskDao;

        insertAsyncTask(ReceiptDao receiptDao) {
            asyncTaskDao = receiptDao;
        }

        @Override
        protected Long doInBackground(final Receipt... params) {
            if (params[0].getId()!=0) asyncTaskDao.delete(params[0].getId());
            long insertedRowId = asyncTaskDao.insert(params[0]);
            return insertedRowId;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            Timber.d("onPostExecute: id="+aLong);
        }
    }

    public void updateDriveId (String driveId, long recordId) {
        Receipt receipt = new Receipt();
        receipt.setDriveID(driveId);
        receipt.setId((int) recordId);
        new updateDriveIdAsyncTask(receiptDao).execute(receipt);
    }

    public void updateWebLink (String webLink, long recordId) {
        Receipt receipt = new Receipt();
        receipt.setWebLink(webLink);
        receipt.setId((int) recordId);
        new updateWebLinkAsyncTask(receiptDao).execute(receipt);
    }

    private static class updateDriveIdAsyncTask extends AsyncTask<Receipt, Void, Void> {

        private ReceiptDao asyncTaskDao;

        updateDriveIdAsyncTask(ReceiptDao receiptDao) {
            asyncTaskDao = receiptDao;
        }

        @Override
        protected Void doInBackground(final Receipt... params) {
            asyncTaskDao.updateDriveId(params[0].getDriveID(), params[0].getId());
            return null;
        }
    }

    private static class updateWebLinkAsyncTask extends AsyncTask<Receipt, Void, Void> {

        private ReceiptDao asyncTaskDao;

        updateWebLinkAsyncTask(ReceiptDao receiptDao) {
            asyncTaskDao = receiptDao;
        }

        @Override
        protected Void doInBackground(final Receipt... params) {
            asyncTaskDao.updateWebLink(params[0].getWebLink(), params[0].getId());
            return null;
        }
    }
}



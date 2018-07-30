package com.douglasharvey.receipttracker.data;

import android.app.Application;
import android.arch.lifecycle.LiveData;

import com.douglasharvey.receipttracker.utilities.AppExecutors;

import java.util.List;

public class ReceiptRepository {
    private ReceiptDao receiptDao;
    private LiveData<List<Receipt>> allReceipts;
    private LiveData<List<Receipt>> receiptsWithoutWebLink;

    public ReceiptRepository(Application application) {
        ReceiptRoomDatabase db = ReceiptRoomDatabase.getDatabase(application);
        this.receiptDao = db.receiptDao();
        this.allReceipts = receiptDao.getReceiptsLiveData();
        this.receiptsWithoutWebLink = receiptDao.getBlankWebLinks();
    }

    public LiveData<List<Receipt>> getAllReceipts() {
        return allReceipts;
    }

    public LiveData<List<Receipt>> getBlankWebLinks() {
        return receiptsWithoutWebLink;
    }

    public void insert(Receipt receipt) {
        AppExecutors.getInstance().diskIO().execute(() -> receiptDao.insert(receipt));
    }

    public void delete(int receiptId) {
        AppExecutors.getInstance().diskIO().execute(() -> receiptDao.delete(receiptId));
    }

    public void updateWebLink(String webLink, long recordId) {
        AppExecutors.getInstance().diskIO().execute(() -> receiptDao.updateWebLink(webLink, recordId));
    }

}



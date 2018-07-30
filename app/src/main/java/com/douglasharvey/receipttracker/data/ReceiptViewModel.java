package com.douglasharvey.receipttracker.data;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

public class ReceiptViewModel extends AndroidViewModel {

    private ReceiptRepository repository;
    private LiveData<List<Receipt>> allReceipts;
    private LiveData<List<Receipt>> receiptsWithoutWebLink;

    public ReceiptViewModel(@NonNull Application application) {
        super(application);
        repository = new ReceiptRepository(application);
        allReceipts = repository.getAllReceipts();
        receiptsWithoutWebLink = repository.getBlankWebLinks();
    }

    public LiveData<List<Receipt>> getAllReceipts() {
        return allReceipts;
    }
    public LiveData<List<Receipt>> getReceiptswithoutWebLink() {
        return receiptsWithoutWebLink;
    }

  /*  public void insert(Receipt receipt) {
        repository.insert(receipt);
    }
    */
}

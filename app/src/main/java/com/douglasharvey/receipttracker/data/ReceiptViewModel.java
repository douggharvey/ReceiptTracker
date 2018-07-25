package com.douglasharvey.receipttracker.data;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

public class ReceiptViewModel extends AndroidViewModel {

    private ReceiptRepository repository;
    private LiveData<List<Receipt>> allReceipts;

    public ReceiptViewModel(@NonNull Application application) {
        super(application);
        repository = new ReceiptRepository(application);
        allReceipts = repository.getAllReceipts();
    }

    public LiveData<List<Receipt>> getAllReceipts() {
        return allReceipts;
    }

  /*  public void insert(Receipt receipt) {
        repository.insert(receipt);
    }
    */
}

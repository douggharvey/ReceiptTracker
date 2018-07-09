package com.douglasharvey.receipttracker.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.adapters.ReceiptListAdapter;
import com.douglasharvey.receipttracker.data.Receipt;
import com.douglasharvey.receipttracker.data.ReceiptViewModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.rv_receipts)
    RecyclerView rvReceipts;
    @BindView(R.id.fab_add_receipt)
    FloatingActionButton fabAddReceipt;
    private ReceiptViewModel receiptViewModel;
    public static final int NEW_RECEIPT_ACTIVITY_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final ReceiptListAdapter adapter = new ReceiptListAdapter(this);
        rvReceipts.setAdapter(adapter);
        rvReceipts.setLayoutManager(new LinearLayoutManager(this));

        receiptViewModel = ViewModelProviders.of(this).get(ReceiptViewModel.class);
        receiptViewModel.getAllReceipts().observe(this, new Observer<List<Receipt>>() {
            @Override
            public void onChanged(@Nullable final List<Receipt> receipts) {
                adapter.setReceipts(receipts);
            }
        });
        fabAddReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ReceiptActivity.class);
                intent.putExtra(getString(R.string.ADD_RECEIPT_EXTRA), true);
                startActivityForResult(intent, NEW_RECEIPT_ACTIVITY_REQUEST_CODE);
                                         }}); //TODO NEED RESULT?
    }
}

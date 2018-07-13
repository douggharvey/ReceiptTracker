package com.douglasharvey.receipttracker.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.adapters.ReceiptListAdapter;
import com.douglasharvey.receipttracker.data.Receipt;
import com.douglasharvey.receipttracker.data.ReceiptRepository;
import com.douglasharvey.receipttracker.data.ReceiptViewModel;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.rv_receipts)
    RecyclerView rvReceipts;
    @BindView(R.id.fab_add_receipt)
    FloatingActionButton fabAddReceipt;
    private ReceiptViewModel receiptViewModel;
    String[] categoryArray;
    String[] paymentTypeArray;
    boolean exportMenuItemActive=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        categoryArray = this.getResources().getStringArray(R.array.category_array);
        paymentTypeArray = this.getResources().getStringArray(R.array.payment_type_array);

        final ReceiptListAdapter adapter = new ReceiptListAdapter(this);
        rvReceipts.setAdapter(adapter);
        rvReceipts.setLayoutManager(new LinearLayoutManager(this));
        rvReceipts.setHasFixedSize(true);
        RecyclerView.ItemDecoration itemDecoration = new // TODO divider not working - colours?
                DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL);
        rvReceipts.addItemDecoration(itemDecoration);

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
                //startActivityForResult(intent, NEW_RECEIPT_ACTIVITY_REQUEST_CODE);
                startActivity(intent);
            }
        }); //TODO NEED RESULT?

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.menu_export) {

            exportMenuItemActive=false;
            invalidateOptionsMenu();

            //todo add dialog for date selecton for date range, send criteria to dao

            //todo refactor in new class?
            String sourceFileName = "receiptsDOUGLAS.csv";
            File sourceLocation = new File(this.getExternalFilesDir(null) + "/" + sourceFileName);
            Writer fileWriter = null;
            CSVWriter csvWriter;
            FileOutputStream fos;


            try { //todo decide file location. upload to google drive?
                fos = new FileOutputStream(sourceLocation, false);
                fileWriter = new FileWriter(fos.getFD());
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            csvWriter = new CSVWriter(fileWriter);
            ReceiptRepository repository = new ReceiptRepository(getApplication());
            List<Receipt> receiptList = repository.getReceipts();

            //todo need to finish here.
            //dao will be a join to get category.
            //move off main thread
            //finalize formatting
            for (Receipt receipt : receiptList
                    ) {
                String[] fields = new String[7];
                fields[0] = paymentTypeArray[receipt.getType()];
                fields[1] = receipt.getCompany();
                fields[2] = String.valueOf(receipt.getAmount());
                fields[3] = new SimpleDateFormat("dd-MM-yy", Locale.UK).format(receipt.getReceiptDate());
                fields[4] = categoryArray[receipt.getCategory()];
                fields[5] = receipt.getComment();
                fields[6] = receipt.getFile();


                csvWriter.writeNext(fields);
            }
            //todo add closes refer
            // https://gist.github.com/zafe/dfd6a8d0101fc7e3307e
            try {
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileWriter.close();
            } catch (NullPointerException | IOException e ) {
                e.printStackTrace();
            }
            //fos.close();
            Timber.d("onOptionsItemSelected: file written");

            Intent intent = new Intent(MainActivity.this, UploadFileActivity.class);
            intent.putExtra(getString(R.string.UPLOAD_FILE_NAME_EXTRA), sourceFileName);
            intent.putExtra(getString(R.string.UPLOAD_FILE_LOCATION_EXTRA), sourceLocation.toString());
            startActivity(intent);
            Timber.d("onOptionsItemSelected: upload to drive started");
            exportMenuItemActive=true;
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        super.onPrepareOptionsMenu(menu);
        MenuItem export = menu.findItem(R.id.menu_export);
        export.setEnabled(exportMenuItemActive);
        return true;
    }
}

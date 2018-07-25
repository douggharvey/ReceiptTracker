package com.douglasharvey.receipttracker.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.adapters.ReceiptListAdapter;
import com.douglasharvey.receipttracker.data.Receipt;
import com.douglasharvey.receipttracker.data.ReceiptRepository;
import com.douglasharvey.receipttracker.data.ReceiptViewModel;
import com.douglasharvey.receipttracker.interfaces.LongClickItemCallBack;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.tasks.Task;
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
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends BaseDemoActivity
        implements LongClickItemCallBack {

    @BindView(R.id.rv_receipts)
    RecyclerView rvReceipts;
    @BindView(R.id.fab_add_receipt)
    FloatingActionButton fabAddReceipt;
    ReceiptViewModel receiptViewModel;
    String[] categoryArray;
    String[] paymentTypeArray;
    boolean exportMenuItemActive = true;
    @BindView(R.id.receipt_action_import)
    com.github.clans.fab.FloatingActionButton receiptActionImport;
    @BindView(R.id.receipt_action_text)
    com.github.clans.fab.FloatingActionButton receiptActionText;
    @BindView(R.id.receipt_action_camera)
    com.github.clans.fab.FloatingActionButton receiptActionCamera;
    @BindView(R.id.fab_menu)
    FloatingActionMenu fabMenu;
    int deleteId;
    ReceiptListAdapter adapter = null;

    private ActionMode currentActionMode;
    private ActionMode.Callback modeCallBack =
            new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.menu_edit_mode, menu);
                    mode.setTitle("Confirm deletion");
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    ReceiptRepository repository = new ReceiptRepository(getApplication());
                    adapter.clearSelection();
                    repository.delete(deleteId);
                    mode.finish();
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    adapter.clearSelection();
                    currentActionMode = null;
                }

            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        categoryArray = this.getResources().getStringArray(R.array.category_array);
        paymentTypeArray = this.getResources().getStringArray(R.array.payment_type_array);

        adapter = new ReceiptListAdapter(this, this);
        rvReceipts.setAdapter(adapter);
        rvReceipts.setLayoutManager(new LinearLayoutManager(this));
        rvReceipts.setHasFixedSize(true);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(MainActivity.this, LinearLayoutManager.VERTICAL);
        rvReceipts.addItemDecoration(itemDecoration);

        receiptViewModel = ViewModelProviders.of(this).get(ReceiptViewModel.class);
        receiptViewModel.getAllReceipts().observe(this, new Observer<List<Receipt>>() {
            @Override
            public void onChanged(@Nullable final List<Receipt> receipts) {
                adapter.setReceipts(receipts);
            }
        });
        fabAddReceipt.setOnClickListener((View view) -> {
            Intent addReceiptIntent = new Intent(MainActivity.this, ReceiptActivity.class);
            addReceiptIntent.putExtra(getString(R.string.ADD_RECEIPT_EXTRA), true);
            startActivity(addReceiptIntent);
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        fabMenu.close(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ReceiptRepository repository = new ReceiptRepository(getApplication());
        List<Receipt> receiptList = repository.getBlankWebLinks();

        //todo move off main thread
        for (Receipt receipt : receiptList
                ) {
            if (receipt.getDriveID() != null) {
                DriveId driveIdtoDownload = DriveId.decodeFromString(receipt.getDriveID());
                DriveFile driveFile = driveIdtoDownload.asDriveFile();

                retrieveMetadata(receipt.getId(), driveFile);
            }
        }
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

            exportMenuItemActive = false;
            invalidateOptionsMenu();

            //todo add dialog for date selecton for date range, send criteria to dao

            //todo refactor in new class?
            String sourceFileName = "receiptsDOUGLAS.csv";
            File sourceLocation = new File(this.getExternalFilesDir(null) + "/" + sourceFileName);
            Writer fileWriter = null;
            CSVWriter csvWriter;
            FileOutputStream fos;


            try {
                fos = new FileOutputStream(sourceLocation, false);
                fileWriter = new FileWriter(fos.getFD());
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            csvWriter = new CSVWriter(fileWriter);
            ReceiptRepository repository = new ReceiptRepository(getApplication());
            List<Receipt> receiptList = repository.getReceipts();

            //todo move off main thread, finalize formatting
            for (Receipt receipt : receiptList
                    ) {
                String[] fields = new String[8];
                fields[0] = paymentTypeArray[receipt.getType()];
                fields[1] = new SimpleDateFormat("dd-MM-yy", Locale.UK).format(receipt.getReceiptDate());
                fields[2] = receipt.getCompany();
                fields[3] = categoryArray[receipt.getCategory()];
                fields[4] = receipt.getComment();
                fields[5] = String.valueOf(receipt.getAmount() * -1);
                fields[6] = ""; //blank field for total in Excel
                fields[7] = receipt.getWebLink();
                csvWriter.writeNext(fields);
            }
            //todo add closes refer https://gist.github.com/zafe/dfd6a8d0101fc7e3307e
            try {
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileWriter.close();
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
            //fos.close();
            Timber.d("onOptionsItemSelected: file written");

            Intent intent = new Intent(MainActivity.this, UploadFileActivity.class);
            intent.putExtra(getString(R.string.UPLOAD_FILE_NAME_EXTRA), sourceFileName);
            intent.putExtra(getString(R.string.UPLOAD_FILE_LOCATION_EXTRA), sourceLocation.toString());
            startActivity(intent);
            Timber.d("onOptionsItemSelected: upload to drive started");
            exportMenuItemActive = true;
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem export = menu.findItem(R.id.menu_export);
        export.setEnabled(exportMenuItemActive);
        return true;
    }

    @Override
    protected void onDriveClientReady() {

    }

    private void retrieveMetadata(final long recordId, final DriveFile file) {
        Task<Metadata> getMetadataTask = getDriveResourceClient().getMetadata(file);
        getMetadataTask
                .addOnSuccessListener(this,
                        metadata -> {
                            updateLink(recordId, metadata.getAlternateLink());
                        })
                .addOnFailureListener(this, e -> {
                    Timber.d("retrieveMetadata: metadata read failed");
                });
    }

    private void updateLink(long recordId, String link) {
        ReceiptRepository repository = new ReceiptRepository(getApplication());
        repository.updateWebLink(link, recordId);
    }

    @OnClick({R.id.receipt_action_import, R.id.receipt_action_text, R.id.receipt_action_camera})
    public void onViewClicked(View view) {
        Intent addReceiptIntent;
        switch (view.getId()) {
            case R.id.receipt_action_import:
                addReceiptIntent = new Intent(MainActivity.this, ReceiptActivity.class);
                addReceiptIntent.putExtra(getString(R.string.ADD_RECEIPT_EXTRA), true);
                startActivity(addReceiptIntent);
                break;
            case R.id.receipt_action_text:
                addReceiptIntent = new Intent(MainActivity.this, ReceiptActivity.class);
                addReceiptIntent.putExtra(getString(R.string.ADD_RECEIPT_EXTRA), true);
                addReceiptIntent.putExtra(getString(R.string.ADD_RECEIPT_EXTRA_TEXTONLY), true);
                startActivity(addReceiptIntent);
                break;
            case R.id.receipt_action_camera: //Camera option inactive for now.
                break;
        }
    }

    @Override
    public void triggerEditMode(int receiptId) {
        if (currentActionMode == null) {
            currentActionMode = startActionMode(modeCallBack);
        }
        this.deleteId = receiptId;
    }

}

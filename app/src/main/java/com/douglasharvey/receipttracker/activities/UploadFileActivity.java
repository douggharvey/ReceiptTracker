package com.douglasharvey.receipttracker.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.douglasharvey.receipttracker.R;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

import static com.google.android.gms.drive.DriveId.decodeFromString;

public class UploadFileActivity extends BaseDemoActivity {

    @Override
    protected void onDriveClientReady() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String driveID = sharedPreferences.getString(getString(R.string.receiptTrackerFolderDriveID), getString(R.string.receiptTrackerFolderIDmissing)); //should also query drive to ensure it still exists
        if (driveID.equals(getString(R.string.receiptTrackerFolderIDmissing))) {
            pickFolder()
                    .addOnSuccessListener(this,
                            driveId -> {
                                createReceiptTrackerFolder(driveId.asDriveFolder());

                            })
                    .addOnFailureListener(this, e -> {
                        Timber.d("onDriveClientReady: No folder selected" + e);
                        showMessage(getString(R.string.folder_not_selected));
                        finish();
                    });
        } else {
            uploadFile(decodeFromString(driveID).asDriveFolder());
            finish();
        }
    }

    //creates "ReceiptTracker" directory in chosen directory then upload file to that directory.
    //need to query metadata to get web path & upload to database.
    private void createReceiptTrackerFolder(final DriveFolder parent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(getString(R.string.receiptTrackerFolderTitle))
                .setMimeType(DriveFolder.MIME_TYPE)
                .build();

        getDriveResourceClient()
                .createFolder(parent, changeSet)
                .addOnSuccessListener(this,
                        driveFolder -> {
                            showMessage(getString(R.string.file_created,
                                    driveFolder.getDriveId().encodeToString()));
                            uploadFile(driveFolder);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            String folderDriveID = getString(R.string.receiptTrackerFolderDriveID);
                            editor.putString(folderDriveID, driveFolder.getDriveId().encodeToString());
                            editor.apply(); //todo consider also saving directory and give option to change it for future receipts
                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    Timber.d("createReceiptTrackerFolder: Unable to create folder" + e);
                    showMessage(getString(R.string.file_create_error));
                    finish();
                });
    }

    private void uploadFile(final DriveFolder parent) {

        Intent intent = getIntent();
        String uploadFileName = intent.getStringExtra(getString(R.string.UPLOAD_FILE_NAME_EXTRA));
        String uploadFileLocation = intent.getStringExtra(getString(R.string.UPLOAD_FILE_LOCATION_EXTRA));

        getDriveResourceClient()
                .createContents()
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    OutputStream outputStream = contents.getOutputStream();
                    writeFile(uploadFileLocation, outputStream);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(uploadFileName)
                            .setPinned(true) // only if needed, offline does not seem to work. setstarred?
                            .build();

                    return getDriveResourceClient().createFile(parent, changeSet, contents);
                })
                .addOnSuccessListener(this,
                        driveFile -> {
                            showMessage(getString(R.string.file_created,
                                    driveFile.getDriveId().encodeToString()));

                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    showMessage(getString(R.string.file_create_error));
                    finish();
                });
    }

    private void writeFile(String uploadFileLocation, OutputStream outputStream) {
        try {
            InputStream inputStream = new FileInputStream(uploadFileLocation);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();

            outputStream.flush();
            outputStream.close();

            new File(uploadFileLocation).delete();

        } catch (Exception e) {
            Timber.d("writeFile: " + e.getMessage());
        }
    }
}

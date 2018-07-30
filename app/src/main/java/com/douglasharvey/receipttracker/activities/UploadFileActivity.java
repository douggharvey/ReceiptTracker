package com.douglasharvey.receipttracker.activities;

import android.app.Activity;
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
        String driveID = sharedPreferences.getString(getString(R.string.receiptTrackerFolderDriveID), getString(R.string.receiptTrackerFolderIDmissing)); //TODO should also query drive to ensure it still exists
        if (driveID.equals(getString(R.string.receiptTrackerFolderIDmissing))) {
            pickFolder()
                    .addOnSuccessListener(this,
                            driveId -> {
                                createReceiptTrackerFolder(driveId.asDriveFolder());
                                Timber.d("onDriveClientReady: selected folder driveID: "+driveId.encodeToString());

                            })
                    .addOnFailureListener(this, e -> {
                        showMessage(getString(R.string.folder_not_selected));
                        finish();
                    });
        } else {
            uploadFile(decodeFromString(driveID).asDriveFolder());
         //   finish();
        }
    }

    //creates "ReceiptTracker" directory in chosen directory then upload file to that directory.
    //TODO currently does not check if this directory already exists which can result in a duplicate directory
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
                          //  finish();
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
        Timber.d("uploadFile: "+uploadFileName+uploadFileLocation);
        getDriveResourceClient()
                .createContents()
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    OutputStream outputStream = contents.getOutputStream();
                    writeFile(uploadFileLocation, outputStream);
                    Timber.d("uploadFile: after writefile");
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(uploadFileName)
                            .setPinned(true) // only if needed, offline does not seem to work. Also explore caching. After viewing automatic caching occurs.
                            //refer to https://developers.google.com/drive/android/pinning. May not be available for documents.
                            .build();
                    Timber.d("uploadFile: after meta build");
                    return getDriveResourceClient().createFile(parent, changeSet, contents);
                })
                .addOnSuccessListener(this,
                        driveFile -> {
                            Timber.d("uploadFile: "+driveFile.getDriveId().encodeToString());
                            showMessage(getString(R.string.file_created,
                                    driveFile.getDriveId().encodeToString()));
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("createdDriveID",driveFile.getDriveId().encodeToString());
                            setResult(Activity.RESULT_OK, returnIntent);
                            Timber.d("uploadFile: set result OK");
                            //todo consider saving this driveId then using the below method to retrieve the file.
///https://stackoverflow.com/questions/43169487/how-to-download-and-save-in-local-directory-from-google-drive-selected-file-in-a
//https://stackoverflow.com/questions/31759473/download-selected-file-from-google-drive/31761586#31761586
//https://github.com/seanpjanson/GDAADemo
                            //also see official demos - retreivemetadata/retirevcontents - these may be the best bet
                            //https://developers.google.com/drive/android/files


                            finish(); //only finish activity when we are 100% complete
                            //todo need to query metadata to get web path & upload to database.
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
            Timber.d("writeFile: completed successfully");

        } catch (Exception e) {
            Timber.d("writeFile: " + e.getMessage());
        }
    }
}

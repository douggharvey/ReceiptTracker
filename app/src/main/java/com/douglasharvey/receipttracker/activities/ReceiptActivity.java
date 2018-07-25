package com.douglasharvey.receipttracker.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.data.Receipt;
import com.douglasharvey.receipttracker.data.ReceiptRepository;
import com.douglasharvey.receipttracker.data.ReceiptResult;
import com.douglasharvey.receipttracker.fragments.DatePickerFragment;
import com.douglasharvey.receipttracker.utilities.DateUtils;
import com.douglasharvey.receipttracker.utilities.FileUtils;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.douglasharvey.receipttracker.utilities.ProcessTextRecognition.processTextRecognitionResult;

public class ReceiptActivity extends BaseDemoActivity implements DatePickerFragment.EditDateDialogListener {

    //TODO add/remove attachment.

    @BindView(R.id.tv_edit_new_heading)
    TextView tvEditNewHeading;
    @BindView(R.id.tv_ocr_company_name)
    TextView tvOcrCompanyName;
    @BindView(R.id.et_company_name)
    EditText etCompanyName;
    @BindView(R.id.tv_ocr_amount)
    TextView tvOcrAmount;
    @BindView(R.id.et_amount)
    EditText etAmount;
    @BindView(R.id.tv_ocr_date)
    TextView tvOcrDate;
    @BindView(R.id.et_date)
    EditText etDate;
    @BindView(R.id.receipt_details)
    ConstraintLayout receiptDetails;
    @BindView(R.id.graphic_overlay)
    GraphicOverlay graphicOverlay;
    @BindView(R.id.iv_pdf)
    SubsamplingScaleImageView ivPdf;
    @BindView(R.id.fab_add_receipt)
    FloatingActionButton fabAddReceipt;
    @BindView(R.id.tv_ocr_payment_type)
    TextView tvOcrPaymentType;
    @BindView(R.id.sp_payment_type)
    Spinner spPaymentType;
    @BindView(R.id.sp_category)
    Spinner spCategory;
    @BindView(R.id.iv_save_receipt)
    ImageView ivSaveReceipt;
    @BindView(R.id.et_comment)
    EditText etComment;
    private Bitmap selectedImage;
    private Uri selectedDocument = null;
    private static final int REQUEST_OPEN = 1;
    private static final int REQUEST_UPLOAD = 2;
    private static final String STATE_SELECTED = "selected";
    BottomSheetBehavior sheetBehavior;
    int peekHeight = 100;
    String sourceFileName;
    File sourceLocation;
    File newFileName;
    private static Receipt receipt;
    String[] paymentTypeArray;
    String[] categoryArray;
    private int RECEIPT_MODE;
    private final int ADD_MODE = 1;
    private final int EDIT_MODE = 2;
    private final int ADD_MODE_TEXT_ONLY = 3;
    int existingReceiptID;
    String existingFileDriveId;
    String uploadedFileDriveId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_main);
        ButterKnife.bind(this);
        paymentTypeArray = this.getResources().getStringArray(R.array.payment_type_array);
        categoryArray = this.getResources().getStringArray(R.array.category_array);

        Intent receivedIntent = getIntent();
        if (receivedIntent.hasExtra(getString(R.string.ADD_RECEIPT_EXTRA))) { // if FAB button was pressed on MainActivity go direct to new receipt import processing.
            if (receivedIntent.getBooleanExtra(getString(R.string.ADD_RECEIPT_EXTRA), false)) {
                existingReceiptID = 0;
                existingFileDriveId = null;
                if (receivedIntent.getBooleanExtra(getString(R.string.ADD_RECEIPT_EXTRA_TEXTONLY), false)) {
                    RECEIPT_MODE = ADD_MODE_TEXT_ONLY;
                    tvEditNewHeading.setText(R.string.header_new_receipt); // No Image text only entry
                    etDate.setText(DateUtils.getTodaysDate());
                }
                else {
                    RECEIPT_MODE = ADD_MODE;
                    addReceipt();
                }
            }
        }

        if (savedInstanceState != null) {
            selectedDocument = savedInstanceState.getParcelable(STATE_SELECTED);

            if (selectedDocument != null) {
                Timber.d("onCreate: show "+selectedDocument.toString());
                show(selectedDocument);
            }
        }

        fabAddReceipt.setOnClickListener((View view) -> {
            RECEIPT_MODE = ADD_MODE;
            addReceipt();
        });

        createSaveReceiptClickListener();

        setupBottomSheet();

        initializeEntryFields();

        if (receivedIntent.hasExtra(getString(R.string.EDIT_RECEIPT_EXTRA))) {
            Bundle data = receivedIntent.getExtras();
            receipt = data.getParcelable(getString(R.string.EDIT_RECEIPT_EXTRA));
            existingReceiptID = receipt.getId();
            existingFileDriveId = receipt.getDriveID();
            if (existingFileDriveId == null || existingFileDriveId.isEmpty()) sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            Timber.d("onCreate: editReceipt:"+existingFileDriveId);
            RECEIPT_MODE = EDIT_MODE;
            editReceipt();
        }
    }

    private void createSaveReceiptClickListener() {
        ivSaveReceipt.setOnClickListener((View view) -> {
            if (etCompanyName.getText().toString().isEmpty()) {
                Toast.makeText(ReceiptActivity.this, "Please enter Company Name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (etAmount.getText().toString().isEmpty()) {
                Toast.makeText(ReceiptActivity.this, "Please enter Amount", Toast.LENGTH_SHORT).show();
                return;
            }
            //todo need validation for date?
            if (RECEIPT_MODE == ADD_MODE)
                setSourceVariables();
            else if ((RECEIPT_MODE != ADD_MODE_TEXT_ONLY) && receipt.getDriveID()!=null) setSourceVariables();
            // renaming file to app directory prior to uploading to google drive
            // this will prevent user from trying to add this receipt again.
            if (RECEIPT_MODE == ADD_MODE) {
                boolean renameResult = renameFile();
                if (renameResult) {
                    Intent uploadIntent = new Intent(ReceiptActivity.this, UploadFileActivity.class);
                    uploadIntent.putExtra(getString(R.string.UPLOAD_FILE_NAME_EXTRA), sourceFileName);
                    uploadIntent.putExtra(getString(R.string.UPLOAD_FILE_LOCATION_EXTRA), newFileName.toString());
                    startActivityForResult(uploadIntent, REQUEST_UPLOAD);
                }
                else {
                    Toast.makeText(this, "Unable to move file", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            if ((RECEIPT_MODE == EDIT_MODE) ||
               (RECEIPT_MODE == ADD_MODE_TEXT_ONLY))
            {
                insertReceipt();
                finish();
            }
        });
    }

    private void insertReceipt() {
        ReceiptRepository receiptRepository = new ReceiptRepository(getApplication());
        Receipt receipt = new Receipt();
        receipt.setCompany(etCompanyName.getText().toString());
        receipt.setAmount(Float.parseFloat(etAmount.getText().toString()));
        int selectedCategoryPosition = spCategory.getSelectedItemPosition();
        receipt.setCategory(selectedCategoryPosition);
        receipt.setComment(etComment.getText().toString());
        int selectedTypePosition = spPaymentType.getSelectedItemPosition();
        receipt.setType(selectedTypePosition);
//        receipt.setFile(selectedDocument.toString()); todo may remove this later
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        try {
            receipt.setReceiptDate(formatter.parse(etDate.getText().toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Timber.d("insertReceipt: Receipt_mode:"+RECEIPT_MODE);
        if (RECEIPT_MODE == EDIT_MODE) { //passing existing ID's to delete existing record and add again with the same ID's in one transaction
            receipt.setId(existingReceiptID);
            receipt.setDriveID(existingFileDriveId);
            Timber.d("insertReceipt: existingFileDriveId:"+existingFileDriveId);
        }

        if (RECEIPT_MODE == ADD_MODE) receipt.setDriveID(uploadedFileDriveId);

        receiptRepository.insert(receipt);
    }

    private void initializeEntryFields() {
        etDate.setShowSoftInputOnFocus(false);
        ArrayAdapter paymentTypeAdapter = ArrayAdapter.createFromResource(this, R.array.payment_type_array, R.layout.spinner_payment_type);
        paymentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentType.setAdapter(paymentTypeAdapter);
        spPaymentType.setSelection(0);

        ArrayAdapter categoryAdapter = ArrayAdapter.createFromResource(this, R.array.category_array, R.layout.spinner_category);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
        spCategory.setSelection(1); //default to groceries
    }

    private void setupBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(receiptDetails);
        if (RECEIPT_MODE == ADD_MODE_TEXT_ONLY)
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        else sheetBehavior.setPeekHeight(0);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior.setPeekHeight(peekHeight);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset == 0) {
                  //todo to be removed  fabAddReceipt.show();
                }
                if ((slideOffset > 0) && (fabAddReceipt.getVisibility() == View.VISIBLE)) {
                    fabAddReceipt.hide();
                }
            }
        });
    }

    private void editReceipt() {
        if (receipt.getFile()!=null) selectedDocument = Uri.parse(receipt.getFile());
//        show(selectedDocument); todo
        tvEditNewHeading.setText(R.string.header_edit_receipt);
        etCompanyName.setText(receipt.getCompany());
        etAmount.setText(new DecimalFormat("######.00").format(receipt.getAmount()));
        Date receiptDate = receipt.getReceiptDate();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        etDate.setText(formatter.format(receiptDate));
        sheetBehavior.setPeekHeight(peekHeight);
        setSpinnerToValue(spPaymentType, paymentTypeArray[receipt.getType()]);
        setSpinnerToValue(spCategory, categoryArray[receipt.getCategory()]);
        etComment.setText(receipt.getComment());
        //todo add savereceipt processing with update to database, try to integrate with existing add proeessing
        // also add delete processing - delete record but keep receipt document where it is.
    }

    private boolean renameFile() {
        newFileName = new File(this.getExternalFilesDir(null), "/" + sourceFileName);
        //todo need to add obtain permissions code see busy coder or easypermissions
        //or manually update permission after each clean install!

        //todo consider whether to update file field on database
        boolean renameResult = false;
        if (sourceLocation != null && sourceLocation.exists()) {
            renameResult = sourceLocation.renameTo(newFileName);
            if (renameResult)
                Toast.makeText(this, "Move file successful", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Move file failed: check permissions", Toast.LENGTH_SHORT).show();

        } else
            Toast.makeText(this, "File cannot be selected from here (google drive)", Toast.LENGTH_SHORT).show();

        return renameResult;
    }

    private void setSourceVariables() {
        sourceLocation = null;

        try {
            String path = FileUtils.getPath(this, selectedDocument);
            if (path == null) return;
            sourceLocation = new File(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Timber.d("setSourceVariables: sourceLocation: " + sourceLocation.toString());

        sourceFileName = sourceLocation.getName();
    }

    public void setSpinnerToValue(Spinner spinner, String value) {
        int index = 0;
        SpinnerAdapter adapter = spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                index = i;
                break;
            }
        }
        spinner.setSelection(index);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(STATE_SELECTED, selectedDocument);
    }

    private void addReceipt() {
        etCompanyName.setText("");
        etAmount.setText("");
        etDate.setText("");
        etComment.setText("");
        open();
    }

    private void open() {
        Intent i = new Intent()
                .setType("application/pdf")
                .setAction(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(i, REQUEST_OPEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        Timber.d("onActivityResult: "+resultCode+" "+requestCode );
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OPEN) {
                selectedDocument = data.getData();
                boolean showResult = show(selectedDocument);
                if (showResult) runTextRecognition();
                else Toast.makeText(this, "Unable to show document", Toast.LENGTH_SHORT).show();
            }
            if (requestCode == REQUEST_UPLOAD) {
                Timber.d("onActivityResult: request_upload");
                uploadedFileDriveId = data.getStringExtra("createdDriveID");
                Timber.d("onActivityResult: uploadedID:"+uploadedFileDriveId);
                insertReceipt();
                finish();
            }
        }
    }

    @Override
    protected void onDriveClientReady() {
        Timber.d("onDriveClientReady: mode:"+RECEIPT_MODE);
        if (RECEIPT_MODE == EDIT_MODE) downloadExistingFile();
    }

    private void downloadExistingFile() {
        String driveId = receipt.getDriveID();
        if (driveId==null) {
            return;
        }
        DriveId driveIdtoDownload = DriveId.decodeFromString(receipt.getDriveID());
        DriveFile driveFile = driveIdtoDownload.asDriveFile();
        Task<DriveContents> openFileTask =
                getDriveResourceClient().openFile(driveFile, DriveFile.MODE_READ_WRITE);
        openFileTask
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    show(contents.getParcelFileDescriptor());

                    return getDriveResourceClient().discardContents(contents);
                })
                .addOnFailureListener(e -> {
                    Timber.d("downloadExistingFile: Unable to read contents");
                    showMessage("Unable to read file contents");
                });
    }

    private boolean show(Uri selectedDocument) {
        //todo class is not thread safe
        ParcelFileDescriptor parcelFileDescriptor = null;

        try {
            parcelFileDescriptor = this.getContentResolver().openFileDescriptor(
                    selectedDocument, "r");
        } catch (Exception e) //refine this, which exception may occur??
        {
            e.printStackTrace();
        }

        return show(parcelFileDescriptor);
    }

    private boolean show (ParcelFileDescriptor parcelFileDescriptor) {
        PdfRenderer renderer = null;
        selectedImage = null;

        if (parcelFileDescriptor == null)
            return false;
        try {
            renderer = new PdfRenderer(parcelFileDescriptor); //todo prevent this warning
            //TODO W/System.err: java.io.IOException: cannot create document. Error: 3
            //07-17 22:42:17.640 27688-27688/com.douglasharvey.receipttracker W/System.err:     at android.graphics.pdf.PdfRenderer.nativeCreate(Native Method)
            //        at android.graphics.pdf.PdfRenderer.<init>(PdfRenderer.java:153)
        } catch (IOException e) {
            e.printStackTrace();
        }
        PdfRenderer.Page page = renderer.openPage(0); // can return null if no page

        int height = 1550;
        int width = height * page.getWidth() / page.getHeight();

        selectedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        selectedImage.eraseColor(0xFFFFFFFF);
        page.render(selectedImage, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        ivPdf.resetScaleAndCenter();
        ivPdf.setImage(ImageSource.cachedBitmap(selectedImage));
        page.close();
        return true;

    }

    private void runTextRecognition() {
        graphicOverlay.clear();

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(selectedImage);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();

        detector.detectInImage(image)
                .addOnSuccessListener(
                        texts -> {
                            ReceiptResult receiptResult = new ReceiptResult();

                            List<FirebaseVisionText.Element> elements = processTextRecognitionResult(texts, receiptResult);
                            tvEditNewHeading.setText(R.string.header_new_receipt);
                            tvOcrCompanyName.setText(receiptResult.getCompany());
                            etCompanyName.setText(receiptResult.getCompany());
                            String receiptAmount = receiptResult.getAmount();
                            tvOcrAmount.setText(receiptAmount);
                            etAmount.setText(extractAmount(receiptAmount));
                            tvOcrDate.setText(receiptResult.getDate());
                            etDate.setText(extractDate(receiptResult.getDate()));
                            tvOcrPaymentType.setText(receiptResult.getPaymentType());
                            setPaymentTypeSpinner(receiptResult);
                            sheetBehavior.setPeekHeight(peekHeight);
                            spCategory.setSelection(1); // default to groceries

                            if (elements != null) {
                                for (int m = 0; m < elements.size(); m++) {
                                    GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(m));
                                    graphicOverlay.add(textGraphic);
                                }
                            }
                            //todo consider removing GraphicOverlay!! not needed & currently does not work with imageview either scaled or not scaled
//todo performance - : Skipped 138 frames!  The application may be doing too much work on its main thread.

                        })
                .addOnFailureListener(
                        e -> {
                            e.printStackTrace();
                        });
    }

    private void setPaymentTypeSpinner(ReceiptResult receiptResult) {
        String paymentType = receiptResult.getPaymentType();
        if (paymentType != null && !paymentType.isEmpty()) {
            if ((paymentType.contains("KRE")) ||
                    (paymentType.contains("KART"))) {
                setSpinnerToValue(spPaymentType, "Credit Card");
            }
            if (paymentType.contains("NAK")) {
                setSpinnerToValue(spPaymentType, "Cash");
            }
        }
    }

    private String extractAmount(String amount) {
        String extractedAmount;
        if (amount == null || amount.isEmpty())
            return null;
        extractedAmount = amount.replaceAll("[^0-9.,]", "");
        extractedAmount = extractedAmount.replace(",", ".");
        return extractedAmount;

    }

    private String extractDate(String date) {
        String extractedDate;
        if (date == null || date.isEmpty())
            return null;
        extractedDate = date.replaceAll("[^0-9./,]", "");
        extractedDate = extractedDate.replace('.', '/');
        extractedDate = extractedDate.replace(',', '/');
        if (extractedDate.length() > 12) extractedDate = null;
        return extractedDate;
    }

    public void ReceiptDetailsOnClick(View view) {
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            sheetBehavior.setPeekHeight(150);
            fabAddReceipt.hide();
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            fabAddReceipt.show();
        }
    }

    public void showDatePickerDialog(View v) {

        DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

        DialogFragment newFragment = new DatePickerFragment();
        String dateText = etDate.getText().toString();
        dateText = dateText.replace('.', '/'); //todo move this
        if (!dateText.isEmpty()) {
            try {
                Date enteredDate = sdf.parse(dateText);
                Calendar cal = Calendar.getInstance();
                cal.setTime(enteredDate);
                Bundle bundle = new Bundle();
                bundle.putInt("setYear", cal.get(Calendar.YEAR));
                bundle.putInt("setMonth", cal.get(Calendar.MONTH));
                bundle.putInt("setDay", cal.get(Calendar.DAY_OF_MONTH));
                newFragment.setArguments(bundle);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        hideSoftKeyboard(v);
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public void onFinishEditDialog(int year, int month, int day) {
        etDate.setText(DateUtils.formatDate(year,month,day));
    }


}

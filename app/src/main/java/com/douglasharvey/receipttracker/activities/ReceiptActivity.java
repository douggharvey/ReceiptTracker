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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.data.Receipt;
import com.douglasharvey.receipttracker.data.ReceiptRepository;
import com.douglasharvey.receipttracker.data.ReceiptResult;
import com.douglasharvey.receipttracker.fragments.DatePickerFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.douglasharvey.receipttracker.utilities.ProcessTextRecognition.processTextRecognitionResult;

public class ReceiptActivity extends AppCompatActivity implements DatePickerFragment.EditDateDialogListener {
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
    private static final String STATE_SELECTED = "selected";
    BottomSheetBehavior sheetBehavior;
    int peekHeight = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_main);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        if (intent.getBooleanExtra(getString(R.string.ADD_RECEIPT_EXTRA),false)) addReceipt();

        if (savedInstanceState != null) {
            selectedDocument = savedInstanceState.getParcelable(STATE_SELECTED);

            if (selectedDocument != null) {
                show(selectedDocument);
            }
        }

        fabAddReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addReceipt();
            }
        });

        ivSaveReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReceiptRepository receiptRepository = new ReceiptRepository(getApplication());
                Receipt receipt = new Receipt();
                receipt.setCompany(etCompanyName.getText().toString());
                receipt.setAmount(Float.parseFloat(etAmount.getText().toString()));
                receipt.setCategory(1);
                receipt.setComment(etComment.getText().toString());
                receipt.setType(2);
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    receipt.setReceiptDate(formatter.parse(etDate.getText().toString()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                //todo add date & url later
                receiptRepository.insert(receipt);
                finish();
            }
        });

        sheetBehavior = BottomSheetBehavior.from(receiptDetails);
        sheetBehavior.setPeekHeight(0);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior.setPeekHeight(peekHeight);
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                if (slideOffset == 0) {
                    fabAddReceipt.show();
                }
                if ((slideOffset > 0) && (fabAddReceipt.getVisibility() == View.VISIBLE)) {
                    fabAddReceipt.hide();
                }
            }
        });
        etDate.setShowSoftInputOnFocus(false);
        ArrayAdapter paymentTypeAdapter = ArrayAdapter.createFromResource(this, R.array.payment_type_arrays, R.layout.spinner_payment_type);
        paymentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentType.setAdapter(paymentTypeAdapter);
        spPaymentType.setSelection(0);

        ArrayAdapter categoryAdapter = ArrayAdapter.createFromResource(this, R.array.category_array, R.layout.spinner_category);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
        spCategory.setSelection(0);
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
        if (resultCode == Activity.RESULT_OK) {
            selectedDocument = data.getData();
            show(selectedDocument);
            runTextRecognition();
        }
    }

    private void show(Uri selectedDocument) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        PdfRenderer renderer = null;
        selectedImage = null;
        //ParcelFileDescriptor pfd =  openFileDescriptor(uri, "r");
        try {
            parcelFileDescriptor = this.getContentResolver().openFileDescriptor(
                    selectedDocument, "r");
        } catch (Exception e) //refine this, which exception may occur??
        {
            e.printStackTrace();
        }

        try {
            renderer = new PdfRenderer(parcelFileDescriptor); //todo prevent this warning
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
    }


    private void runTextRecognition() {
        graphicOverlay.clear();

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(selectedImage);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();
        //mButton.setEnabled(false);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                //                      mButton.setEnabled(true);
                                ReceiptResult receiptResult = new ReceiptResult();

                                List<FirebaseVisionText.Element> elements = processTextRecognitionResult(texts, receiptResult);
                                tvEditNewHeading.setText("New Receipt");
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
                                spCategory.setSelection(0);

                                //      etPaymentType.setText(receiptResult.getPaymentType());

                           /*     tvReceiptDetails.setText("Receipt details:\n\nCompany: " + receiptResult.getCompany());
                                tvReceiptDetails.append("\n\nAmount: " + receiptResult.getAmount());
                                tvReceiptDetails.append("\n\nDate: " + receiptResult.getDate());
                                tvReceiptDetails.append("\n\nPayment Type: " + receiptResult.getPaymentType());
*/
                                if (elements != null) {
                                    for (int m = 0; m < elements.size(); m++) {
                                        GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(m));
                                        graphicOverlay.add(textGraphic);
                                    }
                                }
                                //todo consider removing GraphicOverlay!! not needed & currently does not work with imageview either scaled or not scaled
//todo performance - : Skipped 138 frames!  The application may be doing too much work on its main thread.

                                //todo latest test on A101  receipt - got KDV instead of Total line
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                //     mButton.setEnabled(true);
                                e.printStackTrace();
                            }
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

        DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

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
        etDate.setText(day + "/" + (month + 1) + "/" + year); //todo correctly format the date
    }
}
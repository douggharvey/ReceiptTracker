package com.douglasharvey.receipttracker.utilities;

import android.graphics.Point;

import com.douglasharvey.receipttracker.data.ReceiptResult;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static org.apache.commons.lang3.StringUtils.replace;

public class ProcessTextRecognition {
    private static int toplamYPosition1;
    private static int toplamYPosition2;
    private static String totalAmount;
    private static String paymentType;
    private static String receiptDate;
    private static String company;
    private static String firstWord;

    public static List<FirebaseVisionText.Element> processTextRecognitionResult(FirebaseVisionText texts, ReceiptResult receiptResult) {
        toplamYPosition1 = 0;
        toplamYPosition2 = 0;
        totalAmount = null;
        paymentType = null;
        receiptDate = null;
        company = null;
        firstWord = null;
        List<FirebaseVisionText.Element> extractedElements = null;
        List<FirebaseVisionText.Element> allElements = new ArrayList<>();
        List<FirebaseVisionText.Block> blocks = texts.getBlocks();

        if (blocks.size() == 0) {
            //      showToast("No text found");
            return null;
        }
        //    mGraphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                allElements.addAll(lines.get(j).getElements());
            }
        }
        extractedElements = filterElements(allElements);

 /*       for (int m = 0; m < extractedElements.size(); m++) {
            Graphic textGraphic = new TextGraphic(mGraphicOverlay, extractedElements.get(m));
            mGraphicOverlay.add(textGraphic);
        }
   */
        Timber.d("processTextRecognitionResult: " +
                "Company: " + company + "\n"
                + "First word: " + firstWord + "\n"
                + "Payment Type: " + paymentType + "\n"
                + "Amount: " + totalAmount + "\n"
                + "Date: " + receiptDate + "\n"
        );
        if (company == null || company.isEmpty()) receiptResult.setCompany(firstWord);
        else receiptResult.setCompany(company);
        receiptResult.setAmount(totalAmount);
        if (receiptDate==null) receiptDate=DateUtils.getTodaysDate();
        receiptResult.setDate(receiptDate);
        receiptResult.setPaymentType(paymentType);
        return extractedElements; // also above items!

    }

    private static List<FirebaseVisionText.Element> filterElements(List<FirebaseVisionText.Element> elements) {
        boolean companyNameExists, paymentTypeExists, dateExists;
        List<FirebaseVisionText.Element> extractedElements = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            FirebaseVisionText.Element element = elements.get(i);
            //    if ((i==0)&&(firstWord==null||firstWord.isEmpty())) firstWord = element.getText(); //TODO this doesn't work, seems to get leftmost field first!!
            Timber.d("filterElements: " + element.getText().toString());
            companyNameExists = companyNameCheck(element.getText());
            paymentTypeExists = paymentTypeCheck(element.getText());
            dateExists = dateValidate(element.getText());

            if (companyNameExists ||
                    element.getText().contains(",") || // amounts
                    paymentTypeExists ||
                    totalCheck(element.getText()) ||
                    dateExists
                    ) { //consider "matches" here instead

                Point[] cornerPoints = element.getCornerPoints();

                if (cornerPoints.length > 0) { // get corner points
                    for (int j = 0; j < cornerPoints.length; j++) {
                        Point cornerPoint = cornerPoints[j];

                        if (totalCheck(element.getText())) {
                            Timber.d("filterElements: totalcheck positive:" + element.getText());
                            if (toplamYPosition1 == 0) toplamYPosition1 = cornerPoint.y;
                            else toplamYPosition2 = cornerPoint.y; // get y position from 2 corners
                            Timber.d("filterElements: toplamYposition1:" + toplamYPosition1);
                            Timber.d("filterElements: toplamYposition2:" + toplamYPosition2);
                        } else if (!element.getText().contains(",")) {
                            extractedElements.add(elements.get(i)); // add to output if not related to amount.
                        }
                    }
                }
            }
           //    extractedElements.add(elements.get(i)); //uncomment this to get all boxes
/*TODO
1) İF no match on name, consider getting first line in entirety. or do this instead
  2) image 6 - improve date matching
  3) consider getting time
  4) image 11 - some issues with too many boxes, use ORTAK POS but does not work
5) İMAGE 13 - does not find name? - CLOUD RECOGNITION DOES IT OK - IMAGE QUALITY KEY HERE!
  */
        }

        findTotalAmount(elements, extractedElements);

        return extractedElements;
    }

    private static boolean dateCheck(String text) {
        if ((text.contains("/") || (text.contains(".")) || (text.contains("-")))
                && (text.contains("2") && text.contains("1"))
                && (text.length() <= 17)) {
            text = replace(text, "-", "/");
            text = replace(text, ".", "/");
            receiptDate = text;
            return true;
        } else return false;

    }

    private static boolean dateValidate(String date) {
        date=replace(date,"TARIH","");
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

        format.setLenient(false);

        try {
            format.parse(date);
            receiptDate = date;
            return true;
        } catch (ParseException e) {
            Timber.d("dateValidate: 1-parse exception");
        }
        format = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
        try {
            format.parse(date);
            receiptDate = date;
            return true;
        } catch (ParseException e) {
            Timber.d("dateValidate: 2-parse exception");
        }
        format = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
        try {
            format.parse(date);
            receiptDate = date;
            return true;
        } catch (ParseException e) {
            Timber.d("dateValidate: 2-parse exception");
        }
        return false;
    }

    private static boolean paymentTypeCheck(String text) {
        if (
                text.contains("KRED") || // kredi kartı
                        text.contains("KART") || // kredi kartı
                        text.contains("POS") || // kredi kartı (ORTAK POS) //TODO DOES NOT WORK
                        text.contains("NAK")) { // nakit
            paymentType = text;
            return true;
        } else return false;
    }

    private static boolean companyNameCheck(String text) {
        // Timber.d("companyNameCheck: text:"+text);
        if (text.contains("A101") ||
                text.contains("A01") ||
                text.contains("BIM") ||
                text.contains("BtM") ||
                text.contains("TAHTAKALE") ||
                text.contains("MANGAL") ||
                text.contains("PİY") || //TODO
                text.contains("CARREFOUR") || //TODO
                text.contains("LC") ||
                text.contains("ECZ") ||
                text.contains("BİM") ||
                text.contains("BiM") ||
                text.contains("MIGR") ||
                text.contains("ŞOK") ||
                text.contains("sOK") ||
                text.contains("şoK") ||
                text.contains("SOKM") ||
                (text.contains("SOK") && !text.contains("."))) { //todo consider capitalizing text before continuing
            //TODO ŞOK RECEİPTS NOT GETTING FROM FIRST HEADING - if I see Şok can just put Şok
            text = replace(text, "SOKMARKET", "ŞOK");
            text = replace(text, "COM.TR", "");
            text = replace(text, "BtM", "BİM");
            text = replace(text, "BIM", "BİM");
            text = replace(text, "BiM", "BİM");
            text = replace(text, "sOK", "ŞOK");
            text = replace(text, "SOK", "ŞOK");
            text = replace(text, "şoK", "ŞOK");
            text = replace(text, "A01", "A101");
            text = replace(text, ".", "");
            company = text;
            return true;
        } else return false;
    }

    private static boolean totalCheck(String text) {
        return (text.contains("TOP") && (!text.contains("KDV")));

//                (text.contains("UTARI"))); //*TODO Added for credit card processing but not very successful. Consider TL as alternative. get Y position then amount to the left
    }

    private static void findTotalAmount(List<FirebaseVisionText.Element> elements, List<FirebaseVisionText.Element> extractedElements) {
        // finds amount at same level as 'toplam' heading
        int tolerance = 10;
        for (int k = 0; k < elements.size(); k++) {
            FirebaseVisionText.Element element = elements.get(k);
//            Timber.d("findTotalAmount: " + element.getText());
            Point[] cornerPoints = element.getCornerPoints();
//            Timber.d("findTotalAmount: " + cornerPoints[0].y);
//            Timber.d("findTotalAmount: " + cornerPoints[1].y);
//           Timber.d("findTotalAmount: " + cornerPoints[2].y);
            int averageYPoint = (cornerPoints[0].y + cornerPoints[2].y) / 2;
            if (!element.getText().contains("TOP"))  // total)
            {
//                Timber.d("findTotalAmount: toplamYPosition1:" + toplamYPosition1);
//                Timber.d("findTotalAmount: toplamYPosition2:" + toplamYPosition2);
//                Timber.d("findTotalAmount: averageYPoint:" + averageYPoint);

                if ((toplamYPosition1 - tolerance < averageYPoint) && (averageYPoint < toplamYPosition2 + tolerance)) {
                    extractedElements.add(elements.get(k));
//                    Timber.d("findTotalAmount: set totalamount" + element.getText());
                    totalAmount = element.getText();
                }
            }
        }
    }

}

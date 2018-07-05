package com.douglasharvey.receipttracker.utilities;

import android.graphics.Point;

import com.douglasharvey.receipttracker.data.ReceiptResult;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ProcessTextRecognition {
    private static int toplamYPosition1;
    private static int toplamYPosition2;
    private static String totalAmount;
    private static String paymentType;
    private static String receiptDate;
    private static String company;

    public static List<FirebaseVisionText.Element> processTextRecognitionResult(FirebaseVisionText texts, ReceiptResult receiptResult) {
        toplamYPosition1 = 0;
        toplamYPosition2 = 0;
        totalAmount = null;
        paymentType = null;
        receiptDate = null;
        company = null;
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
                + "Payment Type: " + paymentType + "\n"
                + "Amount: " + totalAmount + "\n"
                + "Date: " + receiptDate + "\n"
        );
        receiptResult.setCompany(company);
        receiptResult.setAmount(totalAmount);
        receiptResult.setDate(receiptDate);
        receiptResult.setPaymentType(paymentType);
        return extractedElements; // also above items!

    }

    private static List<FirebaseVisionText.Element> filterElements(List<FirebaseVisionText.Element> elements) {
        boolean companyNameExists, paymentTypeExists, dateExists;
        List<FirebaseVisionText.Element> extractedElements = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            FirebaseVisionText.Element element = elements.get(i);

            companyNameExists = companyNameCheck(element.getText());
            paymentTypeExists = paymentTypeCheck(element.getText());
            dateExists = dateCheck(element.getText());

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
                            if (toplamYPosition1 == 0) toplamYPosition1 = cornerPoint.y;
                            else toplamYPosition2 = cornerPoint.y; // get y position from 2 corners
                        } else if (!element.getText().contains(",")) {
                            extractedElements.add(elements.get(i)); // add to output if not related to amount.
                        }
                    }
                }
            }
            //  extractedElements.add(elements.get(i));
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
                && (text.contains("2") && text.contains("1"))) {
            receiptDate = text;
            return true;
        } else return false;

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
        if (text.contains("A101") ||
                text.contains("BIM") ||
                text.contains("TAHTAKALE") ||
                text.contains("MANGAL") ||
                text.contains("PİY") || //TODO
                text.contains("CARREFOUR") || //TODO
                text.contains("LC") ||
                text.contains("ECZ") ||
                text.contains("BİM") ||
                text.contains("MIGR") ||
                text.contains("ŞOK") ||
                text.contains("SOK")) {
            company = text;
            return true;
        } else return false;
    }

    private static boolean totalCheck(String text) {
        // WHAT ABOUT T O P L A M
        return (text.contains("TOP") && (!text.contains("KDV")) ); // consider TUTAR for credit card receipts
    }

    private static void findTotalAmount(List<FirebaseVisionText.Element> elements, List<FirebaseVisionText.Element> extractedElements) {
        // finds amount at same level as 'toplam' heading
        int tolerance = 10;
        for (int k = 0; k < elements.size(); k++) {
            FirebaseVisionText.Element element = elements.get(k);
            Point[] cornerPoints = element.getCornerPoints();
            int averageYPoint = (cornerPoints[0].y + cornerPoints[2].y) / 2;
            if (!element.getText().contains("TOP"))  // total)
            {
                if ((toplamYPosition1 - tolerance < averageYPoint) && (averageYPoint < toplamYPosition2 + tolerance)) {
                    extractedElements.add(elements.get(k));
                    totalAmount = element.getText();
                }
            }
        }
    }

}

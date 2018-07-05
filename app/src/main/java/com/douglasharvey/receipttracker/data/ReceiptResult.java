package com.douglasharvey.receipttracker.data;

public class ReceiptResult {
    private String company;
    private String amount;
    private String date;
    private String paymentType;

    public void setCompany(String company) {
        this.company = company;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getCompany() {

        return company;
    }

    public String getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    public String getPaymentType() {
        return paymentType;
    }
}

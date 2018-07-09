package com.douglasharvey.receipttracker.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "receipt_table")
@TypeConverters(DateConverter.class)
public class Receipt {


    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;

    private String company;

    private float amount;

    @ColumnInfo(name = "receipt_date")
    private Date receiptDate;

    private String file;

    public Receipt() {
    }

    public Date getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(Date receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }


    public void setId(@NonNull int id) {
        this.id = id;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }


    public void setType(int type) {
        this.type = type;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private int type;

    private int category;

    @NonNull
    public int getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public float getAmount() {
        return amount;
    }

    public int getType() {
        return type;
    }

    public int getCategory() {
        return category;
    }

    public String getComment() {
        return comment;
    }

    private String comment;

}

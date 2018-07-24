package com.douglasharvey.receipttracker.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "receipt_table", indices = {@Index("receipt_date")})
@TypeConverters(DateConverter.class)
public class Receipt implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;

    private String company;

    private float amount;

    @ColumnInfo(name = "receipt_date")
    private Date receiptDate;

    private String file;

    private int type;

    private int category;

    private String comment;

    @ColumnInfo(name = "drive_id")
    private String driveID;
    @ColumnInfo(name = "web_link")
    private String webLink;

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


    public String getDriveID() {
        return driveID;
    }

    public void setDriveID(String driveID) {
        this.driveID = driveID;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    protected Receipt(Parcel in) {
        id = in.readInt();
        company = in.readString();
        amount = in.readFloat();
        long tmpReceiptDate = in.readLong();
        receiptDate = tmpReceiptDate != -1 ? new Date(tmpReceiptDate) : null;
        file = in.readString();
        type = in.readInt();
        category = in.readInt();
        comment = in.readString();
        driveID = in.readString();
        webLink = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(company);
        dest.writeFloat(amount);
        dest.writeLong(receiptDate != null ? receiptDate.getTime() : -1L);
        dest.writeString(file);
        dest.writeInt(type);
        dest.writeInt(category);
        dest.writeString(comment);
        dest.writeString(driveID);
        dest.writeString(webLink);
    }
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Receipt> CREATOR = new Parcelable.Creator<Receipt>() {
        @Override
        public Receipt createFromParcel(Parcel in) {
            return new Receipt(in);
        }

        @Override
        public Receipt[] newArray(int size) {
            return new Receipt[size];
        }
    };
}

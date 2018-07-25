package com.douglasharvey.receipttracker.utilities;


import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static String getTodaysDate() {

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today); // don't forget this if date is arbitrary
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH); // 0 being January
        int year = cal.get(Calendar.YEAR);

        return DateUtils.formatDate(year, month, dayOfMonth);

        //Read more: http://www.java67.com/2016/12/how-to-get-current-day-month-year-from-date-in-java8.html#ixzz5MGLwqgOf
    }

    public static String formatDate(int year, int month, int day) {
        return day + "/" + (month + 1) + "/" + year;
    }
}

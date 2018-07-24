package com.douglasharvey.receipttracker.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;

public class Migrations {
    static final Migration FROM_7_TO_8=new Migration(7,8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE receipt_table RENAME to receipt_table_old");
            //db.execSQL("DROP INDEX `index_receipt_table_receipt_date`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `receipt_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    " `company` TEXT, `amount` REAL NOT NULL, `receipt_date` INTEGER, `file` TEXT, `type` INTEGER NOT NULL," +
                    " `category` INTEGER NOT NULL, `comment` TEXT, `drive_id` TEXT, `web_link` TEXT)");
            db.execSQL("CREATE  INDEX `index_receipt_table_receipt_date` ON `receipt_table` (`receipt_date`)");
            db.execSQL("INSERT INTO receipt_table (id, company, amount, receipt_date, file, type, category, comment, drive_id, web_link)" +
                    "SELECT id, company, amount, receipt_date, file, type, category, comment, driveId, webLink FROM receipt_table_old");
            db.execSQL("DROP TABLE receipt_table_old");
        }
    };

}

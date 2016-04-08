package ru.mail.park.chat.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Михаил on 06.03.2016.
 */
public class MessengerDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "Messenger.db";

    public static final DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());

    public MessengerDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ChatsContract.CREATE_TABLE);
        db.execSQL(ContactsContract.CREATE_TABLE);
        db.execSQL(MessagesContract.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropDatabase(db);
        onCreate(db);
    }

    public void dropDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        dropDatabase(db);
    }

    private void dropDatabase(SQLiteDatabase db) {
        db.execSQL(ChatsContract.DROP_TABLE);
        db.execSQL(ContactsContract.DROP_TABLE);
        db.execSQL(MessagesContract.DROP_TABLE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

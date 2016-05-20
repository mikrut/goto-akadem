package ru.mail.park.chat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ru.mail.park.chat.models.Contact;

public class ContactsHelper {
    private final MessengerDBHelper dbHelper;

    public ContactsHelper(Context context) {
        dbHelper = new MessengerDBHelper(context);
    }

    public void updateContact(@NonNull Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        updateContact(contact, db);
    }

    private void updateContact(@NonNull Contact contact, SQLiteDatabase db) {
        ContentValues values = contact.getContentValues();
        String whereClause = ContactsContract.ContactsEntry.COLUMN_NAME_UID + " = ?";
        String[] whereArgs = {contact.getUid()};
        db.update(ContactsContract.ContactsEntry.TABLE_NAME, values, whereClause, whereArgs);
    }

    public long saveContact(@NonNull Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return saveContact(contact, db);
    }

    private long saveContact(@NonNull Contact contact, SQLiteDatabase db) {
        ContentValues values = contact.getContentValues();
        return db.insertWithOnConflict(ContactsContract.ContactsEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private long deleteAll(SQLiteDatabase db) {
        return db.delete(ContactsContract.ContactsEntry.TABLE_NAME, null, null);
    }

    public void updateContactsList(@NonNull List<Contact> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            deleteAll(db);
            for (Contact contact : contactList) {
                saveContact(contact, db);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    private Cursor getContactsCursor() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.query(
                ContactsContract.ContactsEntry.TABLE_NAME,
                ContactsContract.CONTACT_PROJECTION,
                null, // Return all chats (no WHERE)
                null, // No WHERE - no args
                null, // No GROUP BY
                null, // No GROUP BY filter
                null  // No ORDER BY
        );
    }

    @Nullable
    public Contact getContact(@NonNull String uid) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = ContactsContract.ContactsEntry.COLUMN_NAME_UID + " = ?";
        String[] selectionArgs = { uid };
        Cursor cursor = db.query(ContactsContract.ContactsEntry.TABLE_NAME,
                ContactsContract.CONTACT_PROJECTION,
                selection, selectionArgs,
                null, null, null,
                "1");

        Contact contact = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            contact = new Contact(cursor);
        }
        cursor.close();
        return contact;
    }

    @NonNull
    public List<Contact> getContactsList() {
        Cursor contactsCursor = getContactsCursor();
        ArrayList<Contact> chatsList = new ArrayList<>(contactsCursor.getCount());

        for(contactsCursor.moveToFirst(); !contactsCursor.isAfterLast(); contactsCursor.moveToNext()) {
            chatsList.add(new Contact(contactsCursor));
        }
        return chatsList;
    }

    public int deleteContact(@NonNull String cid) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = ContactsContract.ContactsEntry.COLUMN_NAME_UID + " = ?";
        String[] selectionArgs = { cid };
        return db.delete(ContactsContract.ContactsEntry.TABLE_NAME, selection, selectionArgs);
    }
}

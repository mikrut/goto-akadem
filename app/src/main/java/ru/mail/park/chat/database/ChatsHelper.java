package ru.mail.park.chat.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.mail.park.chat.models.Chat;
import ru.mail.park.chat.models.Contact;

/**
 * Created by Михаил on 06.03.2016.
 */
public class ChatsHelper {
    private final MessengerDBHelper dbHelper;

    public static final String LOG_TAG = "[TP-diploma]";

    public ChatsHelper(Context context) {
        dbHelper = new MessengerDBHelper(context);
    }

    public void updateChat(@NonNull Chat chat) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = chat.getContentValues();
        String whereClause = ChatsContract.ChatsEntry.COLUMN_NAME_CID + " = ?";
        String[] whereArgs = {chat.getCid()};
        db.update(ChatsContract.ChatsEntry.TABLE_NAME, values, whereClause, whereArgs);
    }

    public long saveChat(@NonNull Chat chat) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = chat.getContentValues();
        return db.insertWithOnConflict(ChatsContract.ChatsEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // FIXME: ORDER BY last_message_time DESC
    @NonNull
    private Cursor getChatsCursor(@NonNull String queryString) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        queryString = queryString.replace("\\", "\\\\");
        queryString = queryString.replace("%", "\\%");
        queryString = queryString.replace("_", "\\_");
        queryString = queryString.replace("[", "\\[");

        String[] selectionArgs = { queryString + "*", "%" + queryString + "%" };

        return db.rawQuery("SELECT * FROM " + ChatsContract.ChatsEntry.TABLE_NAME +
                " WHERE " + ChatsContract.ChatsEntry.COLUMN_NAME_CID + " IN (" +
                "SELECT " + MessagesContract.MessagesEntry.COLUMN_NAME_CID +
                " FROM " + MessagesContract.MessagesEntry.TABLE_NAME + " WHERE " + MessagesContract.MessagesEntry._ID + " IN (" +
                "SELECT docid FROM fts_" + MessagesContract.MessagesEntry.TABLE_NAME + " WHERE fts_" + MessagesContract.MessagesEntry.TABLE_NAME + " MATCH ?" +
                ")" +
                ") OR " +
                ChatsContract.ChatsEntry.COLUMN_NAME_NAME + " LIKE ? ESCAPE '\\'", selectionArgs);
    }

    // FIXME: ORDER BY last_message_time DESC
    @NonNull
    private Cursor getChatsCursor() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.query(
                ChatsContract.ChatsEntry.TABLE_NAME,
                ChatsContract.CHAT_PROJECTION,
                null, // Return all chats (no WHERE)
                null, // No WHERE - no args
                null, // No GROUP BY
                null, // No GROUP BY filter
                null  // No ORDER BY
        );
    }

    @Nullable
    public Chat getChat(@NonNull String cid) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = ChatsContract.ChatsEntry.COLUMN_NAME_CID + " = ?";
        String[] selectionArgs = { cid };
        Cursor cursor = db.query(ChatsContract.ChatsEntry.TABLE_NAME,
                ChatsContract.CHAT_PROJECTION,
                selection, selectionArgs,
                null, null, null,
                "1");

        Chat chat = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            chat = new Chat(cursor);
        }
        cursor.close();
        return chat;
    }

    @NonNull
    public List<Chat> getChatsList() {
        Cursor chatsCursor = getChatsCursor();
        return cursorToList(chatsCursor);
    }

    @NonNull
    public List<Chat> getChatsList(@NonNull String queryString) {
        Cursor chatsCursor = getChatsCursor(queryString);
        List<Chat> result = cursorToList(chatsCursor);
        chatsCursor.close();
        return result;
    }

    private static final List<Chat> cursorToList(Cursor chatsCursor) {
        ArrayList<Chat> chatsList = new ArrayList<>(chatsCursor.getCount());

        for(chatsCursor.moveToFirst(); !chatsCursor.isAfterLast(); chatsCursor.moveToNext()) {
            chatsList.add(new Chat(chatsCursor));
        }

        return chatsList;
    }

    public int deleteChat(@NonNull String cid) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = ChatsContract.ChatsEntry.COLUMN_NAME_CID + " = ?";
        String[] selectionArgs = { cid };
        return db.delete(ChatsContract.ChatsEntry.TABLE_NAME, selection, selectionArgs);
    }

    public void updateChatList(@NonNull List<Chat> chatList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "updateChatList");

        db.beginTransaction();
        try {
            Log.d(LOG_TAG, "try deleteAll");
            deleteAll(db);
            Log.d(LOG_TAG, "try to saveChat for " + chatList.size() + " elements");
            for (Chat chat : chatList) {
                saveChat(chat);
            }
            Log.d(LOG_TAG, "done");
            db.setTransactionSuccessful();
        //} catch() {
        //    Log.d(LOG_TAG, "exception caught");
        } finally {
            db.endTransaction();
        }
    }

    private long deleteAll(SQLiteDatabase db) {
        try {
            return db.delete(ChatsContract.ChatsEntry.TABLE_NAME, null, null);
        } catch (SQLiteException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
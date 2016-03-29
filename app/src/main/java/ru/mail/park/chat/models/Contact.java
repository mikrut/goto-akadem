package ru.mail.park.chat.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import ru.mail.park.chat.database.ContactsContract;
import ru.mail.park.chat.database.MessengerDBHelper;

/**
 * Created by Михаил on 08.03.2016.
 */

// TODO: implement firstname, lastname + stringification
// TODO: implement drawables
public class Contact implements Comparable<Contact> {
    private @NonNull String uid;
    private @NonNull String login;

    private @Nullable String email;
    private @Nullable String phone;
    private @Nullable String firstName;
    private @Nullable String lastName;
    private @Nullable Calendar lastSeen;

    private boolean online = false;

    public enum Relation {FRIEND, SELF, OTHER};

    protected Contact() {}

    public Contact(JSONObject contact) throws JSONException, ParseException {
        setUid(contact.getString("id"));
        setLogin(contact.getString("login"));

        if (contact.has("email"))
            setEmail(contact.getString("email"));
        if (contact.has("phone"))
            setPhone(contact.getString("phone"));
        if (contact.has("firstName"))
            setFirstName(contact.getString("firstName"));
        if (contact.has("lastName"))
            setFirstName(contact.getString("lastName"));

        if (contact.has("last_seen")) {
            java.util.Date dateLastSeen = MessengerDBHelper.iso8601.parse(contact.getString("last_seen"));
            GregorianCalendar lastSeen = new GregorianCalendar();
            lastSeen.setTime(dateLastSeen);
            setLastSeen(lastSeen);
        }

        if (contact.has("online")) {
            setOnline(contact.getBoolean("online"));
        }
    }

    public Contact(Cursor cursor) {
        uid = cursor.getString(ContactsContract.PROJECTION_UID_INDEX);
        login = cursor.getString(ContactsContract.PROJECTION_LOGIN_INDEX);

        email = cursor.getString(ContactsContract.PROJECTION_EMAIL_INDEX);
        phone = cursor.getString(ContactsContract.PROJECTION_PHONE_INDEX);
        firstName = cursor.getString(ContactsContract.PROJECTION_FIRST_NAME_INDEX);
        lastName = cursor.getString(ContactsContract.PROJECTION_LAST_NAME_INDEX);
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    @NonNull
    public String getLogin() {
        return login;
    }

    public void setLogin(@NonNull String login) {
        this.login = login;
    }

    @Nullable
    public String getPhone() {
        return phone;
    }

    public void setPhone(@Nullable String phone) {
        this.phone = phone;
    }

    public @Nullable Calendar getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(@Nullable Calendar lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public int compareTo(@NonNull Contact another) {
        return this.login.compareTo(another.getLogin());
    }
    

    public @Nullable String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@Nullable String firstName) {
        this.firstName = firstName;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@Nullable String lastName) {
        this.lastName = lastName;
    }

    @NonNull
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactsContract.ContactsEntry.COLUMN_NAME_UID, uid);
        contentValues.put(ContactsContract.ContactsEntry.COLUMN_NAME_LOGIN, login);

        contentValues.put(ContactsContract.ContactsEntry.COLUMN_NAME_EMAIL, email);
        contentValues.put(ContactsContract.ContactsEntry.COLUMN_NAME_PHONE, phone);
        contentValues.put(ContactsContract.ContactsEntry.COLUMN_NAME_FIRST_NAME, firstName);
        contentValues.put(ContactsContract.ContactsEntry.COLUMN_NAME_LAST_NAME, lastName);
        return contentValues;
    }

    @NonNull
    public String getContactTitle() {
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(firstName != null ? firstName : "");
        if (firstName != null && lastName != null)
            titleBuilder.append(" ");
        titleBuilder.append(lastName != null ? lastName : "");
        if (firstName == null && lastName == null) {
            return  getLogin();
        }
        return titleBuilder.toString();
    }
}

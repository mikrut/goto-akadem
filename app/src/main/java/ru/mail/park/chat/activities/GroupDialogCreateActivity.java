package ru.mail.park.chat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.adapters.AContactAdapter;
import ru.mail.park.chat.activities.adapters.ContactAdapter;
import ru.mail.park.chat.models.Contact;

/**
 * Created by mikrut on 01.04.16.
 */
public class GroupDialogCreateActivity extends ContactsActivity {
    private EditText choosenContactsList;
    private TreeSet<Contact> choosenContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        choosenContactsList = (EditText) findViewById(R.id.choosen_contacts_list);
        choosenContacts = new TreeSet<>();
    }

    @Override
    protected void onSetContentView() {
        setContentView(R.layout.activity_group_dialog_create);
    }

    @Override
    protected AContactAdapter onCreateContactAdapter(@NonNull List<Contact> data) {
        return new ContactAdapter(data, new AContactAdapter.ContactHolder.OnContactClickListener() {
            @Override
            public void onContactClick(View contactView, AContactAdapter.ContactHolder viewHolder) {
                Contact contact = viewHolder.getContact();
                if (choosenContacts.contains(contact)) {
                    choosenContacts.remove(contact);
                } else {
                    choosenContacts.add(contact);
                }
                rebuildContactsList();
            }
        });
    }

    private void rebuildContactsList() {
        StringBuilder builder = new StringBuilder();
        Iterator<Contact> contactIterator = choosenContacts.iterator();
        for (int i = 0; i < choosenContacts.size(); i++) {
            Contact currentContact = contactIterator.next();
            builder.append(currentContact.getContactTitle());
            if (i != choosenContacts.size() - 1) {
                builder.append(", ");
            }
        }
        choosenContactsList.setText(builder.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_dialog_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create:
                Intent intent = new Intent(this, DialogActivity.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

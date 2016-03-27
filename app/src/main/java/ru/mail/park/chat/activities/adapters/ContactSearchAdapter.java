package ru.mail.park.chat.activities.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.Collections;
import java.util.List;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.tasks.AddContactTask;
import ru.mail.park.chat.models.Contact;

/**
 * Created by Михаил on 12.03.2016.
 */
public class ContactSearchAdapter extends AContactAdapter {
    List<Contact> contactList;

    public static class ContactSearchHolder extends ContactHolder {
        protected ImageButton addFriendImage;

        public ContactSearchHolder(final View itemView) {
            super(itemView);
            addFriendImage = (ImageButton) itemView.findViewById(R.id.addFriendImage);
            
            addFriendImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v("add frend", "clicked");
                    new AddContactTask(itemView.getContext()).execute(uid);
                }
            });
        }
    }

    public ContactSearchAdapter(@NonNull List<Contact> contactList) {
        this.contactList = contactList;
        Collections.sort(contactList);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contactView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.element_contacts_search, parent, false);
        return new ContactSearchHolder(contactView);
    }

    @Override
    protected Contact getContactForPosition(int position) {
        return contactList.get(position);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }
}

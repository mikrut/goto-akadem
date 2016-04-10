package ru.mail.park.chat.activities.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.UserProfileActivity;
import ru.mail.park.chat.activities.views.TitledPicturedViewHolder;
import ru.mail.park.chat.models.Contact;

/**
 * Created by Михаил on 12.03.2016.
 */
public abstract class AContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final int CONTACT = 1;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contactView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.element_contact, parent, false);
        return new ContactHolder(contactView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ContactHolder contactHolder = (ContactHolder) holder;
        Contact contact = getContactForPosition(position);
        contactHolder.setTitle(contact.getContactTitle());
        contactHolder.setUid(contact.getUid());

        Calendar lastSeen = contact.getLastSeen();
        if (lastSeen != null) {
            contactHolder.setContactLastSeen(lastSeen.getTime().toGMTString());
        } else {
            contactHolder.setContactLastSeen(contact.isOnline() ? "Online" : "Offline");
        }

        contactHolder.setContact(contact);
    }

    protected abstract Contact getContactForPosition(int position);

    @Override
    public int getItemViewType(int position) {
        return CONTACT;
    }

    public static class ContactHolder extends TitledPicturedViewHolder {
        final ImageView contactImage;
        final TextView contactName;
        final TextView contactLastSeen;

        String uid;
        Contact contact;

        public ContactHolder(View itemView) {
            super(itemView);
            contactImage = (ImageView) itemView.findViewById(R.id.image);
            contactName = (TextView) itemView.findViewById(R.id.contactName);
            contactLastSeen = (TextView) itemView.findViewById(R.id.contactLastSeen);

            setOnContactClickListener(new OnContactClickListener() {
                @Override
                public void onContactClick(View v, ContactHolder holder) {
                    Intent intent = new Intent(v.getContext(), UserProfileActivity.class);
                    intent.putExtra(UserProfileActivity.UID_EXTRA, uid);
                    v.getContext().startActivity(intent);
                }
            });
        }

        public void setContact(Contact contact) {
            this.contact = contact;
        }

        public Contact getContact() {
            return contact;
        }

        public void setUid(String uid) {this.uid = uid;}

        public void setContactImage(Bitmap bitmap) {
            contactImage.setImageBitmap(bitmap);
        }

        @Override
        public void setTitle(String name) {
            super.setTitle(name);
            contactName.setText(name);
        }

        public void setContactLastSeen(String lastSeen) {
            contactLastSeen.setText(lastSeen);
        }

        public void setOnContactClickListener(final OnContactClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onContactClick(v, ContactHolder.this);
                }
            });
        }

        public interface OnContactClickListener {
            void onContactClick(View contactView, ContactHolder viewHolder);
        }
    }
}

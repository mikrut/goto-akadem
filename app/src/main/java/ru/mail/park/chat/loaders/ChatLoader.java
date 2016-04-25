package ru.mail.park.chat.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import ru.mail.park.chat.activities.ChatsActivity;
import ru.mail.park.chat.database.ChatHelper;
import ru.mail.park.chat.models.Chat;

/**
 * Created by Михаил on 06.03.2016.
 */
// TODO: implement limit, start number etc.
public class ChatLoader extends AsyncTaskLoader<List<Chat>> {
    List<Chat> chats;

    public ChatLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    public List<Chat> loadInBackground() {
        ChatHelper chatHelper = new ChatHelper(getContext());
        chats = chatHelper.getChatsList();
        return chats;
    }

    @Override
    protected void onStartLoading() {
        if (chats != null) {
            deliverResult(chats);
        } else {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        chats = null;
    }

    @Override
    public int getId() {
        return ChatsActivity.CHAT_DB_LOADER;
    }
}

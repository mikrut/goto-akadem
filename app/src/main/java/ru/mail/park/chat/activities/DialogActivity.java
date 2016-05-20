package ru.mail.park.chat.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.rockerhieu.emojicon.EmojiconEditText;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import de.hdodenhof.circleimageview.CircleImageView;
import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.adapters.FilesAdapter;
import ru.mail.park.chat.activities.adapters.MessagesAdapter;
import ru.mail.park.chat.activities.interfaces.IActionBarListener;
import ru.mail.park.chat.activities.views.KeyboardDetectingLinearLayout;
import ru.mail.park.chat.api.Chats;
import ru.mail.park.chat.api.ChatInfo;
import ru.mail.park.chat.api.HttpFileUpload;
import ru.mail.park.chat.api.Messages;
import ru.mail.park.chat.database.ChatHelper;
import ru.mail.park.chat.database.ContactHelper;
import ru.mail.park.chat.file_dialog.FileDialog;
import ru.mail.park.chat.loaders.MessagesDBLoader;
import ru.mail.park.chat.loaders.MessagesLoader;
import ru.mail.park.chat.message_interfaces.IChatListener;
import ru.mail.park.chat.message_interfaces.IMessageSender;
import ru.mail.park.chat.models.AttachedFile;
import ru.mail.park.chat.models.Chat;
import ru.mail.park.chat.models.Contact;
import ru.mail.park.chat.models.Message;
import ru.mail.park.chat.models.OwnerProfile;

// TODO: emoticons
// TODO: send message
public class DialogActivity
        extends AppCompatActivity
        implements IChatListener,
        EmojiconGridFragment.OnEmojiconClickedListener,
        EmojiconsFragment.OnEmojiconBackspaceClickedListener,
        HttpFileUpload.IUploadListener,
        IActionBarListener {
    public static final String CHAT_ID = DialogActivity.class.getCanonicalName() + ".CHAT_ID";
    private static final int CODE_FILE_SELECTED = 3;
    public static final String SERVER_URL = "http://p30480.lab1.stud.tech-mail.ru/";
    private static final String FILE_UPLOAD_URL = "http://p30480.lab1.stud.tech-mail.ru/file/upload";
    public static final String USER_ID = DialogActivity.class.getCanonicalName() + ".USER_ID";

    private KeyboardDetectingLinearLayout globalLayout;
    private FrameLayout emojicons;
    private RecyclerView messagesList;
    private ImageButton insertEmoticon, attachFile;
    private EmojiconEditText inputMessage;
    private ImageButton sendMessage;
    private RecyclerView attachments;
    private ImageButton buttonDown;
    private ActionBar mActionBar;

    private String chatID;
    private String userID;
    private String ownerID;
    private String accessToken;
    private ChatInfo chatInfo;
    private Chat thisChat;

    private Timer schedulerTimer;
    private final Handler uiHandler = new Handler();
    private static final long WRITER_DISAPPEAR_DELAY_MILLIS = 2000;
    private static final long RETRY_DELAY_MILLIS = 5000;

    private List<Pair<Long, Contact>> writers;
    private static final int MAX_UNDELIVERED_MESSAGES = 50;
    private BlockingQueue<Message> undeliveredMessages = new ArrayBlockingQueue<Message>(MAX_UNDELIVERED_MESSAGES);

    private List<Message> receivedMessageList;
    private List<AttachedFile> attachemtsList;
    private MessagesAdapter messagesAdapter;
    private LinearLayoutManager layoutManager;
    protected IMessageSender messages;

    private boolean receivedFromWeb = false;

    private boolean isEmojiFragmentShown = false;
    private boolean isSoftKeyboardShown = false;

    public static final int MESSAGES_DB_LOADER = 0;
    public static final int MESSAGES_WEB_LOADER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        initViews();
        initAttachments();

        try {
            messages = getMessageSender();
        } catch (IOException e) {
            e.printStackTrace();
        }

        chatID = getIntent().getStringExtra(CHAT_ID);
        userID = getIntent().getStringExtra(USER_ID);

        OwnerProfile ownerProfile = new OwnerProfile(this);
        ownerID = ownerProfile.getUid();
        accessToken = ownerProfile.getAuthToken();
        chatInfo = null;
        thisChat = null;

        initMessagesList();
        initActionListeners();

        setEmojiconFragment(false);

        if (chatID != null) {
            Log.d("[TP-diploma]", "calling onUpdateChatID");
            onUpdateChatID();
        }

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.actionbar_dialog, null);
        mCustomView.findViewById(R.id.small_dialog_user_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);
        new InitActionBarTask(this,  mActionBar, chatID).execute();
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dialog, menu);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        initWriters();
        initRetryTimeout();

        ChatHelper ch = new ChatHelper(this);
        thisChat = thisChat == null ? ch.getChat(chatID) : thisChat;

        if(thisChat != null && chatInfo != null) {
            onLoadInfoCompleted(mActionBar, chatInfo);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.actionbar_dialog, null);
        mCustomView.findViewById(R.id.small_dialog_user_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mActionBar.setCustomView(mCustomView);
    }

    private void initViews() {
        globalLayout = (KeyboardDetectingLinearLayout) findViewById(R.id.main);
        messagesList = (RecyclerView) findViewById(R.id.messagesList);
        insertEmoticon = (ImageButton) findViewById(R.id.insertEmoticon);
        attachFile = (ImageButton) findViewById(R.id.attachFile);
        inputMessage = (EmojiconEditText) findViewById(R.id.inputMessage);
        sendMessage = (ImageButton) findViewById(R.id.sendMessage);
        emojicons = (FrameLayout) findViewById(R.id.emojicons);
        attachments = (RecyclerView) findViewById(R.id.attachments_recycler_view);
        buttonDown = (ImageButton) findViewById(R.id.buttonDown);

        globalLayout.setOnKeyboardEventListener(new KeyboardDetectingLinearLayout.OnKeyboardEventListener() {
            @Override
            public void onSoftKeyboardShown() {
                isSoftKeyboardShown = true;
                isEmojiFragmentShown = false;
                emojicons.setVisibility(isEmojiFragmentShown ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onSoftKeyboardHidden() {
                isSoftKeyboardShown = false;
                emojicons.setVisibility(isEmojiFragmentShown ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void initAttachments() {
        attachemtsList = new ArrayList<>();
        FilesAdapter filesAdapter = new FilesAdapter(attachemtsList);
        attachments.setAdapter(filesAdapter);
        LinearLayoutManager attachmentsManager = new LinearLayoutManager(this);
        attachmentsManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        attachments.setLayoutManager(attachmentsManager);
        attachments.setVisibility(View.GONE);
    }

    // FIXME: possible multithreading bugs
    private void initWriters() {
        writers = new LinkedList<>();
        final TextView writersView = (TextView) findViewById(R.id.writersTextView);

        schedulerTimer = new Timer();
        schedulerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final long currentTimeMillis = System.currentTimeMillis();
                Iterator<Pair<Long, Contact>> i = writers.iterator();
                while (i.hasNext()) {
                    Pair<Long, Contact> current = i.next();
                    if (Math.abs(current.first - currentTimeMillis) > WRITER_DISAPPEAR_DELAY_MILLIS)
                        i.remove();
                }

                String writersString = null;
                if (writers.size() > 0) {
                    writersString = "User%s %s writes a message...";
                    String writersConcatenated = "";
                    for (Pair<Long, Contact> writer : writers) {
                        writersConcatenated = writersConcatenated +
                                writer.second.getContactTitle() + ", ";
                    }
                    writersConcatenated =
                            writersConcatenated.substring(0, writersConcatenated.length() - 2);
                    writersString = String.format(writersString,
                            writers.size() > 1 ? "s" : "",
                            writersConcatenated);
                }
                final String resultString = writersString;
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (writersView != null) {
                            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) messagesList.getLayoutParams();
                            int bottomMargin = 0;
                            if (resultString != null) {
                                writersView.setText(resultString);
                                writersView.setVisibility(View.VISIBLE);
                                bottomMargin = writersView.getHeight();
                            } else {
                                writersView.setVisibility(View.GONE);
                            }
                            layoutParams.setMargins(layoutParams.leftMargin,
                                    layoutParams.topMargin, layoutParams.rightMargin, bottomMargin);
                        }
                    }
                });
            }
        }, WRITER_DISAPPEAR_DELAY_MILLIS, WRITER_DISAPPEAR_DELAY_MILLIS);
    }

    private void initRetryTimeout() {
        schedulerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (chatID != null && messages instanceof Messages && !((Messages) messages).isStateOK()) {
                            Bundle args = new Bundle();
                            args.putString(MessagesLoader.CID_ARG, chatID);
                            getLoaderManager().restartLoader(MESSAGES_WEB_LOADER, args, listener).forceLoad();
                        }

                        if (undeliveredMessages.size() == 0)
                            messages.reconnect();

                        for (Message message : undeliveredMessages) {
                            sendMessage(message);
                        }
                    }
                });
            }
        }, RETRY_DELAY_MILLIS, RETRY_DELAY_MILLIS);
    }

    private void initMessagesList() {
        receivedMessageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(receivedMessageList, ownerID);
        messagesList.setAdapter(messagesAdapter);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesList.setLayoutManager(layoutManager);

        messagesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean atBottom =
                        (layoutManager.findLastVisibleItemPosition() == receivedMessageList.size() - 1);
                if (atBottom) {
                    buttonDown.setVisibility(View.GONE);
                }
            }
        });


    }

    private void initActionListeners() {
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chatID != null)
                    messages.write(chatID);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messagesList.scrollToPosition(receivedMessageList.size() - 1);
            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        insertEmoticon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSoftKeyboardShown) {
                    isEmojiFragmentShown = !isEmojiFragmentShown;
                    emojicons.setVisibility(isEmojiFragmentShown ? View.VISIBLE : View.GONE);
                } else {
                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    isEmojiFragmentShown = true;
                }
            }
        });

        attachFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), FileDialog.class);
                intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory());
                intent.putExtra(FileDialog.CAN_SELECT_DIR, false);

                startActivityForResult(intent, CODE_FILE_SELECTED);
            }
        });
    }

    protected IMessageSender getMessageSender() throws IOException {
        Messages messages = new Messages(this);
        messages.setChatListener(this);
        return messages;
    }

    private void onUpdateChatID() {
        Bundle args = new Bundle();
        args.putString(MessagesLoader.CID_ARG, chatID);
        args.putString(MessagesLoader.UID_ARG, userID);
        getLoaderManager().initLoader(MESSAGES_DB_LOADER, args, listener);
    }

    private void sendMessage() {
        final String messageBody = inputMessage.getText().toString();
        if (!messageBody.equals("") || attachemtsList.size() > 0) {
            Message message = new Message(messageBody, this);
            message.setFiles(attachemtsList);
            sendMessage(message);

            initAttachments();
            inputMessage.setText("");
        }
    }

    protected void sendMessage(@NonNull Message message) {
        undeliveredMessages.add(message);
        addMessage(message);
        if (chatID != null) {
            messages.sendMessage(chatID, message);
        } else if (userID != null) {
            messages.sendFirstMessage(userID, message);
        }
    }

    private void addMessage(@NonNull Message message) {
        boolean atBottom =
                (layoutManager.findLastVisibleItemPosition() == receivedMessageList.size() - 1);

        boolean inserted = false;
        boolean isDifferent = true;
        for (int position = 0; position < receivedMessageList.size() && !inserted; position++) {
            int comp = message.compareTo(receivedMessageList.get(position));
            if (comp < 0) {
                receivedMessageList.add(position, message);
                messagesAdapter.notifyItemInserted(position);
                inserted = true;
            } else if (comp == 0) {
                Message mess = receivedMessageList.get(position);
                undeliveredMessages.remove(mess);
                if (mess.getMessageBody().equals(message.getMessageBody())) {
                    isDifferent = false;
                }
                receivedMessageList.set(position, message);
                messagesAdapter.notifyItemChanged(position);
                inserted = true;
            }
        }

        if (!inserted) {
            receivedMessageList.add(message);
            messagesAdapter.notifyItemInserted(receivedMessageList.size() - 1);
        }

        if (atBottom) {
            messagesList.scrollToPosition(receivedMessageList.size() - 1);
        } else if (isDifferent) {
            buttonDown.setVisibility(View.VISIBLE);
        }
    }

    private void removeMessage(@NonNull String messageID) {
        for (int position = 0; position < receivedMessageList.size(); position++) {
            Message message = receivedMessageList.get(position);
            if (message.getMid() != null && message.getMid().equals(messageID)) {
                receivedMessageList.remove(position);
                messagesAdapter.notifyItemRemoved(position);
                return;
            }
        }
    }

    @Nullable
    private Message dispatchNewMessage(JSONObject message) {
        try {
            String newChatID = null;
            if (message.has("idRoom")) {
                newChatID = message.getString("idRoom");
                if (chatID == null) {
                    chatID = newChatID;
                    onUpdateChatID();
                }
            }

            Message incomeMsg = new Message(message, this, chatID);

            if (newChatID != null && chatID != null && !chatID.equals(newChatID)) {
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);

                notificationBuilder.setSmallIcon(R.drawable.ic_message_black_24dp)
                        .setContentTitle(incomeMsg.getTitle())
                        .setContentText(incomeMsg.getMessageBody());

                Intent intent = new Intent(this, DialogActivity.class);
                intent.putExtra(CHAT_ID, newChatID);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder.setContentIntent(pendingIntent);

                Notification notification = notificationBuilder.build();
                notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(0, notification);
            } else {
                return incomeMsg;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onIncomeMessage(JSONObject message){
        Message incomeMsg = dispatchNewMessage(message);
        if (incomeMsg != null)
            addMessage(incomeMsg);
    }

    @Override
    public void onActionDeleteMessage(int mid) {
        removeMessage(String.valueOf(mid));
    }

    @Override
    public void onAcknowledgeSendMessage(JSONObject msg){
        try {
            Message incomeMsg = dispatchNewMessage(msg);
            if (incomeMsg != null)
                addMessage(incomeMsg);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGetHistoryMessages(ArrayList<Message> msg_list) {
        receivedMessageList.clear();
        receivedMessageList.addAll(msg_list);
        Collections.sort(receivedMessageList);

        messagesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onWrite(String cid, Contact user) {
        if (ObjectUtils.compare(cid, chatID) == 0) {
            for (int i = 0; i < writers.size(); i++) {
                if (writers.get(i).second.equals(user)) {
                    writers.set(i, new Pair<>(System.currentTimeMillis(), user));
                    return;
                }
            }
            writers.add(new Pair<>(System.currentTimeMillis(), user));
        }
    }

    private void setEmojiconFragment(boolean useSystemDefault) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.emojicons, EmojiconsFragment.newInstance(useSystemDefault))
                .commit();
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(inputMessage);
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(inputMessage, emojicon);
    }

    @Override
    public void onUploadComplete(AttachedFile file) {
        attachemtsList.add(file);
        attachments.getAdapter().notifyItemInserted(attachemtsList.size() - 1);
        attachments.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoadInfoCompleted(ActionBar mActionBar, ChatInfo chatInfo) {
        CircleImageView smallUserPic;
        TextView dialogTitle;
        TextView dialogLastSeen;

        smallUserPic = (CircleImageView) mActionBar.getCustomView().findViewById(R.id.CircularImageView1);
        dialogTitle = (TextView) mActionBar.getCustomView().findViewById(R.id.dialog_title);
        dialogLastSeen = (TextView) mActionBar.getCustomView().findViewById(R.id.dialog_last_seen);

        Log.d("[TP-diploma]", "onLoadInfoCompleted step I");

        if(chatInfo == null)
            return;

        Log.d("[TP-diploma]", "onLoadInfoCompleted step II");

        Contact companion = chatInfo.getFirst();
        if(companion != null && companion.getUid().equals(ownerID)) {
            companion = chatInfo.getSecond();

            if(companion.getUid().equals(ownerID)) {
                companion = null;
            }
        }

        if(companion == null)
            return;

        final Contact finalComp = companion;

        Log.d("[TP-diploma]", "onLoadInfoCompleted step III");

//        companion = new ContactHelper(this).getContact(companion.getUid());

        dialogTitle.setText(companion.getContactTitle());
//        if (companion.isOnline())
//            dialogLastSeen.setText("online");
//        else if(companion.getLastSeen() != null)
//            dialogLastSeen.setText(companion.getLastSeen().getTime().toGMTString());
//        else
//            dialogLastSeen.setText("offline");

        String filePath = Environment.getExternalStorageDirectory() + "/torchat/avatars/users/" + companion.getUid() + ".bmp";
        File file = new File(filePath);

        if(file.exists()) {
            smallUserPic.setImageBitmap(BitmapFactory.decodeFile(filePath));
        }

        mActionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DialogActivity.this, ProfileViewActivity.class);
                intent.putExtra(ProfileViewActivity.UID_EXTRA, finalComp.getUid());
                startActivity(intent);
                finish();
            }
        });

        ChatHelper ch = new ChatHelper(DialogActivity.this);
        Chat currentChat = ch.getChat(chatID);

        try {
            currentChat.setCompanionId(companion.getUid());
            ch.saveChat(currentChat);
        } catch(NullPointerException e) {

        }

        Log.d("[TP-diploma]", "onLoadInfoCompleted done");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (messages != null) {
            messages.disconnect();
        }
        schedulerTimer.cancel();
    }

    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        Log.d("[TP-diploma]", "preparing to send file");
        if (resultCode == Activity.RESULT_OK) {
            String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
            FileInputStream fstrm = null;
            HttpFileUpload hfu = null;

            if (requestCode == CODE_FILE_SELECTED) {
                Log.d("[TP-diploma]", "sending file started");
                try {
                    fstrm = new FileInputStream(filePath);
                    hfu = new HttpFileUpload(FILE_UPLOAD_URL, filePath.substring(filePath.lastIndexOf('/'), filePath.length()), accessToken);
                    hfu.Send_Now(fstrm, this);
                } catch(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final LoaderManager.LoaderCallbacks<List<Message>> listener = new LoaderManager.LoaderCallbacks<List<Message>>() {
        @Override
        public Loader<List<Message>> onCreateLoader(int id, Bundle args) {
            Log.d("[TP-diploma]", "creating MessagesListener");
            switch (id) {
                case MESSAGES_WEB_LOADER:
                    return new MessagesLoader(DialogActivity.this, args);
                case MESSAGES_DB_LOADER:
                default:
                    return new MessagesDBLoader(DialogActivity.this, args);
            }
        }

        @Override
        public void onLoadFinished(Loader<List<Message>> loader, List<Message> data) {
            if (data != null) {
                Log.d("[TP-diploma]", "messages count: " + data.size());
                for (Message message : data) {
                    addMessage(message);
                }
                if (!receivedFromWeb)
                    messagesList.scrollToPosition(receivedMessageList.size() - 1);
                if (loader.getId() == MESSAGES_WEB_LOADER) {
                    receivedFromWeb = true;
                }
            } else {
                Log.d("[TP-diploma]", "empty list");
            }

            if (loader.getId() == MESSAGES_DB_LOADER) {
                Bundle args = new Bundle();
                args.putString(MessagesLoader.CID_ARG, chatID);
                getLoaderManager().restartLoader(MESSAGES_WEB_LOADER, args, this);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Message>> loader) {
            // TODO: something
        }
    };

    private class InitActionBarTask extends AsyncTask<Void, Void, ChatInfo> {
        IActionBarListener listener;
        ActionBar mActionBar;
        String chatID;

        public InitActionBarTask(IActionBarListener listener, ActionBar mActionBar, String chatID) {
            this.listener = listener;
            this.mActionBar = mActionBar;
            this.chatID = chatID;
        }

        protected ChatInfo doInBackground(Void... urls) {
            Chats chatsAPI = new Chats(DialogActivity.this);
            Log.d("[TP-diploma]", "InitActionBarTask started");

            try {
                chatInfo = chatsAPI.getChatInfo(chatID);
            } catch (IOException e) {
                Log.d("[TP-diploma]", "getChatInfo Exception:" + e.getMessage());
                return null;
            }

            try {
                checkUserData(chatInfo.getFirst());
                checkUserData(chatInfo.getSecond());
            } catch(IOException e) {
                Log.d("[TP-diploma]", "CheckUserData Exception:" + e.getMessage());
                return null;
            }

            return chatInfo;
        }

        protected void onPostExecute(ChatInfo chatInfo) {
            listener.onLoadInfoCompleted(mActionBar, chatInfo);
        }
    }

    private void checkUserData(Contact user) throws IOException {
        if(user == null)
            return;

        ContactHelper contactHelper = new ContactHelper(DialogActivity.this);
        Contact userFromBase = contactHelper.getContact(user.getUid());

        if(userFromBase != null && !user.getImg().equals("false")) {
            String requestPath = SERVER_URL + user.getImg();
            String localPath = Environment.getExternalStorageDirectory() + "/torchat/avatars/users/" + user.getUid() + ".bmp";

            File file = new File(localPath);

            if(!file.exists()) {
                InputStream in = new java.net.URL(requestPath).openStream();
                Bitmap bm = BitmapFactory.decodeStream(in);

                FileOutputStream fos = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();
            }

        }
    }
}

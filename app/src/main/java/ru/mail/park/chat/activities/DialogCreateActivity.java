package ru.mail.park.chat.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeSet;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.fragments.ContactsFragment;
import ru.mail.park.chat.activities.fragments.FSM;
import ru.mail.park.chat.database.ContactsToChatsHelper;
import ru.mail.park.chat.models.Chat;
import ru.mail.park.chat.models.Contact;

/**
 * Created by Михаил on 24.04.2016.
 */
public class DialogCreateActivity
        extends AppCompatActivity
        implements ContactsFragment.OnPickEventListener {
    TextView newGroupClickable;
    TextView newP2PClickable;
    EditText onionAddress;
    MenuItem p2pMenuItem;

    private DialogCreateFSM fsm = new DialogCreateFSM();
    private Contact choosenContact = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_create);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(fsm.createListener(UIEvent.BACK_PRESSED));


        newGroupClickable = (TextView) findViewById(R.id.new_group_dialog);
        newGroupClickable.setOnClickListener(fsm.createListener(UIEvent.NEW_GROUP_PRESSED));

        newP2PClickable = (TextView) findViewById(R.id.new_p2p_dialog);
        newP2PClickable.setOnClickListener(fsm.createListener(UIEvent.NEW_P2P_CHAT_PRESSED));

        onionAddress = (EditText) findViewById(R.id.onion_address);
    }

    @Override
    public void onBackPressed() {
        fsm.handleEvent(UIEvent.BACK_PRESSED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dialog_create, menu);
        p2pMenuItem = menu.findItem(R.id.action_create_p2p);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_p2p:
                fsm.handleEvent(UIEvent.P2P_CREATE_PRESSED);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (fsm.getCurrentState().equals(UIState.CHOOSE_DIALOG_TYPE)) {
            p2pMenuItem.setVisible(false);
        } else if (fsm.getCurrentState().equals(UIState.CREATE_P2P)) {
            p2pMenuItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onContactSetChanged(TreeSet<Contact> chosenContacts) {

    }

    @Override
    public void onContactClicked(Contact contact) {
        choosenContact = contact;
        fsm.handleEvent(UIEvent.CONTACT_PRESSED);
    }

    enum UIState {
        CHOOSE_DIALOG_TYPE,
        CREATE_P2P,
        OUT_BACK,
        OUT_NEW_GROUP,
        OUT_P2P,
        OUT_DIALOG
    }

    enum UIEvent {
        NEW_GROUP_PRESSED,
        NEW_P2P_CHAT_PRESSED,
        BACK_PRESSED,
        P2P_CREATE_PRESSED,
        CONTACT_PRESSED,
        VALIDATION_FAILED
    }

    private class DialogCreateFSM extends FSM<UIEvent, UIState> {
        public DialogCreateFSM() {
            super(UIState.CHOOSE_DIALOG_TYPE);
        }

        @Override
        protected UIState nextState(UIEvent uiEvent, UIState currentState) {
            switch (uiEvent) {
                case NEW_GROUP_PRESSED:
                    return UIState.OUT_NEW_GROUP;
                case NEW_P2P_CHAT_PRESSED:
                    return UIState.CREATE_P2P;
                case BACK_PRESSED:
                    switch (currentState) {
                        case CREATE_P2P:
                            return UIState.CHOOSE_DIALOG_TYPE;
                        default:
                            return UIState.OUT_BACK;
                    }
                case P2P_CREATE_PRESSED:
                    return UIState.OUT_P2P;
                case CONTACT_PRESSED:
                    switch (currentState) {
                        case CREATE_P2P:
                            return UIState.OUT_P2P;
                        case CHOOSE_DIALOG_TYPE:
                            return UIState.OUT_DIALOG;
                        default:
                            return currentState;
                    }
                case VALIDATION_FAILED:
                    switch (currentState) {
                        case OUT_P2P:
                            return UIState.CREATE_P2P;
                        default:
                            return currentState;
                    }
                default:
                    return currentState;
            }
        }
    }

    FSMListener listener = new FSMListener();

    private class FSMListener implements DialogCreateFSM.FSMListener<UIState> {
        public FSMListener() {
            fsm.setListener(this);
        }

        @Override
        public void onStateChange(UIState newState) {
            switch (newState) {
                case CHOOSE_DIALOG_TYPE:
                    newGroupClickable.setVisibility(View.VISIBLE);
                    newP2PClickable.setVisibility(View.VISIBLE);
                    onionAddress.setVisibility(View.GONE);
                    supportInvalidateOptionsMenu();
                    break;
                case CREATE_P2P:
                    newGroupClickable.setVisibility(View.GONE);
                    newP2PClickable.setVisibility(View.GONE);
                    onionAddress.setVisibility(View.VISIBLE);
                    onionAddress.setText("");
                    supportInvalidateOptionsMenu();
                    break;
                case OUT_BACK:
                    DialogCreateActivity.super.onBackPressed();
                    break;
                case OUT_NEW_GROUP:
                    Intent newGroupIntent = new Intent(DialogCreateActivity.this, GroupDialogCreateActivity.class);
                    startActivity(newGroupIntent);
                    finish();
                    break;
                case OUT_P2P:
                    String onionURL = onionAddress.getText().toString();
                    if (onionURL.equals("") && choosenContact != null && choosenContact.getOnionAddress() != null) {
                        onionURL = choosenContact.getOnionAddress().getHost();
                    }
                    if (onionURL.matches("[a-zA-Z\\d]{1,50}\u002Eonion")) {
                        Intent p2pIntent = new Intent(DialogCreateActivity.this, P2PDialogActivity.class);
                        p2pIntent.putExtra(P2PDialogActivity.HOST_ARG, onionURL);
                        p2pIntent.putExtra(P2PDialogActivity.PORT_ARG, P2PDialogActivity.LISTENER_DEFAULT_PORT);
                        startActivity(p2pIntent);
                        finish();
                    } else if (choosenContact == null && onionURL.equals("")) {
                        Intent p2pIntent = new Intent(DialogCreateActivity.this, P2PDialogActivity.class);
                        startActivity(p2pIntent);
                        finish();
                    } else {
                        Toast.makeText(DialogCreateActivity.this, "Invalid onion address", Toast.LENGTH_SHORT).show();
                        fsm.handleEvent(UIEvent.VALIDATION_FAILED);
                    }
                    choosenContact = null;

                    break;
                case OUT_DIALOG:
                    if (choosenContact != null) {
                        Intent intent = new Intent(DialogCreateActivity.this, DialogActivity.class);
                        ContactsToChatsHelper helper = new ContactsToChatsHelper(DialogCreateActivity.this);
                        Chat chat = helper.getChat(choosenContact.getUid());
                        if (chat != null) {
                            intent.putExtra(DialogActivity.CHAT_ID, chat.getCid());
                        } else {
                            intent.putExtra(DialogActivity.USER_ID, choosenContact.getUid());
                        }
                        startActivity(intent);
                        finish();
                    }
                    break;
            }
        }
    }
}

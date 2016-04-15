package ru.mail.park.chat.activities;

import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import java.util.List;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.adapters.ChatsAdapter;
import ru.mail.park.chat.activities.adapters.MenuAdapter;
import ru.mail.park.chat.activities.auth_logout.IAuthLogout;
import ru.mail.park.chat.activities.tasks.LogoutTask;
import ru.mail.park.chat.database.MessengerDBHelper;
import ru.mail.park.chat.loaders.ChatSearchLoader;
import ru.mail.park.chat.loaders.ChatWebLoader;
import ru.mail.park.chat.models.Chat;
import ru.mail.park.chat.models.OwnerProfile;

public class ChatsActivity extends AppCompatActivity implements IAuthLogout {
    private FloatingActionButton fab;
    private RecyclerView chatsList;
    private SearchView searchView;
    private SwipeRefreshLayout swipeContainer;

    public static final int CHAT_WEB_LOADER = 0;
    public static final int CHAT_SEARCH_LOADER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Query queryer = new Query();
                // queryer.execute((String[]) null);
                Intent intent = new Intent(ChatsActivity.this, DialogActivity.class);
                intent.putExtra(DialogActivity.CHAT_ID, "123");
                startActivity(intent);
            }
        });

        chatsList = (RecyclerView) findViewById(R.id.chatsList);
        chatsList.setLayoutManager(new LinearLayoutManager(this));

        getLoaderManager().initLoader(CHAT_WEB_LOADER, null, messagesLoaderListener);

        // TODO: real menu options
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.left_drawer);
        mRecyclerView.setHasFixedSize(true);

        String[] titles = {
                getString(R.string.action_show_profile),
                getString(R.string.action_group_chats),
                getString(R.string.contacts),
                getString(R.string.action_settings),
                getString(R.string.action_help),
                getString(R.string.action_log_out)
        };
        View.OnClickListener[] listeners = {new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatsActivity.this, UserProfileActivity.class);
                startActivity(intent);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatsActivity.this, GroupDialogCreateActivity.class);
                startActivity(intent);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatsActivity.this, ContactsActivity.class);
                startActivity(intent);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatsActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        }, null,
           new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("[TechMail]", "starting LogoutTask");
                    OwnerProfile owner = new OwnerProfile(ChatsActivity.this);
                    new LogoutTask(ChatsActivity.this, ChatsActivity.this).execute(owner.getAuthToken());

                    owner.removeFromPreferences(ChatsActivity.this);
                    MessengerDBHelper dbHelper = new MessengerDBHelper(ChatsActivity.this);
                    dbHelper.dropDatabase();

                    Intent intent = new Intent(ChatsActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        
        int[] pictures = {
                R.drawable.ic_person_black_48dp,
                R.drawable.ic_group_black_24dp,
                R.drawable.ic_contacts_black_24dp,
                R.drawable.ic_settings_black_24dp,
                R.drawable.ic_help_black_24dp,
                R.drawable.ic_lock_black_24dp
            };
        OwnerProfile owner = new OwnerProfile(this);
        RecyclerView.Adapter mAdapter = new MenuAdapter(
                owner.getLogin(),
                owner.getEmail(),
                R.drawable.ic_user_picture,
                titles,
                listeners,
                pictures);
        mRecyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(refreshListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chats, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        int searchPlateId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundResource(R.color.colorPrimary);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getLoaderManager().restartLoader(CHAT_SEARCH_LOADER, null, messagesLoaderListener);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                getLoaderManager().restartLoader(CHAT_SEARCH_LOADER, null, messagesLoaderListener);
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                getLoaderManager().restartLoader(CHAT_WEB_LOADER, null, messagesLoaderListener);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            // searchView.setIconified(false);
            searchView.requestFocusFromTouch();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStartLogout() {
        Log.d("[TechMail]", "calling onStartLogout");
    }

    @Override
    public void onLogoutSuccess() {
        Log.d("[TechMail]", "calling onLogoutSuccess");
    }

    @Override
    public void onLogoutFail() {
        Log.d("[TechMail]", "calling onLogoutFail");
    }

    private final LoaderManager.LoaderCallbacks<List<Chat>> messagesLoaderListener =
            new LoaderManager.LoaderCallbacks<List<Chat>>() {
                @Override
                public Loader<List<Chat>> onCreateLoader(int id, Bundle args) {
                    switch (id) {
                        case CHAT_SEARCH_LOADER:
                            ChatSearchLoader searchLoader = new ChatSearchLoader(ChatsActivity.this);
                            searchLoader.setQueryString(searchView.getQuery().toString());
                            return searchLoader;
                        case CHAT_WEB_LOADER:
                        default:
                            return new ChatWebLoader(ChatsActivity.this);
                    }
                }

                @Override
                public void onLoadFinished(Loader<List<Chat>> loader, List<Chat> data) {
                    if (data != null) {
                        chatsList.setAdapter(new ChatsAdapter(data));
                    }
                    swipeContainer.setRefreshing(false);
                }

                @Override
                public void onLoaderReset(Loader<List<Chat>> loader) {
                    if (loader instanceof ChatSearchLoader) {
                        ((ChatSearchLoader) loader).setQueryString(searchView.getQuery().toString());
                    }
                }
            };

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getLoaderManager().restartLoader(0, null, messagesLoaderListener);
        }
    };

}


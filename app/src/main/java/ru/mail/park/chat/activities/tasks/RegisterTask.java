package ru.mail.park.chat.activities.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import java.io.IOException;

import ru.mail.park.chat.api.Auth;
import ru.mail.park.chat.auth_signup.IRegisterCallbacks;
import ru.mail.park.chat.models.OwnerProfile;

/**
 * Created by Михаил on 27.03.2016.
 */
public class RegisterTask extends AsyncTask<String, Void, Pair<String, OwnerProfile>> {
    private final IRegisterCallbacks listener;
    private final Auth auth;

    public RegisterTask(Context context, IRegisterCallbacks listener) {
        auth = new Auth(context);
        this.listener = listener;
        listener.onRegistrationStart();
    }

    @Override
    protected Pair<String, OwnerProfile> doInBackground(String... params) {
        String login = params[0];
        String firstName = params[1];
        String lastName = params[2];
        String password = params[3];
        String email = params[4];

        OwnerProfile user = null;
        String message = null;

        try {
            user = auth.signUp(login, firstName, lastName, password, email);
        } catch (IOException e) {
            message = e.getLocalizedMessage();
        }

        return new Pair<>(message, user);
    }

    @Override
    protected void onPostExecute(Pair<String, OwnerProfile> result) {
        listener.onRegistrationFinish();

        if (result.second != null) {
            listener.onRegistrationSuccess(result.second);
        } else {
            listener.onRegistrationFail(result.first);
        }
    }
}

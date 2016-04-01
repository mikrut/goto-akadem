package ru.mail.park.chat.activities.tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ru.mail.park.chat.message_income.IMessageReaction;
import ru.mail.park.chat.models.Message;

/**
 * Created by 1запуск BeCompact on 27.03.2016.
 */
public class IncomeMessageTask extends AsyncTask<String, Void, Void> {
    private IMessageReaction listener;

    public IncomeMessageTask(Context context, IMessageReaction listener) {
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(String... params) {
        String income = params[0];
        JSONObject jsonIncome = new JSONObject();
        String method = "";
        int mid = 0;
        ArrayList<Message> msgList = new ArrayList<>();

        try {
            jsonIncome = new JSONObject(income);
            method = jsonIncome.getString("method");
            if (jsonIncome.has("mid")) {
                mid = jsonIncome.getInt("mid");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        switch(method) {
            case "SEND":
                listener.onActionSendMessage(income);
                break;
            case "DELETE":
                listener.onActionDeleteMessage(mid);
                break;
            case "GET":
                try {
                    JSONArray jsonMsgArray = jsonIncome.getJSONArray("messages");

                    for(int i = 0; i < jsonMsgArray.length(); i++)
                    {
                        JSONObject item = jsonMsgArray.getJSONObject(i);
                        Message msg = new Message(item);

                        msgList.add(msg);
                    }

                    listener.onGetHistoryMessages(msgList);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                break;
            case "POST":
            case "INCOME":
                listener.onIncomeMessage(income);
                break;
            case "COMET":
                break;
        }
        return null;
    }

    @Override
    protected void onPreExecute() {

    }
}

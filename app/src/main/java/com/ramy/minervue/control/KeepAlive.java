package com.ramy.minervue.control;

import android.os.AsyncTask;
import android.util.Log;

import com.ramy.minervue.app.MainService;
import com.ramy.minervue.util.ATask;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by peter on 11/7/13.
 */
public class KeepAlive extends ATask<Void, Integer, Void> {

    private static final String TAG = "RAMY-KeepAlive";

    private ControlManager controller;

    private int packetsSent = 0;

    public KeepAlive(ControlManager controller) {
        this.controller = controller;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        controller.onKeepAliveSent();
    }

    @Override
    protected Void doInBackground(Void... params) {
        JSONObject json = new JSONObject();
        try {
            json.put("uuid", MainService.getInstance().getUUID());
            json.put("type", "keep-alive");
        } catch (JSONException e) {
            return super.doInBackground();
        }
        while (!isCancelled()) {
        	Log.i(TAG, "KeepAlive");
            controller.sendPacket(json);
            publishProgress(++packetsSent);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }
        return super.doInBackground();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(TAG, "Keep-alive terminated unexpectedly.");
        controller.startKeepAlive();
    }

}

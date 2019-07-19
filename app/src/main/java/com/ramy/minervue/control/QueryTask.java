package com.ramy.minervue.control;

import android.os.AsyncTask;
import android.util.Log;

import com.ramy.minervue.util.ATask;

import org.json.JSONObject;

/**
 * Created by peter on 11/24/13.
 */
public class QueryTask extends ATask<Void, Void, JSONObject> implements ControlManager.PacketListener {

    private static final String TAG = "RAMY-QueryTask";

    private static final int INTERVAL = 1000;

    private ControlManager controlManager;
    private ControlManager.PacketListener listener;
    private int repeat;
    private volatile JSONObject packet;
    private volatile JSONObject reply = null;

    public QueryTask(ControlManager control, JSONObject packet,
                     ControlManager.PacketListener packetListener, int repeat) {
        this.controlManager = control;
        this.listener = packetListener;
        this.packet = packet;
        this.repeat = repeat;
    }

    @Override
    public void onPacketArrival(JSONObject packet) {
        reply = packet;
        cancel(true);
    }

    @Override
    protected void onCancelled() {
        controlManager.removePacketListener(this);
        if (listener != null && reply != null) {
            listener.onPacketArrival(reply);
        }
    }

    @Override
    protected void onPostExecute(JSONObject object) {
        controlManager.removePacketListener(this);
        if (listener != null && reply != null) {
            listener.onPacketArrival(reply);
        }
    }

    @Override
    public String getPacketType() {
        return listener.getPacketType();
    }

    @Override
    protected JSONObject doInBackground(Void... params) {
        Log.i(TAG, "Query for type '" + getPacketType() + "', repeat = " + repeat);
        controlManager.addPacketListener(this);
        while (!isCancelled()) {
            if (repeat == 0) {
                break;
            }
            controlManager.sendPacket(packet);
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                break;
            }
            if (repeat > 0) {
                --repeat;
            }
        }
        return reply;
    }

}

package com.ramy.minervue.control;

import android.util.Log;

import com.ramy.minervue.app.MainService;
import com.ramy.minervue.util.ATask;
import com.ramy.minervue.util.ConfigUtil;
import com.ramy.minervue.util.PreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by peter on 11/7/13.
 */
public class ControlManager implements ConfigUtil.PreferenceListener {

    private static final String TAG = "RAMY-ControlManager";

    private KeepAlive keepAlive = null;

    private DatagramChannel datagramChannel;

    private HashMap<String, LinkedList<PacketListener>> listenerMap = new HashMap<String, LinkedList<PacketListener>>();

    private ConfigUtil configUtil;
    private ByteBuffer buffer = ByteBuffer.wrap(new byte[65536]);
    private Receiver receiver;

    private int noResponseCount = 0;

    public ControlManager() {
        configUtil = MainService.getInstance().getConfigUtil();
        configUtil.addListener(ConfigUtil.ConfigType.KEY_SERVER_ADDR, this);
    }

    public void onKeepAliveSent() {
        ++noResponseCount;
        if (noResponseCount > 3) {
            MainService.getInstance().updateServerOnline(false);
        }
    }

    public void onKeepAliveReceived() {
        noResponseCount = 0;
        MainService.getInstance().updateServerOnline(true);
    }

    public void updateWifiAvailability(boolean available) {
        if (available) {
            startReceiver();
            startKeepAlive();
        } else {
            stopReceiver();
            stopKeepAlive();
        }
    }

    private synchronized void initChannel(String serverAddr, int controlPort) {
        SocketAddress remoteAddr = new InetSocketAddress(serverAddr, controlPort);
        SocketAddress boundPort = new InetSocketAddress(controlPort);
        try {
        	
            datagramChannel = DatagramChannel.open();
            datagramChannel.socket().bind(boundPort);
            datagramChannel.connect(remoteAddr);
            Log.v(TAG, "Control channel " + serverAddr + ":" + controlPort
                    + ", connected: " + datagramChannel.isConnected() + ".");
        } catch (IOException e) {
            e.printStackTrace();
            datagramChannel = null;
        }
    }

    public void release() {
        stopKeepAlive();
        stopReceiver();
        configUtil.removeListener(ConfigUtil.ConfigType.KEY_SERVER_ADDR, this);
    }

    private synchronized void closeChannel() {
        if (datagramChannel != null) {
            try {
                datagramChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            datagramChannel = null;
        }
    }

    public synchronized void sendPacket(JSONObject json) {
        if (datagramChannel != null && datagramChannel.isConnected()) {
            try {
                datagramChannel.write(ByteBuffer.wrap(json.toString().getBytes()));
            } catch (IOException e) {
                Log.w(TAG, "Send packet failed, trying re-establishment.");
                stopReceiver();
                startReceiver();
            }
        }
    }

    public QueryTask createQueryTask(JSONObject json, PacketListener listener, int repeat) {
        return new QueryTask(this, json, listener, repeat);
    }

    public void addPacketListener(PacketListener listener) {
        if (listener == null) {
            return;
        }
        LinkedList<PacketListener> list = listenerMap.get(listener.getPacketType());
        if (list == null) {
            list = new LinkedList<PacketListener>();
            listenerMap.put(listener.getPacketType(), list);
        }
        list.add(listener);
    }

    public void removePacketListener(PacketListener listener) {
        LinkedList<PacketListener> list = listenerMap.get(listener.getPacketType());
        if (list != null && listener != null) {
            list.remove(listener);
        }
    }

    public void startKeepAlive() {
        if (keepAlive == null || keepAlive.isFinished()) {
            keepAlive = new KeepAlive(this);
            keepAlive.start();
            Log.v(TAG, "Keep-alive started.");
        }
    }

    public void startReceiver() {
        if (receiver == null || receiver.isFinished()) {
            receiver = new Receiver();
            receiver.start();
            Log.v(TAG, "Controller started listening.");
        }
    }

    public void stopKeepAlive() {
        if (keepAlive != null) {
            keepAlive.cancelAndClear(true);
            keepAlive = null;
            Log.v(TAG, "Keep-alive exited.");
        }
    }

    public void stopReceiver() {
        if (receiver != null) {
            receiver.cancelAndClear(true);
            receiver = null;
            Log.v(TAG, "Controller stopped listening.");
        }
    }

    @Override
    public void onPreferenceChanged() {
        if (receiver != null) {
            Log.i(TAG, "Server changed, restarting.");
            stopReceiver();
            startReceiver();
        }
    }

    public interface PacketListener {
        public void onPacketArrival(JSONObject packet);
        public String getPacketType();
    }

    private class Receiver extends ATask<Void, JSONObject, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String serverAddr = configUtil.getServerAddr();
            int controlPort = configUtil.getPortControl();
            initChannel(serverAddr, controlPort);
            while (!isCancelled()) {
                buffer.clear();
                try {
                    int count = datagramChannel.read(buffer);
                    if (count < 0) {
                        continue;
                    }
                    String s = new String(buffer.array(), 0, count);    // Safe since it's wrapped.
                    JSONObject json = new JSONObject(s);
                    publishProgress(json);
                } catch (ClosedByInterruptException e) {
                    break;
                } catch (IOException e) {
                    // Ignored.
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            closeChannel();
            return super.doInBackground();
        }

        @Override
        protected void onProgressUpdate(JSONObject... values) {
            String type = null;
            try {
                type = values[0].getString("type");
                Log.i(TAG, " type ="+type);
            } catch (JSONException e) {
                return;
            }
            LinkedList<PacketListener> listeners = listenerMap.get(type);
            if (listeners != null) {
                for (PacketListener listener : listeners) {
                    listener.onPacketArrival(values[0]);
                }
            }
            if (type.equals("keep-alive")) {
                onKeepAliveReceived();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "Receiver terminated unexpectedly.");
            startReceiver();
        }

    }

}

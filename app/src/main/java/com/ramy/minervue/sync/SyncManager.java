package com.ramy.minervue.sync;

import android.content.Context;
import android.util.Log;

import com.ramy.minervue.app.MainService;
import com.ramy.minervue.util.ConfigUtil;
import com.ramy.minervue.util.PreferenceUtil;

/**
 * Created by peter on 11/25/13.
 */
public class SyncManager implements ConfigUtil.PreferenceListener {

    private static final String TAG = "RAMY-SyncManager";

    private LocalFileUtil localFileUtil;
    private SyncTask syncThread = null;
    private ConfigUtil configUtil = null;

    private boolean isWifiAvailable = false;

    public SyncManager(Context context) {
        configUtil = MainService.getInstance().getConfigUtil();
        configUtil.addListener(ConfigUtil.ConfigType.KEY_SERVER_ADDR, this);
        localFileUtil = new LocalFileUtil(context);
    }

    public void updateWifiAvailability(boolean available) {
        isWifiAvailable = available;
        if (available) {
            startSync();
        } else {
            stopSync();
        }
    }

    public void startSync() {
        if ((syncThread == null || syncThread.isFinished()) && isWifiAvailable) {
            syncThread = new SyncTask(localFileUtil, configUtil.getServerAddr());
            syncThread.start();
            Log.v(TAG, "File sync started.");
        }
    }

    public void stopSync() {
        if (syncThread != null) {
            syncThread.cancel(true);
            syncThread = null;
            Log.v(TAG, "File sync aborted.");
        }
    }

    public LocalFileUtil getLocalFileUtil() {
        return localFileUtil;
    }

    @Override
    public void onPreferenceChanged() {
        if (syncThread != null) {
            Log.i(TAG, "Server changed, restarting.");
            stopSync();
            startSync();
        }
    }

}

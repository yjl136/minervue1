package com.ramy.minervue.util;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.HashMap;
import java.util.LinkedList;

public class PreferenceUtil implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_TIME_OFFSET = "timeOffset";
    public static final String KEY_UNREAD_DOWNLOAD = "unreadDownload";
    public static final String KEY_SERIAL_NUMBER = "serialNumber";

    private SharedPreferences sharedPreferences = null;

    private HashMap<String, LinkedList<PreferenceListener>> listenerMap = new HashMap<String, LinkedList<PreferenceListener>>();
    
    public PreferenceUtil(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    public void release() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
    public void addListener(String key, PreferenceListener listener) {
        if (listener != null) {
            LinkedList<PreferenceListener> list = listenerMap.get(key);
            if (list == null) {
                list = new LinkedList<PreferenceListener>();
                listenerMap.put(key, list);
            }
            list.add(listener);
        }
    }

    public void removeListener(String key, PreferenceListener listener) {
        LinkedList<PreferenceListener> list = listenerMap.get(key);
        if (list != null && listener != null) {
            list.remove(listener);
        }
    }

    public long getTimeOffset(PreferenceListener listener) {
        addListener(KEY_TIME_OFFSET, listener);
        return sharedPreferences.getLong(KEY_TIME_OFFSET, 0);
    }

    public void setTimeOffset(long offset) {
        sharedPreferences.edit().putLong(KEY_TIME_OFFSET, offset).commit();
    }

    public void setUnreadDownload(int unread) {
        sharedPreferences.edit().putInt(KEY_UNREAD_DOWNLOAD, unread).commit();
    }

    public int getUnreadDownload(PreferenceListener listener) {
        addListener(KEY_UNREAD_DOWNLOAD, listener);
        return sharedPreferences.getInt(KEY_UNREAD_DOWNLOAD, 0);
    }


    public void setSerialNumber(String serialNumber) {
        sharedPreferences.edit().putString(KEY_SERIAL_NUMBER, serialNumber).commit();
    }

    public String getSerialNumber(PreferenceListener listener) {
        addListener(KEY_SERIAL_NUMBER, listener);
        return sharedPreferences.getString(KEY_SERIAL_NUMBER, null);
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LinkedList<PreferenceListener> list = listenerMap.get(key);
        if (list != null) {
            for (PreferenceListener listener : list) {
                listener.onPreferenceChanged();
            }
        }
    }

    public interface PreferenceListener {
        public void onPreferenceChanged();
    }

}

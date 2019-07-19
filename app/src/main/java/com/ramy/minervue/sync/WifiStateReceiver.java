package com.ramy.minervue.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.ramy.minervue.app.MainService;

/**
 * Created by peter on 11/25/13.
 */
public class WifiStateReceiver extends BroadcastReceiver {

	private static final String TAG = "RAMY-WifiStateReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "onreceive");
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			MainService service = MainService.getInstance();
			if (service != null) {
				NetworkInfo info = cm.getActiveNetworkInfo();
				if (info != null && info.isAvailable() && info.isConnected()) {
					Log.i(TAG, "true");
				}else{
					Log.i(TAG, "false");
				}
				service.updateWifiAvailability(info != null
						&& info.isAvailable() && info.isConnected());
			}
		}
	}

}

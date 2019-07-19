package com.ramy.minervue.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceLoader extends BroadcastReceiver {

    private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION.equals(intent.getAction())) {
            Intent service = new Intent(context, MainService.class);
            context.startService(service);
        }
    }

}

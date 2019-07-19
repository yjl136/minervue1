package com.ramy.minervue.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by peter on 3/8/14.
 */
public class HourlyAlarm {

    private AlarmManager manager;
    private PendingIntent pendingIntent;

    public HourlyAlarm(Context context, Class<?> receiver) {
        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiver);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public void start() {
        manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
    }

    public void cancel() {
        manager.cancel(pendingIntent);
    }

}

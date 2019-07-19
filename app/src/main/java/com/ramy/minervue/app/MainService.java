package com.ramy.minervue.app;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import com.ramy.minervue.R;
import com.ramy.minervue.control.ControlManager;
import com.ramy.minervue.sync.LocalFileUtil;
import com.ramy.minervue.sync.SyncManager;
import com.ramy.minervue.util.ConfigUtil;
import com.ramy.minervue.util.HourlyAlarm;
import com.ramy.minervue.util.License;
import com.ramy.minervue.util.PreferenceUtil;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

/**
 * Created by peter on 11/7/13.
 */
public class MainService extends Service implements Thread.UncaughtExceptionHandler {

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "RAMY-MainService";

    private static MainService instance = null;

    private KeyguardManager  km;
	private KeyguardLock kl;
	private PowerManager pm;
	private PowerManager.WakeLock wl;
    private ControlManager controlManager;
    private PreferenceUtil preferenceUtil;
    private ConfigUtil configUtil;
    private SyncManager syncManager;
    private PowerManager.WakeLock wakeLock = null;
    private boolean upgradeAttempted = false;
    private boolean timeAdjustAttempted = false;

    private boolean isWifiAvailable = false;
    private boolean isServerOnline = false;
    private boolean registeredFunctionStarted = false;

    private ArrayList<ServerOnlineStatusListener> onlineStatusListeners = new ArrayList<ServerOnlineStatusListener>();
    private MonitorActivity activeMonitor;
    private License license;
    private HourlyAlarm hourlyAlarm;

    private LocalBinder binder = new LocalBinder();

    public static MainService getInstance() {
        return instance;
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Notification getNotification(boolean online) {
        String titleText = getString(R.string.app_name);
        String contentText = getString(online ? R.string.app_is_online : R.string.app_is_offline);
        Intent intent = new Intent(this, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent);
        return builder.build();
    }

    private void wakeAndUnlock(boolean b)
	{
		if(b)
		{
			pm=(PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
			wl.acquire();
			km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
			kl = km.newKeyguardLock("unLock");
			kl.disableKeyguard();
		}
		else
		{
			kl.reenableKeyguard();
			wl.release();
		}
	}
    public int getCurrentVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public ControlManager getControlManager() {
        return controlManager;
    }

    public PreferenceUtil getPreferenceUtil() {
        return preferenceUtil;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public void startRegisteredFunctions() {
        controlManager.addPacketListener(new ControlManager.PacketListener() {
            @Override
            public void onPacketArrival(JSONObject packet) {
                if (!timeAdjustAttempted) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        Date date = sdf.parse(packet.getString("time"));
                        long offset = date.getTime() - System.currentTimeMillis();
                        preferenceUtil.setTimeOffset(offset);
                        Log.i(TAG, "App time adjusted.");
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing new time.");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error getting new time.");
                    }
                    timeAdjustAttempted = true;
                }
                try {
                    int version = packet.getInt("versionCode");
                    if (!upgradeAttempted) {
                        upgradeApplication(version);
                    }
                } catch (JSONException e) {
                    // Ignored.
                }
            }
            @Override
            public String getPacketType() {
                return "keep-alive";
            }
        });
    	controlManager.addPacketListener(new ControlManager.PacketListener() {
			@Override
			public void onPacketArrival(JSONObject packet) {
				try {

					if ("start".equals(packet.getString("action"))) {
						
						
						if("requested-photo".equals(packet.getString("result").substring(0,packet.getString("result").length()-1))){
							String count = packet.getString("result").substring(packet.getString("result").length()-1, packet.getString("result").length());
							try {
								Uri uri = RingtoneManager.getDefaultUri(
										RingtoneManager.TYPE_NOTIFICATION);
								Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), uri);
								r.play();
								wakeAndUnlock(true);
								Intent intent = new Intent(MainService.this, VideoActivity.class);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.putExtra(getPackageName() + ".StartNow", true);
								intent.putExtra("Phtot_count_int", Integer.parseInt(count));
								startActivity(intent);
							} catch (Exception e) {
								Uri uri = RingtoneManager.getDefaultUri(
										RingtoneManager.TYPE_ALARM);
								Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), uri);
								r.play();
							}
							return ;
						}
						Log.i(TAG, "Received requested-video.");
						try {
							Uri uri = RingtoneManager.getDefaultUri(
									RingtoneManager.TYPE_NOTIFICATION);
							Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), uri);
							r.play();
						} catch (Exception e) {
							Log.e(TAG, "Cannot play notification ringtone.");
						}
						if (activeMonitor == null) {
							Log.i(TAG, "Starting new monitor.");
							Intent intent = new Intent(MainService.this, MonitorActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.putExtra(getPackageName() + ".StartNow", true);
							startActivity(intent);
						} else {
							Log.i(TAG, "Active monitor exists, starting passively.");
							activeMonitor.startPassively();
						}
					} else if ("stop".equals(packet.getString("action"))) {
						MonitorActivity activity = getActiveMonitorActivity();
						if (activity != null) {
							activity.stopMonitor();
						}
					}
				} catch (JSONException e) {
					// Ignored.
				}
			}
			@Override
			public String getPacketType() {
				return "requested-video";
			}
		});
        controlManager.addPacketListener(new ControlManager.PacketListener() {
            @Override
            public void onPacketArrival(JSONObject packet) {
                MonitorActivity activity = getActiveMonitorActivity();
                if (activity != null) {
                    activity.onSwitchCamera(null);
                }
            }
            @Override
            public String getPacketType() {
                return "switch-camera";
            }
        });
        controlManager.addPacketListener(new ControlManager.PacketListener() {
            @Override
            public void onPacketArrival(JSONObject packet) {
                syncManager.startSync();
            }
            @Override
            public String getPacketType() {
                return "start-sync";
            }
        });
        isWifiAvailable = isWifiAvailable();
        controlManager.updateWifiAvailability(isWifiAvailable);
        syncManager.updateWifiAvailability(isWifiAvailable);
        if (isWifiAvailable) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backLock");
            wakeLock.acquire();
        }
        hourlyAlarm.start();
        registeredFunctionStarted = true;
        Log.v(TAG, "Registered function started. Wifi availability: "  + isWifiAvailable + ".");
    }

    /**
     * Periodically call this function to ensure detection of license expiration.
     */
    public void regularRegistrationCheck() {
        if (registeredFunctionStarted && !license.verifyCurrentLicense()) {
            Log.w(TAG, "License expired, exiting.");
            stopSelf();
        } else {
            Log.i(TAG, "Hourly check: license ok.");
        }
    }

    public String getMachineDigest() {
        return license.getMachineDigest();
    }

    public String getUUID() {
        return configUtil.getUDID();
    }

    /**
     * Verify given serial number and start registered functions upon success.
     * @param base64 The serial number.
     * @return null if failed to verify, {startDate, endDate} otherwise.
     */
    public Date[] verifyRegistration(String base64) {
        Date[] region = license.verifyLicense(base64);
        if (region != null) {
            preferenceUtil.setSerialNumber(base64);
            if (!registeredFunctionStarted) {
                startRegisteredFunctions();
            }
        }
        return region;
    }

    public boolean verifyCurrentRegistration() {
        return license.verifyCurrentLicense();
    }

    @Override
    public void onCreate() {
        configUtil = ConfigUtil.getInstance();
        if (configUtil == null) {
            Log.e(TAG, "No valid config file found, exiting.");
            System.exit(0);
            return;
        }
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(this);
        PreferenceManager.setDefaultValues(this, R.xml.preference, true);
        startForeground(NOTIFICATION_ID, getNotification(false));
        preferenceUtil = new PreferenceUtil(getApplicationContext());
        controlManager = new ControlManager();
        syncManager = new SyncManager(this);

        license = new License();
        hourlyAlarm = new HourlyAlarm(getApplicationContext(), AlarmReceiver.class);
        if (license.verifyCurrentLicense()) {
            startRegisteredFunctions();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public MonitorActivity getActiveMonitorActivity() {
        return activeMonitor;
    }

    public void setActiveMonitorActivity(MonitorActivity monitor) {
        activeMonitor = monitor;
    }

    public boolean isWifiAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.getType() == ConnectivityManager.TYPE_WIFI
                && info.isConnected();
    }

    public String getLocalIp() {
        if (!isWifiAvailable) {
            return null;
        }
        Enumeration<NetworkInterface> en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    String ip = addr.getHostAddress();
                    if (!addr.isLoopbackAddress() && !addr.isMulticastAddress()
                            && InetAddressUtils.isIPv4Address(ip)) {
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            return null;
        }
        return null;
    }

    public void updateWifiAvailability(boolean available) {
        if (isWifiAvailable != available && registeredFunctionStarted) {
            Log.i(TAG, "Wifi availability changed to: " + available + ".");
            controlManager.updateWifiAvailability(available);
            syncManager.updateWifiAvailability(available);
        }
        isWifiAvailable = available;
        if (!isWifiAvailable) {
            updateServerOnline(false);
        }
        if (!isWifiAvailable && wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        } else if (isWifiAvailable && wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backLock");
            wakeLock.acquire();
        }
    }

    @Override
    public void onDestroy() {
        instance = null;
        updateWifiAvailability(false);
        stopForeground(true);
        controlManager.release();
        preferenceUtil.release();
        configUtil.release();
        hourlyAlarm.cancel();
        System.exit(0);
    }

    public void upgradeApplication(int version) {
        if (version > getCurrentVersion()) {
            Log.i(TAG, "New version detected: " + version + ".");
            String name = "minervue-" + version + ".apk";
            File file = syncManager.getLocalFileUtil().getFile(
                    LocalFileUtil.DIR_PUB, name);
            if (file.exists() && !LocalFileUtil.isFileInUse(file)) {
                Log.i(TAG, "Apk available, upgrading.");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                upgradeAttempted = true;
                startActivity(intent);
            }
        }
    }

    public void updateServerOnline(boolean online) {
        isServerOnline = online;
        NotificationManager manager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, getNotification(online));
        for (ServerOnlineStatusListener l : onlineStatusListeners) {
            l.onServerOnlineStatusChanged(online);
        }
    }

    public boolean getServerOnlineStatus() {
        return isServerOnline;
    }

    public void addServerOnlineStatusListener(ServerOnlineStatusListener listener) {
        if (listener != null) {
            onlineStatusListeners.add(listener);
        }
    }

    public void removeServerOnlineStatusListener(ServerOnlineStatusListener listener) {
        onlineStatusListeners.remove(listener);
    }
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        BugReportActivity.start(this, ex);
        System.exit(-1);
    }

    public interface ServerOnlineStatusListener {
        public void onServerOnlineStatusChanged(boolean isOnline);

    }

    public class LocalBinder extends Binder {

        public MainService getService() {
            return MainService.this;
        }

    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            MainService service = MainService.getInstance();
            if (service != null) {
                service.regularRegistrationCheck();
            }
        }

    }

}

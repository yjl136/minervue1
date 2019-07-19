package com.ramy.minervue.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ramy.minervue.R;
import com.ramy.minervue.camera.MyCamera;
import com.ramy.minervue.camera.PreviewSizeAdapter;
import com.ramy.minervue.control.ControlManager;
import com.ramy.minervue.media.Monitor;
import com.ramy.minervue.media.VideoCodec;
import com.ramy.minervue.control.QueryTask;
import com.ramy.minervue.sync.LocalFileUtil;
import com.ramy.minervue.util.ATask;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by peter on 11/7/13.
 */
public class MonitorActivity extends BaseSurfaceActivity implements MainService.ServerOnlineStatusListener, Monitor.OnAbortListener {

    private static final String TAG = "RAMY-MonitorActivity";

    public static final int PREFERRED_WIDTH = 720;
    public static final int PREFERRED_HEIGHT = 480;
    public AudioManager am;
    private TextView infoText;
    private Button actionButton;

    private ProgressDialog dialog;
    private QueryTask queryTask = null;
    private Timer timer = null;
    private boolean isPassive = false;
    private Monitor monitor;
    private ControlManager.PacketListener listener = new PacketListener();

    private boolean isPassive() {
        return isPassive;
    }

    public void startPassively() {
        if (!monitor.isCapturing()) {
            isPassive = true;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isPassive) {
            super.onBackPressed();
        }
    }

    private void sendRequest() {
        JSONObject json = new JSONObject();
        Camera.Size size = getVideoCodec().getCurrentCamera().getCurrentPreviewSize();
        try {
            json.put("uuid", MainService.getInstance().getUUID());
            json.put("type", "video");
            json.put("width", size.width);
            json.put("height", size.height);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ControlManager control = MainService.getInstance().getControlManager();
        queryTask = control.createQueryTask(json, listener, -1);
        queryTask.start();
    }

    public void onServerCall(View view) {
        if (monitor.isCapturing()) {
            stopMonitor();
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            String info = getString(R.string.contacting_server);
            dialog = ProgressDialog.show(this, "", info, true, false);
            timer = new Timer();
            monitor.prepare();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    timer.cancel(true);
                    queryTask.cancel(true);
                    queryTask = null;
                    monitor.reset();
                }
            });
            timer.start();
            sendRequest();
        }
    }

    public void startMonitor() {
        if (!monitor.isCapturing()) {
            setUILevel(UI_LEVEL_BUSY);
            LocalFileUtil util = MainService.getInstance().getSyncManager().getLocalFileUtil();
            monitor.start(util.generateVideoFilename(1));
        }
    }

    public void stopMonitor() {
        if (monitor.isCapturing()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            monitor.stop();
            toast(getString(R.string.saved) + monitor.getMuxer().getFilePath());
            setUILevel(UI_LEVEL_NORMAL);
            MainService.getInstance().getSyncManager().startSync();
            if (isPassive()) {
                finish();
            }
        }
    }

    @Override
    protected VideoCodec getVideoCodec() {
        return monitor.getVideoCodec();
    }

    @Override
    protected void setUILevel(int level) {
        if (level == UI_LEVEL_BUSY) {
            actionButton.setText(R.string.stop_monitor);
            infoText.setText(R.string.being_monitored);
        } else {
            actionButton.setText(R.string.start_monitor);
            boolean isOnline = MainService.getInstance().getServerOnlineStatus();
            infoText.setText(isOnline ? R.string.app_is_online : R.string.app_is_offline);
        }
        actionButton.setEnabled(!isPassive() && level >= UI_LEVEL_BUSY);
        super.setUILevel(level);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        MainService.getInstance().setActiveMonitorActivity(this);
        MainService.getInstance().addServerOnlineStatusListener(this);
        am=(AudioManager) getSystemService(Context.AUDIO_SERVICE);
        monitor = new Monitor(am);
        setContentView(R.layout.monitor_activity);
        monitor.setOnAbortListener(this);
        isPassive = getIntent().getBooleanExtra(getPackageName() + ".StartNow", false);
    }

    @Override
    public void onContentChanged() {
        infoText = (TextView) findViewById(R.id.tv_monitor_info);
        actionButton = (Button) findViewById(R.id.bt_call_server);
        super.onContentChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (monitor.getVideoCodec().isPreviewing()) {
            if (dialog != null && dialog.isShowing()) {
                timer.cancel(true);
                queryTask.cancel(true);
                monitor.reset();
                queryTask = null;
                dialog.dismiss();
            }
            stopMonitor();
            getVideoCodec().stopPreview();
            MainService.getInstance().removeServerOnlineStatusListener(this);
            MainService.getInstance().setActiveMonitorActivity(null);
            finish();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "Surface available, start preview.");
        VideoCodec codec = monitor.getVideoCodec();
        if (codec.startPreview(surface, PREFERRED_WIDTH, PREFERRED_HEIGHT)) {
            setUILevel(UI_LEVEL_NORMAL);
            updateUIForCamera(codec.getCurrentCamera());
            if (isPassive()) {
                Log.i(TAG, "Starting passively.");
                onServerCall(null);
            }
        } else {
            toast(getString(R.string.unknown_error));
        }
    }

    @Override
    public void onServerOnlineStatusChanged(final boolean isOnline) {
        if (!monitor.isCapturing()) {
            infoText.setText(isOnline ? R.string.app_is_online : R.string.app_is_offline);
        }
    }

    @Override
    public void onAbort() {
        toast(getString(R.string.connection_lost));
        toast(getString(R.string.saved) + monitor.getMuxer().getFilePath());
        setUILevel(UI_LEVEL_NORMAL);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MainService.getInstance().getSyncManager().startSync();
        if (isPassive()) {
            finish();
        }
    }

    private class Timer extends ATask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // Ignored.
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog == null || !dialog.isShowing()) {
                return;
            }
            dialog.cancel();
            toast(getString(R.string.server_no_respond));
        }

    }

    private class PacketListener implements ControlManager.PacketListener {

        @Override
        public String getPacketType() {
            return "video";
        }

        @Override
        public void onPacketArrival(JSONObject packet) {
            if (dialog == null || !dialog.isShowing()) {
                Log.w(TAG, "Invalid remote response.");
                return;
            }
            timer.cancel(true);
            try {
                boolean available = "ok".equals(packet.getString("result"));
                if (available) {
                    Log.i(TAG, "Remote accepted monitor request, starting.");
                    startMonitor();
                } else {
                    Log.i(TAG, "Remote rejected monitor request.");
                    monitor.reset();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    toast(getString(R.string.server_rejected_you));
                }
            } catch (JSONException e) {
                // Ignored.
            }
            dialog.dismiss();
        }

    }

}
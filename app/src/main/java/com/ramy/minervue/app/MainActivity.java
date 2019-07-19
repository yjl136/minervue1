 package com.ramy.minervue.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ramy.minervue.R;
import com.ramy.minervue.sync.LocalFileUtil;
import com.ramy.minervue.util.PreferenceUtil;

/**
 * Created by peter on 11/2/13.
 */
public class MainActivity extends Activity implements MainService.ServerOnlineStatusListener, PreferenceUtil.PreferenceListener {

    private TextView infoText;
    private TextView unreadText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        PackageManager manager = getPackageManager();
        String version = "";
        try {
            version = manager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            // Ignore.
        }
        setTitle(getString(R.string.app_name) + " v" + version);
        infoText = (TextView) findViewById(R.id.tv_online_status);
        unreadText = (TextView) findViewById(R.id.tv_unread_download);
        MainService service = MainService.getInstance();
        boolean isOnline = service.getServerOnlineStatus();
        infoText.setText(isOnline ? R.string.app_is_online : R.string.app_is_offline);
        int unread = service.getPreferenceUtil().getUnreadDownload(this);
        if (unread > 0) {
            unreadText.setText(Integer.toString(unread));
            unreadText.setVisibility(View.VISIBLE);
        }
        service.addServerOnlineStatusListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainService service = MainService.getInstance();
        service.removeServerOnlineStatusListener(this);
        service.getPreferenceUtil().removeListener(PreferenceUtil.KEY_UNREAD_DOWNLOAD, this);
    }

    public void onGasDetection(View view) {
        startActivity(new Intent(this, GasActivity.class));
    }

    public void onSetting(View view) {
        startActivity(new Intent(this, SettingActivity.class));
    }

    public void onBrowsing(View view) {
        LocalFileUtil util = MainService.getInstance().getSyncManager().getLocalFileUtil();
        Intent intent = new Intent(this, FileListActivity.class);
        intent.putExtra(getPackageName() + ".ListFile", util.getRoot());
        startActivity(intent);
    }

    public void onRecording(View view) {
        startActivity(new Intent(this, VideoActivity.class));
    }

    public void onMonitoring(View view) {
        startActivity(new Intent(this, MonitorActivity.class));
    }

    @Override
    public void onServerOnlineStatusChanged(final boolean isOnline) {
        infoText.setText(isOnline ? R.string.app_is_online : R.string.app_is_offline);
    }

    @Override
    public void onPreferenceChanged() {
        MainService service = MainService.getInstance();
        int unread = service.getPreferenceUtil().getUnreadDownload(null);
        if (unread > 0) {
            unreadText.setText(Integer.toString(unread));
            unreadText.setVisibility(View.VISIBLE);
        } else {
            unreadText.setText("");
            unreadText.setVisibility(View.INVISIBLE);
        }
    }

}

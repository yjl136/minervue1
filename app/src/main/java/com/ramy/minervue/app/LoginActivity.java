package com.ramy.minervue.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ramy.minervue.R;
import com.ramy.minervue.sync.LocalFileUtil;
import com.ramy.minervue.util.ATask;
import com.ramy.minervue.util.ConfigUtil;
import com.ramy.minervue.ffmpeg.MP4Muxer;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by peter on 10/21/13.
 */
public class LoginActivity extends Activity {

    private static final String TAG = "RAMY-LoginActivity";

    private Dialog serviceDialog = null;

    private Dialog registerDialog = null;

    private LicenseValidationTask validationTask = null;

    private TextView textViewMac;

    private EditText editTextSN;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (serviceDialog != null) {
                serviceDialog.dismiss();
            }
            if (MainService.getInstance().verifyCurrentRegistration()) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                setContentView(R.layout.login_activity);
                textViewMac = (TextView) findViewById(R.id.tv_machine_mac);
                editTextSN = (EditText) findViewById(R.id.et_sn);
                textViewMac.setText(MainService.getInstance().getMachineDigest());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preference, true);
        serviceDialog = ProgressDialog.show(this, "",
                getString(R.string.waiting_for_background_service), true, false);
        startService(new Intent(this, MainService.class));
        bindService(new Intent(this, MainService.class), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    private void validateSerial(String sn) {
        Date[] date = MainService.getInstance().verifyRegistration(sn);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            builder.setTitle(R.string.succeeded);
            String content = getString(R.string.valid_date) + "\n"
                    + getString(R.string.from) + sdf.format(date[0]) + "\n"
                    + getString(R.string.to) + sdf.format(date[1]) + "\n";
            builder.setMessage(content);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            });
            builder.show();
        } else {
            builder.setTitle(R.string.failed);
            builder.setMessage(R.string.please_check_your_sn);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }

    public void onRegister(View view) {
        Editable editable = editTextSN.getText();
        if (editable == null) {
            return;
        }
        validateSerial(editable.toString());
    }

    public void onAutoRegister(View view) {
        ConfigUtil config = MainService.getInstance().getConfigUtil();
        validationTask = new LicenseValidationTask(config.getServerAddr());
        validationTask.start();
        registerDialog = ProgressDialog.show(this, "",
                getString(R.string.fetching_remote_sn), true, true);
        registerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (validationTask != null) {
                    validationTask.cancel(true);
                    validationTask = null;
                }
            }
        });
    }

    public void onCopyMac(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Machine ID", textViewMac.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
    }

    public class LicenseValidationTask extends ATask<Void, Void, String> {

        private static final String TAG = "RAMY-LicenseValidationTask";
        private static final String LOGIN_NAME = "ramy";
        private static final String LOGIN_PASS = "ramy";

        private String serverAddr;
        private FTPClient client = null;

        public LicenseValidationTask(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        private boolean connect() {
            try {
                client = new FTPClient();
                client.setConnectTimeout(5000);
                Log.i(TAG, "Connecting to " + serverAddr + "...");
                client.connect(InetAddress.getByName(serverAddr));
                if (client.login(LOGIN_NAME, LOGIN_PASS)) {
                    return true;
                } else {
                    disconnect();
                }
            } catch (IOException e) {
                disconnect();
            }
            return false;
        }

        private void disconnect() {
            if (client != null) {
                try {
                    client.logout();
                    client.disconnect();
                } catch (IOException e) {
                    // Expected.
                }
                client = null;
            }
        }

        private boolean makeEnter(String dirName) {
            if (dirName == null) {
                disconnect();
                return false;
            }
            try {
                client.makeDirectory(dirName);
                if (!client.changeWorkingDirectory(dirName)) {
                    disconnect();
                    return false;
                }
                return true;
            } catch (IOException e) {
                disconnect();
                return false;
            }
        }

        private String download(String filename) {
            try {
                client.enterLocalPassiveMode();//��FTP�Ĵ��䷽ʽ��Ϊ����ģʽ
                InputStream is = client.retrieveFileStream(filename);
                String result = null;
                if (is != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    result = reader.readLine();
                    Log.d(TAG, result);
                    reader.close();
                    client.completePendingCommand();
                }
                return result == null ? "" : result;
            } catch (IOException e) {
                return "";
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            if (!connect()) {
                Log.e(TAG, "Failed to connect.");
                return "";
            }
            if (!makeEnter(LocalFileUtil.DIR_PUB)) {
                Log.e(TAG, "Failed to enter public directory.");
                return "";
            }
            String digest = MainService.getInstance().getMachineDigest();
            String result = download(digest + ".dat");
            disconnect();
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (registerDialog != null && registerDialog.isShowing()) {
                registerDialog.dismiss();
            }
            validateSerial(s);
        }

    }

}

package com.ramy.minervue.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.ramy.minervue.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Created by peter on 1/2/14.
 */
public class BugReportActivity extends Activity {

    public static void start(Context a, Throwable e) {
        Intent intent = new Intent(a, BugReportActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        intent.putExtra(a.getPackageName() + "ErrInfo", writer.toString());
        a.startActivity(intent);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void writeLog(String info) {
        File folder = Environment.getExternalStorageDirectory();
        File file = new File(folder, UUID.randomUUID().toString() + ".log");
        FileWriter out = null;
        try {
            out = new FileWriter(file);
            out.write(info);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignored.
                }
            }
        }
        toast(getString(R.string.crash_log_saved_to) + file.getPath());
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(getPackageName() + "ErrInfo")) {
            finish();
            return;
        }
        setContentView(R.layout.bug_report_activity);
        TextView view = (TextView) findViewById(R.id.foxish_bug_report_activity_stacktrace);
        String info = getIntent().getStringExtra(getPackageName() + "ErrInfo");
        view.setText(info);
        writeLog(info);
        setResult(RESULT_OK);
    }


}

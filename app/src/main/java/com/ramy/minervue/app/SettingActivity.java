package com.ramy.minervue.app;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

/**
 * Created by peter on 11/2/13.
 */
public class SettingActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        SettingFragment fragment = new SettingFragment();
        fm.beginTransaction().replace(android.R.id.content, fragment).commit();
    }

}
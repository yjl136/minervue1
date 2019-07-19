package com.ramy.minervue.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.ramy.minervue.app.MainService;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by peter on 3/6/14.
 */
public class License {

    private static final String TAG = "RAMY-License";

    private static final String KEY_SN = "serialNumber";

    private static byte[] KEY = {(byte) 0xB8, (byte) 0x99, (byte) 0x6, (byte) 0x64, (byte) 0xF0,
            (byte) 0x52, (byte) 0x91, (byte) 0xC7, (byte) 0xE3, (byte) 0x61, (byte) 0xC9,
            (byte) 0x30, (byte) 0x75, (byte) 0x98, (byte) 0x98, (byte) 0x85, (byte) 0x28,
            (byte) 0xA2, (byte) 0x18, (byte) 0x91, (byte) 0x25, (byte) 0xC6, (byte) 0x84,
            (byte) 0x8A, (byte) 0x07, (byte) 0x33, (byte) 0x41, (byte) 0x77, (byte) 0xF7,
            (byte) 0x3D, (byte) 0xF3, (byte) 0x24};

    private static byte[] IV = {(byte) 0x4E, (byte) 0xD, (byte) 0x48, (byte) 0x3B, (byte) 0xC6,
            (byte) 0xC4, (byte) 0x69, (byte) 0x9B, (byte) 0x85, (byte) 0xCC, (byte) 0x88,
            (byte) 0x35, (byte) 0xF1, (byte) 0xCF, (byte) 0x54, (byte) 0x2C};

    private static String ALGORITHM = "AES/CBC/PKCS7Padding";

    public byte[] getMac() {
        List<NetworkInterface> interfaces = null;
        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface i : interfaces) {
                if (i.getName().equalsIgnoreCase("wlan0")) {
                    byte[] addr = i.getHardwareAddress();
                    if (addr != null) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            // Ignore.
        }
        return null;
    }

    public String getMachineDigest() {
        Context c = MainService.getInstance();
        TelephonyManager m = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        return m.getDeviceId();
    }

    private String getSerialNumber() {
        return MainService.getInstance().getPreferenceUtil().getSerialNumber(null);
    }

    public boolean verifyCurrentLicense() {
        String sn = getSerialNumber();
        return sn != null && verifyLicense(sn) != null;
    }

    private Date toDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    private Date getNow() {
        long offset = MainService.getInstance().getPreferenceUtil().getTimeOffset(null);
        return new Date(System.currentTimeMillis() + offset);
    }
    public Date[] verifyLicense(String sn) {
        try {
            byte[] encrypted = Base64.decode(sn.getBytes(), Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec spec = new SecretKeySpec(KEY, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(IV));
            String[] info = new String(cipher.doFinal(encrypted)).split(",");
            if (info.length != 3 || !info[0].equals(getMachineDigest())) {
                return null;
            }
            Date startDate = toDate(info[1]);
            if (startDate == null) {
                return null;
            }
            int validDay = 0;
            try {
                validDay = Integer.parseInt(info[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.setTime(startDate);
            c.add(Calendar.DATE, validDay);
            Date endDate = c.getTime();
            Date now = getNow();
            if (startDate.before(now) && endDate.after(now)) {
                return new Date[] {startDate, endDate};
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

}

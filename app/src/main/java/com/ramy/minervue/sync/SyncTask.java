package com.ramy.minervue.sync;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.ramy.minervue.app.MainService;
import com.ramy.minervue.util.ATask;
import com.ramy.minervue.util.PreferenceUtil;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 11/25/13.
 */
public class SyncTask extends ATask<Void, Void, Boolean> {

    private static final String TAG = "RAMY-SyncTask";
    private static final String LOGIN_NAME = "ramy";
    private static final String LOGIN_PASS = "ramy";
    private static final String DIR_INCOMING = "incoming";

    private FTPClient client = null;
    private String serverAddr;
    private LocalFileUtil localFileUtil;
    private boolean hasTask = false;

    public SyncTask(LocalFileUtil localFileUtil, String serverAddr) {
        this.serverAddr = serverAddr;
        this.localFileUtil = localFileUtil;
    }

    private HashMap<String, Long> toFileMap(FTPFile[] files) {
        HashMap<String, Long> ret = new HashMap<String, Long>(files.length);
        if (files != null) {
            for (FTPFile file : files) {
                if (file != null && file.isFile()) {
                    ret.put(file.getName(), file.getSize());
                }
            }
        }
        return ret;
    }

    private HashMap<String, Long> toFileMap(File[] files) {
        HashMap<String, Long> ret = new HashMap<String, Long>(files.length);
        if (files != null) {
            for (File file : files) {
                if (file != null && file.isFile() && !LocalFileUtil.isFileInUse(file)) {
                    ret.put(file.getName(), file.length());
                }
            }
        }
        return ret;
    }

    private List<Task> generateTasks(HashMap<String, Long> from, HashMap<String, Long> to) {
        LinkedList<Task> ret = new LinkedList<Task>();
        for (String file : from.keySet()) {
            Long toSize = to.get(file);
            if (toSize != null && toSize.equals(from.get(file))) {
                continue;
            }
            toSize = to.get(file + ".tmp");
            if (toSize == null) {
                ret.add(new Task(file, 0));
            } else {
                if (from.get(file).compareTo(toSize) > 0) {
                    ret.add(new Task(file, toSize));
                }
            }
        }
        if (!ret.isEmpty()) {
            hasTask = true;
        }
        return ret;
    }

    private boolean downloadDirectory(String dirName) {
        if (!makeEnter(dirName)) {
            disconnect();
            return false;
        }
        try {
            HashMap<String, Long> localMap = toFileMap(localFileUtil.listFiles(dirName));
            HashMap<String, Long> remoteMap = toFileMap(client.listFiles());
            List<Task> tasks = generateTasks(remoteMap, localMap);
            for (Task task : tasks) {
                download(dirName, task);
            }
        } catch (IOException e) {
            disconnect();
            return false;
        }
        return enterParent();
    }

    private void download(String dirName, Task task) throws IOException {
        File file = new File(localFileUtil.getDir(dirName), task.fileName + ".tmp");
        client.setFileType(FTP.BINARY_FILE_TYPE);
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
        if (task.offset > 0) {
            client.setRestartOffset(task.offset);
        }
        client.enterLocalPassiveMode();
        client.retrieveFile(task.fileName, output);
        output.close();
        client.deleteFile(task.fileName);
        file.renameTo(new File(localFileUtil.getDir(dirName), task.fileName));
        PreferenceUtil util = MainService.getInstance().getPreferenceUtil();
        util.setUnreadDownload(util.getUnreadDownload(null) + 1);
        Log.v(TAG, "Downloaded: " + task.fileName + ".");
    }

    private boolean uploadDirectory(String dirName) {
        if (!makeEnter(dirName)) {
            disconnect();
            return false;
        }
        try {
            HashMap<String, Long> localMap = toFileMap(localFileUtil.listFiles(dirName));
            HashMap<String, Long> remoteMap = toFileMap(client.listFiles());
            Iterator<Map.Entry<String, Long>> iter = localMap.entrySet().iterator();
            while (iter.hasNext()) {
                String file = iter.next().getKey();
                if (file.charAt(1) != '-') {
                    Log.v(TAG, "Ignore file without priority: " + file + ".");
                    iter.remove();
                }
            }
            List<Task> tasks = generateTasks(localMap, remoteMap);
            for (Task task : tasks) {
                upload(dirName, task);
            }
        } catch (IOException e) {
            disconnect();
            return false;
        }
        return enterParent();
    }

    private void upload(String dirName, Task task) throws IOException {
        File file = new File(localFileUtil.getDir(dirName), task.fileName);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        if (task.offset > 0) {
            input.skip(task.offset);
            client.setRestartOffset(task.offset);
        }
        client.enterLocalPassiveMode();
        client.storeFile(task.fileName + ".tmp", input);
        client.rename(task.fileName + ".tmp", task.fileName);
        input.close();
        Log.v(TAG, "Uploaded: " + task.fileName + ".");
    }

    private boolean connect() {
        for (int i = 0; ; ++i) {
            try {
                client = new FTPClient();
                client.setControlEncoding("UTF-8");
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
            if (i < 3) {
                Log.i(TAG, "Retry in 3 seconds...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return false;
                }
            } else {
                return false;
            }
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

    private boolean enterParent() {
        try {
            client.changeToParentDirectory();
            return true;
        } catch (IOException e) {
            disconnect();
            return false;
        }
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

    @Override
    protected Boolean doInBackground(Void... params) {
        if (!connect()) {
            Log.e(TAG, "Failed to connect.");
            return false;
        }
        String uuid = MainService.getInstance().getUUID();
        if (!makeEnter(DIR_INCOMING) || !makeEnter(uuid)) {
            Log.e(TAG, "Failed to enter user directory.");
            return false;
        }
        do {
            hasTask = false;
            if (!downloadDirectory(LocalFileUtil.DIR_PUB)) {
                Log.e(TAG, "Sync failed.");
                return false;
            }
            if (!uploadDirectory(LocalFileUtil.DIR_IMAGE)
                    || !uploadDirectory(LocalFileUtil.DIR_SENSOR)
                    || !uploadDirectory(LocalFileUtil.DIR_VIDEO)) {
                Log.e(TAG, "Sync failed.");
                return false;
            }
        } while (hasTask);
        Log.v(TAG, "Sync succeeded.");
        disconnect();
        return true;
    }

    private class Task {

        public String fileName;
        public long offset;

        public Task(String fileName, long offset) {
            this.fileName = fileName;
            this.offset = offset;
        }

    }

}

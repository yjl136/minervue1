package com.ramy.minervue.sync;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import com.ramy.minervue.R;
import com.ramy.minervue.app.MainService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by peter on 10/23/13.
 */
public class LocalFileUtil {

	private static final String TAG = "RAMY-LocalFileUtil";

	public static final String DIR_PUB = "pub";
	public static final String DIR_IMAGE = "image";
	public static final String DIR_VIDEO = "video";
	public static final String DIR_SENSOR = "sensor";
	public static final long ONE_KB = 1024;
	public static final long ONE_MB = ONE_KB * ONE_KB;
	public static final long ONE_GB = ONE_KB * ONE_MB;

	private File root;

	public LocalFileUtil(Context context) {
		root = context.getExternalFilesDir(null);

	}
	public boolean isSensorDir(File file){
		return new File(root,DIR_SENSOR).equals(file);
	}
	public boolean isImageDir(File file){
		return new File(root,DIR_IMAGE).equals(file);
	}
	public boolean isVideoDir(File file){
		return new File(root,DIR_VIDEO).equals(file);
	}
	public boolean isRootDir(File file){
		return root.equals(file);

	}
	public File[] listFiles(String dirName) {
		makeDir(dirName);
		File file = new File(root, dirName);
		return file.listFiles();
	}

	public boolean makeDir(String dirName) {
		File file = new File(root, dirName);
		return file.mkdirs() || file.isDirectory();
	}

	public String setPriority(String path, int priority) {
		File from = new File(path);
		File to = new File(from.getParentFile(), priority + "-" + from.getName());
		from.renameTo(to);
		return to.getPath();
	}

	public File getFile(String dirName, String file) {
		makeDir(dirName);
		return new File(new File(root, dirName), file);
	}

	private String getTime() {
		long offset = MainService.getInstance().getPreferenceUtil().getTimeOffset(null);
		Date date = new Date(System.currentTimeMillis() + offset);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		MainService.getInstance().getUUID();
		return MainService.getInstance().getUUID()+"-"+sdf.format(date);
	}

	private String buildName(String dirName, String suffix) {
		return buildName(dirName, null, suffix);
	}
	 public String generateSensorTxtFilename() {
	    	makeDir(DIR_SENSOR);
	    	return buildName(DIR_SENSOR, ".txt");
	    }
	private String buildName(String dirName, String priority, String suffix) {
		StringBuilder builder = new StringBuilder();
		builder.append(root.getPath()).append(File.separator)
		.append(dirName).append(File.separator);
		if (priority != null) {
			builder.append(priority).append("-");
		}
		return builder.append(getTime()).append(suffix).toString();
	}

	public String generateVideoFilename() {
		makeDir(DIR_VIDEO);
		return buildName(DIR_VIDEO, ".mp4");
	}

	public String generateVideoFilename(int priority) {
		makeDir(DIR_VIDEO);
		return buildName(DIR_VIDEO, Integer.toString(priority), ".mp4");
	}

	public String generateImageFilename() {
		makeDir(DIR_IMAGE);
		return buildName(DIR_IMAGE, ".jpg");
	}

	public String generateSensorFilename() {
		makeDir(DIR_SENSOR);
		return buildName(DIR_SENSOR, ".jpg");
	}


	public boolean isPubDir(File file) {
		return new File(root, DIR_PUB).equals(file);
	}

	public File getDir(String dirName) {
		return new File(root, dirName);
	}

	public File getRoot() {
		return root;
	}

	public static boolean isFileInUse(File file) {
		FileChannel channel = null;
		try {
			channel = new RandomAccessFile(file, "rw").getChannel();
		} catch (FileNotFoundException e) {
			return true;
		}
		FileLock lock = null;
		try {
			lock = channel.tryLock();
			return lock == null;
		} catch (IOException e) {
			return true;
		} finally {
			try {
				if (lock != null) {
					lock.release();
				}
			} catch (IOException e) {
				// Expected.
			}
			try {
				channel.close();
			} catch (IOException e) {
				// Expected.
			}
		}
	}

	public static String toReadableSize(long size) {
		String displaySize;
		if (size / ONE_GB > 0) {
			displaySize = String.valueOf(size / ONE_GB) + " GB";
		} else if (size / ONE_MB > 0) {
			displaySize = String.valueOf(size / ONE_MB) + " MB";
		} else if (size / ONE_KB > 0) {
			displaySize = String.valueOf(size / ONE_KB) + " KB";
		} else {
			displaySize = String.valueOf(size) + " Bytes";
		}
		return displaySize;
	}

	public static boolean delete(File file) {
		boolean ret = true;
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				ret = ret & delete(child);
			}
		}
		return ret & file.delete();
	}

}

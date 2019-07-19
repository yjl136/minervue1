package com.ramy.minervue.camera;

import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.ramy.minervue.app.MainService;
import com.ramy.minervue.camera.color.ColorUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by peter on 11/30/13.
 */
public class MyCamera implements Camera.PreviewCallback {

	public static final int PREVIEW_FORMAT = ImageFormat.YV12;

	private static final String TAG = "RAMY-MyCamera";

	private int id;
	private Camera camera = null;
	private CameraZoomer zoomer;
	private boolean isPreviewing = false;
	private boolean isSavingPicture = false;
	private int fpsRange[] = new int[2];
	private Camera.Size currentSize;
	private int currentFrameSize;
	private PreviewCallback previewConsumer;

	public static MyCamera openDefaultCamera(int preferredWidth,
			int preferredHeight) {
		MyCamera myCamera = openBackCamera(preferredWidth, preferredHeight);
		if (myCamera == null) {
			myCamera = openFrontCamera(preferredWidth, preferredHeight);
		}
		return myCamera;
	}

	public static MyCamera openFrontCamera(int preferredWidth,
			int preferredHeight) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		int count = Camera.getNumberOfCameras();
		for (int i = 0; i < count; ++i) {
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				try {
					return new MyCamera(i, preferredWidth, preferredHeight);
				} catch (RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}

	public static MyCamera openBackCamera(int preferredWidth,
			int preferredHeight) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		int count = Camera.getNumberOfCameras();
		for (int i = 0; i < count; ++i) {
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				try {
					return new MyCamera(i, preferredWidth, preferredHeight);
				} catch (RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}

	public static MyCamera reopenCamera(MyCamera last, int preferredWidth,
			int preferredHeight) {
		int lastId = last.id;
		last.release();
		try {
			return new MyCamera(lastId, preferredWidth, preferredHeight);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static MyCamera nextCamera(MyCamera last, int preferredWidth,
			int preferredHeight) {
		int newId = (last.id + 1) % Camera.getNumberOfCameras();
		last.release();
		try {
			return new MyCamera(newId, preferredWidth, preferredHeight);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public int getCurrentFrameSize() {
		return currentFrameSize;
	}

	private MyCamera(int id, int preferredWidth, int preferredHeight)
			throws RuntimeException {
		this.id = id;
		camera = Camera.open(id);
		zoomer = new CameraZoomer(camera);
		Comparator<Camera.Size> comparator;
		if (preferredWidth != 0 && preferredHeight != 0) {
			comparator = new MatchComparator(preferredWidth, preferredHeight);
		} else {
			comparator = new MaxComparator();
		}
		Camera.Parameters param = camera.getParameters();
		// First, some info we need.
		param.getPreviewFpsRange(fpsRange);
		fpsRange[0] /= 1000;
		fpsRange[1] /= 1000;
		// Second, some configuration.
		param.setPreviewFormat(PREVIEW_FORMAT);
		param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		// Third, best jpeg quality.
		Camera.Size size = Collections.max(param.getSupportedPictureSizes(),
				new MaxComparator());
		param.setPictureSize(size.width, size.height);
		param.setJpegQuality(100);
		// Fourth, video quality.
		currentSize = Collections.max(param.getSupportedPreviewSizes(),
				comparator);
		currentFrameSize = ColorUtil.getFrameSize(currentSize, PREVIEW_FORMAT);
		param.setPreviewSize(currentSize.width, currentSize.height);
		// Now configure.
		camera.setParameters(param);
		// Get ready for buffer.
		camera.setPreviewCallbackWithBuffer(this);
		for (int i = 0; i < 4; ++i) {
			camera.addCallbackBuffer(new byte[currentFrameSize]);
		}
	}

	public void setPreviewCallback(PreviewCallback callback) {
		if (camera != null) {
			previewConsumer = callback;
		}
	}

	public boolean isFront() {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(id, info);
		return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
	}

	public boolean startPreview(SurfaceTexture surface) {
		if (camera != null) {
			try {
				camera.setPreviewTexture(surface);
				camera.startPreview();
				isPreviewing = true;
				return true;
			} catch (IOException e) {
				Log.e(TAG, "Error setting texture.");
			}
		}
		return false;
	}

	public int[] getFpsRange() {
		return fpsRange;
	}

	public List<Camera.Size> getPreviewSizes() {
		if (camera != null) {
			return camera.getParameters().getSupportedPreviewSizes();
		} else {
			return null;
		}
	}

	public Camera.Size getCurrentPreviewSize() {
		return currentSize;
	}

	public void release() {
		if (camera == null) {
			return;
		}
		if (isPreviewing) {
			camera.stopPreview();
			try {
				camera.setPreviewTexture(null);
			} catch (IOException e) {
				Log.e(TAG, "Error releasing texture.");
			}
			camera.setPreviewCallbackWithBuffer(null);
			isPreviewing = false;
		}
		camera.release();
		camera = null;
	}

	public boolean isPreviewing() {
		return isPreviewing;
	}

	public void zoom(int percent) {
		if (camera != null) {
			zoomer.zoomTo(percent);
		}
	}

	public void takePicture(final PictureCallback callback) {
		if (camera != null && !isSavingPicture) {
			isSavingPicture = true;
			camera.takePicture(null, null, new Camera.PictureCallback() {
				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					callback.onPictureTaken(data);
					isSavingPicture = false;
					camera.startPreview();
				}
			});
		}
	}

	public void setLED(boolean isChecked) {
		if (camera != null) {
			Camera.Parameters param = camera.getParameters();
			if (isChecked) {
				param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			} else {
				param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			}
			camera.setParameters(param);
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (previewConsumer != null) {
			previewConsumer.onPreviewFrame(data);
		}
		camera.addCallbackBuffer(data);
	}

	private class MatchComparator implements Comparator<Camera.Size> {

		private int preferredWidth;
		private int preferredHeight;

		private int getDelta(Camera.Size size) {
			return Math.abs(preferredWidth - size.width)
					+ Math.abs(preferredHeight - size.height);
		}

		public MatchComparator(int preferredWidth, int preferredHeight) {
			this.preferredWidth = preferredWidth;
			this.preferredHeight = preferredHeight;
		}

		@Override
		public int compare(Camera.Size lhs, Camera.Size rhs) {
			return getDelta(rhs) - getDelta(lhs);
		}

	}

	private class MaxComparator implements Comparator<Camera.Size> {

		@Override
		public int compare(Camera.Size lhs, Camera.Size rhs) {
			return lhs.width * lhs.height - rhs.width * rhs.height;
		}

	}

	public interface PreviewCallback {

		void onPreviewFrame(byte[] data);

	}

	public interface PictureCallback {

		public void onPictureTaken(byte[] data);

	}

}

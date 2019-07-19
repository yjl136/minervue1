package com.ramy.minervue.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.ramy.minervue.camera.color.YUV420Converter;
import com.ramy.minervue.camera.MyCamera;
import com.ramy.minervue.util.ATask;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by peter on 10/24/13.
 */
public class VideoCodec implements MyCamera.PreviewCallback, VideoFrameProvider {

	private static final String TAG = "RAMY-VideoCodec";

	private static final int TIME_OUT = 100000;
	private static final int COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;

	private Camera.Size currentSize;
	private boolean isCapturing = false;
	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers = null;
	private MyCamera camera;
	private MediaCodec codec = null;
	private FrameFetcher frameFetcher = null;
	private YUV420Converter colorConverter;
	private ArrayList<VideoFrameConsumer> consumerList = new ArrayList<VideoFrameConsumer>();

	public MyCamera getCurrentCamera() {
		return camera;
	}

	@Override
	public int[] getFpsRange() {
		return camera.getFpsRange();
	}

	@Override
	public Camera.Size getSize() {
		return currentSize;
	}

	@Override
	public void addConsumer(VideoFrameConsumer consumer) {
		if (consumer != null) {
			consumerList.add(consumer);
		}
	}

	public Matrix getMatrixFor(int targetWidth, int targetHeight) {
		Matrix transform = new Matrix();
		float fromWHR = (float) currentSize.width / currentSize.height;
		float toWHR = (float) targetWidth / targetHeight;
		if (fromWHR > toWHR) {
			transform.setScale(1, toWHR / fromWHR, targetWidth / 2,
					targetHeight / 2);
		} else {
			transform.setScale(fromWHR / toWHR, 1, targetWidth / 2,
					targetHeight / 2);
		}
		return transform;
	}

	private boolean initCodec() {
		codec = MediaCodec.createEncoderByType("video/avc");
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",
				currentSize.width, currentSize.height);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT);
		int bitRate = camera.getFpsRange()[1] * currentSize.width
				* currentSize.height / 15;
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,
				camera.getFpsRange()[1]);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
		mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,
				camera.getCurrentFrameSize());
		try {
			codec.configure(mediaFormat, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE);
		} catch (IllegalStateException e) {
			codec.release();
			codec = null;
			return false;
		}
		return true;
	}

	private void releaseCodec() {
		if (codec != null) {
			codec.release();
			codec = null;
		}
	}

	/**
	 * 
	 * @param surface
	 *            Surface to preview.
	 * @param preferredWidth
	 *            Set to 0 if maximum is desired.
	 * @param preferredHeight
	 *            Set to 0 if maximum is desired.
	 */
	public boolean startPreview(SurfaceTexture surface, int preferredWidth,
			int preferredHeight) {
		if (camera == null) {
			camera = MyCamera
					.openDefaultCamera(preferredWidth, preferredHeight);
		} else {
			camera = MyCamera.reopenCamera(camera, preferredWidth,
					preferredHeight);
		}
		if (camera != null) {
			currentSize = camera.getCurrentPreviewSize();
			camera.setPreviewCallback(this);
			return camera.startPreview(surface);
		}
		return false;
	}

	public boolean isPreviewing() {
		return camera != null;
	}

	public void startCapture() {
		if (!isCapturing && initCodec()) {
			codec.start();
			inputBuffers = codec.getInputBuffers();
			outputBuffers = codec.getOutputBuffers();
			frameFetcher = new FrameFetcher();
			frameFetcher.start();
			colorConverter = YUV420Converter.create(MyCamera.PREVIEW_FORMAT,
					currentSize);
			isCapturing = true;
			Log.i(TAG, "Start capture.");
		}
	}

	public void stopCapture() {
		if (isCapturing) {
			int index = codec.dequeueInputBuffer(-1);
			codec.queueInputBuffer(index, 0, 0, System.nanoTime() / 1000,
					MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			frameFetcher.clear();
			codec.stop();
			releaseCodec();
			consumerList.clear();
			isCapturing = false;
			Log.i(TAG, "Stop capture.");
		}
	}

	public void stopPreview() {
		if (camera != null) {
			camera.release();
			camera = null;
			Log.i(TAG, "Camera released.");
		}
	}

	public boolean isCapturing() {
		return isCapturing;
	}

	public boolean switchCamera(SurfaceTexture surface, int preferredWidth,
			int preferredHeight) {
		if (camera != null) {
			camera = MyCamera.nextCamera(camera, preferredWidth,
					preferredHeight);
			if (camera != null) {
				currentSize = camera.getCurrentPreviewSize();
				camera.setPreviewCallback(this);
				return camera.startPreview(surface);
			}
		}
		return false;
	}

	@Override
	public void onPreviewFrame(byte[] data) {
		if (isCapturing) {
			int index = codec.dequeueInputBuffer(TIME_OUT);
			if (index >= 0) {
				inputBuffers[index].clear();

				colorConverter.convert(data);
				inputBuffers[index].put(data, 0,
						this.camera.getCurrentFrameSize());
				codec.queueInputBuffer(index, 0,
						this.camera.getCurrentFrameSize(),
						System.nanoTime() / 1000, 0);
			}
		}
	}

	public class FrameFetcher extends ATask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			while (true) {
				int index = codec.dequeueOutputBuffer(info, TIME_OUT);
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					codec.releaseOutputBuffer(index, false);
					break;
				}
				if (index >= 0) {
					ByteBuffer b = outputBuffers[index];
					for (VideoFrameConsumer consumer : consumerList) {
						consumer.addVideoFrame(b, info);
					}
					codec.releaseOutputBuffer(index, false);
				} else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					outputBuffers = codec.getOutputBuffers();
				}
			}
			return null;
		}

	}

}

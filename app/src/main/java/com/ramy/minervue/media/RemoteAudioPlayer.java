package com.ramy.minervue.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.ramy.minervue.util.ATask;
import com.ramy.minervue.util.NetUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by peter on 12/6/13.
 */
public class RemoteAudioPlayer {

	private static final String TAG = "RAMY-RemoteAudioPlayer";

	private static final int TIME_OUT = 100000;

	public static final int RATE = 44100;
	public static final int CHANNEL_COUNT = 1;
	private static final int INPUT_SIZE = 2048;

	private boolean isCapturing = false;
	private MediaCodec codec = null;
	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	private Receiver receiver = null;
	private PCMPlayer player = null;
	private Monitor monitor = null;
	private AudioManager am;

	public RemoteAudioPlayer(Monitor monitor, AudioManager am) {
		this.monitor = monitor;
		this.am = am;
	}

	private boolean initCodec() {
		codec = MediaCodec.createDecoderByType("audio/mp4a-latm");
		MediaFormat mediaFormat = MediaFormat.createAudioFormat(
				"audio/mp4a-latm", RATE, CHANNEL_COUNT);
		mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, INPUT_SIZE);
		mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
		mediaFormat.setByteBuffer("csd-0",
				ByteBuffer.wrap(new byte[] { (byte) 0x12, (byte) 0x08 }));
		try {
			codec.configure(mediaFormat, null, null, 0);
		} catch (IllegalStateException e) {
			releaseCodec();
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

	public void startPlayback(int port) {
		if (!isCapturing && initCodec()) {
			codec.start();
			inputBuffers = codec.getInputBuffers();
			outputBuffers = codec.getOutputBuffers();
			receiver = new Receiver();
			receiver.start(port);
			player = new PCMPlayer();
			player.start();
			Log.i(TAG, "Start playback.");
			isCapturing = true;
		}
	}

	public void stopPlayback() {
		if (isCapturing) {
			receiver.cancelAndClear(true);
			Log.i(TAG, "Stop receiving audio.");
			player.cancelAndClear(true);
			codec.stop();
			releaseCodec();
			Log.i(TAG, "Stop playback.");
			isCapturing = false;
		}
	}

	public boolean isCapturing() {
		return isCapturing;
	}

	private class Receiver extends ATask<Integer, Void, Void> {

		private ByteBuffer buffer = ByteBuffer.wrap(new byte[INPUT_SIZE]);
		private ServerSocketChannel serverSocket = null;
		private SocketChannel socket = null;

		private void receiveADTS() throws IOException {
			buffer.clear();//将缓冲区的mark置为-1，position置为0，limit置为缓冲区容量大小（capacity）。
			buffer.limit(4);//将缓冲区的limit重置为4。
			while (buffer.hasRemaining()) {//position和limit之间是否还有元素
				socket.read(buffer);
			}
			buffer.flip();//将limit置为position位置，将position置为0，mark置为-1位置
			int size = buffer.getInt();
			buffer.clear();
			buffer.limit(size);
			while (buffer.hasRemaining()) {
				socket.read(buffer);
			}
			int index = codec.dequeueInputBuffer(TIME_OUT);
			if (index >= 0) {
				inputBuffers[index].clear();
				buffer.flip();
				inputBuffers[index].put(buffer);
				codec.queueInputBuffer(index, 0, size,
						System.nanoTime() / 1000, 0);
			}
		}

		@Override
		protected Void doInBackground(Integer... params) {
			int port = params[0];
			serverSocket = NetUtil.openServerSocketChannel(port);
			if (serverSocket == null) {
				return super.doInBackground();
			}
			Log.i(TAG, "Start waiting at " + port + ".");
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				return super.doInBackground();
			} finally {
				NetUtil.cleanSocket(serverSocket);
			}
			Log.i(TAG, "Remote connected, playback pending.");
			try {
				while (!isCancelled()) {
					receiveADTS();
				}
			} catch (IOException e) {
				return super.doInBackground();
			} finally {
				NetUtil.cleanSocket(socket);
			}
			return super.doInBackground();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			monitor.abort();
		}

	}

	private class PCMPlayer extends ATask<Void, Void, Void> {

		private AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
				44100, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, INPUT_SIZE,
				AudioTrack.MODE_STREAM);
		private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		private byte[] buffer = new byte[INPUT_SIZE];

		private void playFrameFromCodec() {
			int index = codec.dequeueOutputBuffer(info, TIME_OUT);
			if (index >= 0) {
				outputBuffers[index].position(info.offset);
				outputBuffers[index].limit(info.size + info.offset);
				outputBuffers[index].get(buffer, 0, info.size);
				codec.releaseOutputBuffer(index, false);
				track.write(buffer, 0, info.size);
			} else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				outputBuffers = codec.getOutputBuffers();
			}
		}

		@Override
		protected void onPreExecute() {
			// track.setStereoVolume(100, 100);
			am.setStreamVolume(AudioManager.STREAM_MUSIC,
					am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
					AudioManager.FLAG_PLAY_SOUND);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			track.play();
			while (!isCancelled()) {
				playFrameFromCodec();
			}
			track.stop();
			return super.doInBackground();
		}

	}

}

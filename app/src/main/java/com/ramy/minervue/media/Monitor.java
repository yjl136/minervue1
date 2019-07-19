package com.ramy.minervue.media;

import android.media.AudioManager;
import android.media.MediaCodec;
import android.util.Log;
import android.view.TextureView;

import com.ramy.minervue.app.MainService;
import com.ramy.minervue.util.ATask;
import com.ramy.minervue.util.ByteBufferPool;
import com.ramy.minervue.util.ConfigUtil;
import com.ramy.minervue.util.NetUtil;
import com.ramy.minervue.util.PreferenceUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by peter on 12/29/13.
 */
public class Monitor extends Recorder {

    private static final String TAG = "RAMY-Monitor";
    
    private RemoteAudioPlayer audioPlayer;
    private VideoSender vSender;
    private AudioSender aSender;
    private OnAbortListener listener = null;

    public Monitor(AudioManager am) {
    	audioPlayer = new RemoteAudioPlayer(this,am);
	}

	public void prepare() {
        int port = MainService.getInstance().getConfigUtil().getPortAudio();
        audioPlayer.startPlayback(port);
    }

    public void reset() {
        audioPlayer.stopPlayback();
    }

    public void setOnAbortListener(OnAbortListener listener) {
        this.listener = listener;
    }

    @Override
    public void start(String recordFile) {
        ConfigUtil util = MainService.getInstance().getConfigUtil();
        String server = util.getServerAddr();
        int vPort = util.getPortVideo();
        int aPort = util.getPortAudio();
        vSender = new VideoSender(server, vPort);
        aSender = new AudioSender(server, aPort);
        vSender.start();
        aSender.start();
        videoCodec.addConsumer(vSender);
        audioCodec.addConsumer(aSender);
        super.start(recordFile);
    }

    @Override
    public void stop() {
        super.stop();
        audioPlayer.stopPlayback();
        vSender.cancelAndClear(true);
        aSender.cancelAndClear(true);
    }

    public void abort() {
        if (listener != null) {
        	stop();
            listener.onAbort();
        }
    }

    public interface OnAbortListener {

        public void onAbort();

    }

    private abstract class AbstractMediaSender extends ATask<Void, Void, Void> {

        private static final String TAG = "RAMY-AbstractMediaSender";

        protected SocketChannel socket;
        protected SocketAddress remoteAddr;
        protected ByteBufferPool pool = new ByteBufferPool(64);

        protected final LinkedBlockingQueue<ByteBuffer> list = new LinkedBlockingQueue<ByteBuffer>(256);

        private ByteBuffer size = ByteBuffer.allocate(4);

        public AbstractMediaSender(String remoteAddr, int port) {
            this.remoteAddr = new InetSocketAddress(remoteAddr, port);
        }

        @Override
        protected Void doInBackground(Void... params) {
            pool.fillPool(8, getInitBufferSize());
            try {
                socket = SocketChannel.open(remoteAddr);
                Log.v(TAG, "Remote connected, sending in progress.");
                while (!isCancelled()) {
                    synchronized (list) {
                        ByteBuffer buffer = list.take();
                        socket.write(buffer);
                        pool.queue(buffer);
                    }
                }
            } catch (Exception e) {
                // Ignored.
            } finally {
                NetUtil.cleanSocket(socket);
            }
            Log.i(TAG, "MediaSender (" + getClass().getSimpleName() + ") stopped.");
            return super.doInBackground();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.e(TAG, "Unexpected termination, aborting.");
            abort();
        }

        public abstract int getInitBufferSize();

    }

    public class VideoSender extends AbstractMediaSender implements VideoFrameConsumer {

        public VideoSender(String remoteAddr, int port) {
            super(remoteAddr, port);
        }

        @Override
        public void addVideoFrame(ByteBuffer data, MediaCodec.BufferInfo info) {
            ByteBuffer buffer = pool.dequeue(info.size + 4);
            if (buffer == null) {
                return;
            }
            data.position(info.offset);
            data.limit(info.size + info.offset);
            buffer.putInt(info.size);
            buffer.put(data);
            buffer.flip();
            list.offer(buffer);
        }

        @Override
        public int getInitBufferSize() {
            return 128 * 1024;
        }

    }

    public class AudioSender extends AbstractMediaSender implements AudioFrameConsumer {

        public AudioSender(String remoteAddr, int port) {
            super(remoteAddr, port);
        }

        @Override
        public void addAudioFrame(byte[] adtsHeader, ByteBuffer data, MediaCodec.BufferInfo info) {
            ByteBuffer buffer = pool.dequeue(info.size + adtsHeader.length + 4);
            if (buffer == null) {
                return;
            }
            data.position(info.offset);
            data.limit(info.size + info.offset);
            buffer.putInt(info.size + adtsHeader.length);
            buffer.put(adtsHeader);
            buffer.put(data);
            buffer.flip();
            list.offer(buffer);
        }

        @Override
        public int getInitBufferSize() {
            return 1024;
        }

    }

}

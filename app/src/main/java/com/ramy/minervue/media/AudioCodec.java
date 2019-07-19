package com.ramy.minervue.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.ramy.minervue.util.ATask;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by peter on 10/24/13.
 */
public class AudioCodec implements AudioFrameProvider {

    public static final int RATE = 44100;
    public static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int FORMAT_BYTES = 2;
    public static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int CHANNEL_COUNT = 1;
    public static final int PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    public static final int INPUT_SIZE = 2048;
    public static final int BIT_RATE = 54 * 1024;

    private static final int BYTES_PER_SEC = RATE * CHANNEL_COUNT * FORMAT_BYTES;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final String TAG = "RAMY-AudioCodec";

    private static final int TIME_OUT = 100000;

    private boolean isCapturing = false;
    private MediaCodec codec;
    private AudioRecord record;
    private PCMFetcher pcmFetcher;
    private FrameFetcher frameFetcher;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private ArrayList<AudioFrameConsumer> consumerList = new ArrayList<AudioFrameConsumer>();

    public boolean isCapturing() {
        return isCapturing;
    }

    @Override
    public void addConsumer(AudioFrameConsumer consumer) {
        if (consumer != null) {
            consumerList.add(consumer);
        }
    }

    @Override
    public int getSampleRate() {
        return RATE;
    }

    @Override
    public int getChannelCount() {
        return CHANNEL_COUNT;
    }

    @Override
    public int getSampleSizeInBits() {
        return FORMAT_BYTES * 8;
    }

    @Override
    public int getBitRate() {
        return BIT_RATE;
    }

    @Override
    public int getProfile() {
        return PROFILE;
    }

    private boolean initCodec() {
        int size = AudioRecord.getMinBufferSize(RATE, CHANNEL_IN, FORMAT);
        size = Math.max(size, BYTES_PER_SEC * 10);
        record = new AudioRecord(AUDIO_SOURCE, RATE, CHANNEL_IN, FORMAT, size);
        codec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(
                "audio/mp4a-latm", RATE, CHANNEL_COUNT);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, CHANNEL_OUT);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, INPUT_SIZE);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, PROFILE);
        try {
            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            releaseCodec();
            return false;
        }
        return record.getState() == AudioRecord.STATE_INITIALIZED;
    }

    private void releaseCodec() {
        if (record != null) {
            record.release();
            record = null;
        }
        if (codec != null) {
            codec.release();
            codec = null;
        }
    }

    public void startCapture() {
        if (!isCapturing && initCodec()) {
            codec.start();
            inputBuffers = codec.getInputBuffers();
            outputBuffers = codec.getOutputBuffers();
            frameFetcher = new FrameFetcher();
            frameFetcher.start();
            pcmFetcher = new PCMFetcher();
            pcmFetcher.start();
            isCapturing = true;
            Log.i(TAG, "Start capture.");
        }
    }

    public void stopCapture() {
        if (isCapturing) {
            pcmFetcher.cancelAndClear(false);
            frameFetcher.clear();
            codec.stop();
            releaseCodec();
            consumerList.clear();
            isCapturing = false;
            Log.i(TAG, "Stop capture");
        }
    }

    private class FrameFetcher extends ATask<Void, Void, Void> {

        private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        private byte[] header = new byte[] {
                (byte) 0xFF,
                (byte) 0xF9,
                (byte)(((PROFILE - 1) << 6) | (4 << 2) | (CHANNEL_COUNT >>> 2)),    // Assume 44100.
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0xFC,
        };

        private void setADTSHeader(int size) {
            header[3] = (byte)(((CHANNEL_COUNT & 0x03) << 6) | (size >>> 11));
            header[4] = (byte)((size & 0x7FF) >>> 3);
            header[5] = (byte)(((size & 0x07) << 5) | 0x1F);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                int index = codec.dequeueOutputBuffer(info, TIME_OUT);
                if (index >= 0) {
                    setADTSHeader(info.size + header.length);
                    for (AudioFrameConsumer consumer : consumerList) {
                        consumer.addAudioFrame(header, outputBuffers[index], info);
                    }
                    codec.releaseOutputBuffer(index, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break;
                    }
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = codec.getOutputBuffers();
                }
            }
            return null;
        }

    }

    private class PCMFetcher extends ATask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            record.startRecording();
            while (true) {
                int index = codec.dequeueInputBuffer(TIME_OUT);
                if (index >= 0) {
                    inputBuffers[index].clear();
                    long time = System.nanoTime() / 1000;
                    record.read(inputBuffers[index], INPUT_SIZE);
                    if (isCancelled()) {
                        codec.queueInputBuffer(index, 0, INPUT_SIZE, time,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    } else {
                        codec.queueInputBuffer(index, 0, INPUT_SIZE, time, 0);
                    }
                }
            }
            record.stop();
            return super.doInBackground();
        }

    }

}

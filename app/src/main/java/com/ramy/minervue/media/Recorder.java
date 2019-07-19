package com.ramy.minervue.media;
import com.ramy.minervue.ffmpeg.MP4Muxer;


public class Recorder {

    protected VideoCodec videoCodec = new VideoCodec();
    protected AudioCodec audioCodec = new AudioCodec();
    protected MP4Muxer muxer = null;
    protected boolean isCapturing = false;

    public VideoCodec getVideoCodec() {
        return videoCodec;
    }

    public AudioCodec getAudioCodec() {
        return audioCodec;
    }

    public MP4Muxer getMuxer() {
        return muxer;
    }

    public void start(String recordFile) {
        if (!isCapturing) {
            muxer = new MP4Muxer(recordFile, videoCodec, audioCodec);
            videoCodec.startCapture();
            audioCodec.startCapture();
            isCapturing = true;
        }
    }

    public void stop() {
        if (isCapturing) {
            videoCodec.stopCapture();
            audioCodec.stopCapture();
            muxer.finish();
            isCapturing = false;
        }
    }

    public boolean isCapturing() {
        return isCapturing;
    }

}

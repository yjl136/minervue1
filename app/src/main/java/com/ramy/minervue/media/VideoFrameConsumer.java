package com.ramy.minervue.media;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by peter on 11/5/13.
 */
public interface VideoFrameConsumer {

    public void addVideoFrame(ByteBuffer data, MediaCodec.BufferInfo info);

}

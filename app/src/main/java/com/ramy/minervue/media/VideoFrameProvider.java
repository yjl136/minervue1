package com.ramy.minervue.media;

import android.hardware.Camera;

/**
 * Created by peter on 11/5/13.
 */
public interface VideoFrameProvider {

    public int[] getFpsRange();

    public Camera.Size getSize();
    
    public void addConsumer(VideoFrameConsumer consumer);

}

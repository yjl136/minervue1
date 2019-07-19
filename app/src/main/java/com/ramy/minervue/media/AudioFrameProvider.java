package com.ramy.minervue.media;

/**
 * Created by peter on 11/5/13.
 */
public interface AudioFrameProvider {

    public int getSampleRate();

    public int getChannelCount();

    public int getSampleSizeInBits();

    public int getProfile();

    public int getBitRate();

    public void addConsumer(AudioFrameConsumer consumer);

}

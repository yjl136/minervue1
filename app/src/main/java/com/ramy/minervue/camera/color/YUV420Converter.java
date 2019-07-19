package com.ramy.minervue.camera.color;

import android.graphics.ImageFormat;
import android.hardware.Camera;

/**
 * Created by peter on 11/1/13.
 */
public abstract class YUV420Converter {

    protected Camera.Size size;

    public YUV420Converter(Camera.Size size) {
        this.size = size;
    }

    public static YUV420Converter create(int fromFormat, Camera.Size size) {
        switch (fromFormat) {
            case ImageFormat.YV12:
                return new YV12ToYUV420Converter(size);
            default:
                throw new UnsupportedOperationException("Not supported converter.");
        }
    }

    public abstract void convert(byte[] data);

}

package com.ramy.minervue.camera.color;

import android.hardware.Camera;

/**
 * Created by peter on 12/3/13.
 */
public class YV12ToYUV420Converter extends YUV420Converter {

    private int ySize;
    private int uvSize;
    private byte[] buffer;

    public YV12ToYUV420Converter(Camera.Size size) {
        super(size);
        int yStride = (int) Math.ceil(size.width / 16.0) * 16;
        int uvStride = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
        ySize = yStride * size.height;
        uvSize = uvStride * size.height / 2;
        buffer = new byte[uvSize];
    }

    @Override
    public void convert(byte[] data) {
        System.arraycopy(data, ySize, buffer, 0, uvSize);
        System.arraycopy(data, ySize + uvSize, data, ySize, uvSize);
        System.arraycopy(buffer, 0, data, ySize + uvSize, uvSize);
    }

}

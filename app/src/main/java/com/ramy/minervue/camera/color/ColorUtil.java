package com.ramy.minervue.camera.color;

import android.graphics.ImageFormat;
import android.hardware.Camera;

/**
 * Created by peter on 12/3/13.
 */
public class ColorUtil {

    public static int getFrameSize(Camera.Size size, int format) {
        switch (format) {
            case ImageFormat.YV12:
                int yStride = (int) Math.ceil(size.width / 16.0) * 16;
                int uvStride = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
                int ySize = yStride * size.height;
                int uvSize = uvStride * size.height / 2;
                return ySize + uvSize * 2;
            default:
                return size.width * size.height * ImageFormat.getBitsPerPixel(format) / 8;
        }
    }

}

package com.ramy.minervue.camera;

import android.hardware.Camera;

/**
 * Created by peter on 11/30/13.
 */
public class CameraZoomer implements Camera.OnZoomChangeListener {

    private int maxZoom;
    private Camera camera;
    private boolean isZooming = false;
    private int pendingZoom = 0;

    public CameraZoomer(Camera camera) {
        this.camera = camera;
        camera.setZoomChangeListener(this);
        maxZoom = camera.getParameters().getMaxZoom();
    }

    public void zoomTo(int percent) {
        pendingZoom = maxZoom * percent / 100;
        if (!isZooming) {
            camera.startSmoothZoom(pendingZoom);
        }
    }

    @Override
    public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {
        isZooming = !stopped;
        if (stopped && pendingZoom != zoomValue) {
            camera.startSmoothZoom(pendingZoom);
        }
    }

}

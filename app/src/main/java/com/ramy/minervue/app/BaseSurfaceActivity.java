package com.ramy.minervue.app;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ramy.minervue.R;
import com.ramy.minervue.camera.MyCamera;
import com.ramy.minervue.camera.PreviewSizeAdapter;
import com.ramy.minervue.ffmpeg.MP4Muxer;
import com.ramy.minervue.media.VideoCodec;
import com.ramy.minervue.util.MySwitch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by peter on 3/29/14.
 */
public abstract class BaseSurfaceActivity extends Activity implements TextureView.SurfaceTextureListener, MP4Muxer.RecordTimeListener {

    private static final String TAG = "RAMY-BaseSurfaceActivity";

    protected static final int UI_LEVEL_DISABLE = 0;
    protected static final int UI_LEVEL_BUSY = 1;
    protected static final int UI_LEVEL_NORMAL = 2;

    protected TextView timeText;
    protected TextureView textureView;
    protected SeekBar zoomSeek;
    protected TextView cameraFacingText;
    protected MySwitch ledSwitch;
    protected Button switchCameraButton;
    protected Spinner sizeSpinner;

    protected CameraSizeListener sizeListener = new CameraSizeListener();
    protected PreviewSizeAdapter spinnerAdapter;

    protected Date recordTime = new Date();
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public void onContentChanged() {
        timeText = (TextView) findViewById(R.id.tv_video_info);
        textureView = (TextureView) findViewById(R.id.tv_video_preview);
        textureView.setSurfaceTextureListener(this);
        zoomSeek = (SeekBar) findViewById(R.id.sb_zoom_control);
        zoomSeek.setProgress(0);
        zoomSeek.setOnSeekBarChangeListener(new ZoomListener());
        cameraFacingText = (TextView) findViewById(R.id.tv_camera_info);
        ledSwitch = (MySwitch) findViewById(R.id.sw_led);
        ledSwitch.setOnCheckedChangeListener(new LEDListener());
        switchCameraButton = (Button) findViewById(R.id.bt_switch_camera);
        sizeSpinner = (Spinner) findViewById(R.id.sp_resolution);
        sizeSpinner.setOnItemSelectedListener(sizeListener);
        setUILevel(UI_LEVEL_DISABLE);
    }

    protected abstract VideoCodec getVideoCodec();

    protected void updateUIForCamera(MyCamera camera) {
        spinnerAdapter = new PreviewSizeAdapter(camera.getPreviewSizes());
        sizeSpinner.setAdapter(spinnerAdapter);
        int pos = spinnerAdapter.getItemPosition(camera.getCurrentPreviewSize());
        sizeListener.setLastPos(pos);
        sizeSpinner.setSelection(pos);
        boolean front = camera.isFront();
        cameraFacingText.setText(front ? R.string.front_camera : R.string.back_camera);
        Matrix m = getVideoCodec().getMatrixFor(textureView.getWidth(), textureView.getHeight());
        textureView.setTransform(m);
    }

    public void onSwitchCamera(View view) {
        VideoCodec codec = getVideoCodec();
        MyCamera camera = codec.getCurrentCamera();
        if (camera != null) {
            Camera.Size size = camera.getCurrentPreviewSize();
            if (!codec.switchCamera(textureView.getSurfaceTexture(), size.width, size.height)) {
                setUILevel(UI_LEVEL_DISABLE);
                toast(getString(R.string.unknown_error));
            } else {
                updateUIForCamera(codec.getCurrentCamera());
            }
        }
    }

    @Override
    public void onUpdateTime(long recordMilli) {
        recordTime.setTime(recordMilli);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeText.post(new Runnable() {
            @Override
            public void run() {
                timeText.setText(dateFormat.format(recordTime));
            }
        });
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void setUILevel(int level) {
        ledSwitch.setEnabled(level >= UI_LEVEL_BUSY);
        switchCameraButton.setEnabled(level >= UI_LEVEL_BUSY);
        sizeSpinner.setEnabled(level >= UI_LEVEL_NORMAL);
        zoomSeek.setEnabled(level >= UI_LEVEL_BUSY);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // Default implementation: do nothing.
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Default implementation: do nothing.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Default implementation: do nothing.
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Default implementation: do nothing.
    }

    protected class ZoomListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            getVideoCodec().getCurrentCamera().zoom(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

    }

    protected class LEDListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            getVideoCodec().getCurrentCamera().setLED(isChecked);
        }

    }

    protected class CameraSizeListener implements AdapterView.OnItemSelectedListener {

        private int lastPos = 0;

        public void setLastPos(int lastPos) {
            this.lastPos = lastPos;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (lastPos == position) {
                return;
            }
            Camera.Size size = spinnerAdapter.getItem(position);
            Log.i(TAG, "Selecting " + size.width + "x" + size.height + ".");
            SurfaceTexture surface = textureView.getSurfaceTexture();
            if (!getVideoCodec().startPreview(surface, size.width, size.height)) {
                setUILevel(UI_LEVEL_DISABLE);
                toast(getString(R.string.unknown_error));
            } else {
                updateUIForCamera(getVideoCodec().getCurrentCamera());
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    }

}

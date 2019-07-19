package com.ramy.minervue.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;

import com.ramy.minervue.R;
import com.ramy.minervue.camera.MyCamera;
import com.ramy.minervue.media.Recorder;
import com.ramy.minervue.media.VideoCodec;
import com.ramy.minervue.sync.LocalFileUtil;
import com.ramy.minervue.sync.SyncManager;
import com.ramy.minervue.util.MySwitch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class VideoActivity extends BaseSurfaceActivity {

    private static final String TAG = "RAMY-VideoActivity";

    private MySwitch recordTypeSwitch;
    private Button actionButton ,bt_video_action;
	private int count = 1;
	private int mcount = 1;
	private boolean isPassive = false;
    private Recorder recorder = new Recorder();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);
		bt_video_action = (Button) findViewById(R.id.bt_video_action);
    }

    @Override
    public void onContentChanged() {
        recordTypeSwitch = (MySwitch) findViewById(R.id.sw_photo_or_video);
        actionButton = (Button) findViewById(R.id.bt_video_action);
		count = getIntent().getIntExtra("Phtot_count_int", 0);
		isPassive = getIntent().getBooleanExtra(getPackageName() + ".StartNow",
				false);
        super.onContentChanged();
    }

    @Override
    protected void setUILevel(int level) {
        recordTypeSwitch.setEnabled(level >= UI_LEVEL_NORMAL);
        if (level == UI_LEVEL_BUSY) {
            actionButton.setText(R.string.stop);
        } else {
            actionButton.setText(R.string.record);
        }
        actionButton.setEnabled(level >= UI_LEVEL_BUSY);
        super.setUILevel(level);
    }

    @Override
    protected VideoCodec getVideoCodec() {
        return recorder.getVideoCodec();
    }
    
	private void setPrioritys(String path, int priority) {
		LocalFileUtil util = MainService.getInstance().getSyncManager().getLocalFileUtil();
		util.setPriority(path, priority);
		toast(getString(R.string.saved) + util.setPriority(path, priority));
		MainService.getInstance().getSyncManager().startSync();
	}

    private void setPriority(String path, int priority) {
        LocalFileUtil util = MainService.getInstance().getSyncManager().getLocalFileUtil();
        toast(getString(R.string.saved) + util.setPriority(path, priority));
        MainService.getInstance().getSyncManager().startSync();
    }

    private void choosePriority(final String path) {
        String[] priority = getResources().getStringArray(R.array.priority);
        new AlertDialog.Builder(this).setItems(priority, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setPriority(path, which + 1);
            }
        }).setTitle(R.string.choose_priority).setCancelable(false).show();
    }

    public void onRecord(View view) {
        SyncManager syncManager = MainService.getInstance().getSyncManager();
        if (recordTypeSwitch.isChecked()) {
            if (recorder.isCapturing()) {
                recorder.stop();
                String filePath = recorder.getMuxer().getFilePath();
                setUILevel(UI_LEVEL_NORMAL);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                choosePriority(filePath);
            } else {
                String recordFile = syncManager.getLocalFileUtil().generateVideoFilename();
                recorder.start(recordFile);
                recorder.getMuxer().setRecordTimeListener(this);
                setUILevel(UI_LEVEL_BUSY);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else {
			if(isPassive == true){
				MyCamera camera = getVideoCodec().getCurrentCamera();
				camera.takePicture(new MyCamera.PictureCallback() {
					@Override
					public void onPictureTaken(byte[] data) {
						try {
							String path  = MainService.getInstance().getSyncManager().getLocalFileUtil().generateImageFilename();
							FileOutputStream fos = new FileOutputStream(MainService.getInstance().getSyncManager().getLocalFileUtil().generateImageFilename());
							fos.write(data);
							fos.close();
							setPrioritys(path, 1);
							actionButton.requestFocus();
							if(mcount==count){
								MyDestory();
								return;
							}
							handler.sendEmptyMessage(1);   
							mcount++;
						} catch (FileNotFoundException e) {
							MyDestory();
							e.printStackTrace();
						} catch (IOException e) {
							MyDestory();
							e.printStackTrace();
						} 
					}
				});
			}else{
	            final String filename = syncManager.getLocalFileUtil().generateImageFilename();
	            MyCamera camera = getVideoCodec().getCurrentCamera();
	            camera.takePicture(new MyCamera.PictureCallback() {
	                @Override
	                public void onPictureTaken(byte[] data) {
	                    try {
	                        FileOutputStream fos = new FileOutputStream(filename);
	                        fos.write(data);
	                        fos.close();
	                        choosePriority(filename);
	                    } catch (FileNotFoundException e) {
	                        e.printStackTrace();
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                }
	            });
			}
        }
    }

    @Override
    public void onBackPressed() {
        if (recordTypeSwitch.isChecked() && recorder.isCapturing()) {
            recorder.stop();
            String filePath = recorder.getMuxer().getFilePath();
            setUILevel(UI_LEVEL_NORMAL);
            choosePriority(filePath);
        } else {
            super.onBackPressed();
        }
    }
    
	Handler handler = new Handler(){  

		public void handleMessage(Message msg) {  
			switch (msg.what) {      
			case 1:      
				actionButton.performClick();  
				break;      
			}      
			super.handleMessage(msg);  
		}  

	};

    @Override
    protected void onPause() {
        super.onPause();
        if (recordTypeSwitch.isChecked() && recorder.isCapturing()) {
            recorder.stop();
            String filePath = recorder.getMuxer().getFilePath();
            setUILevel(UI_LEVEL_NORMAL);
            setPriority(filePath, 1);
        }
        getVideoCodec().stopPreview();
        finish();
    }
    
	public void MyDestory(){
		this.finish();
	}

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        VideoCodec codec = getVideoCodec();
        if (codec.startPreview(surface, 0, 0)) {
            setUILevel(UI_LEVEL_NORMAL);
            updateUIForCamera(codec.getCurrentCamera());
            if(isPassive==true){
    			bt_video_action.performClick();
    		}
        } else {
            toast(getString(R.string.unknown_error));
        }
    }

}

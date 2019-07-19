package com.ramy.minervue.app;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.mediatek.engineermode.io.EmGpio;
import com.ramy.minervue.bean.GasData;
import com.ramy.minervue.service.GasService;
import com.ramy.minervue.util.ATask;
import com.ramy.minervue.util.DataProcessUtils;
import com.ramy.minervue.view.SlipButton;
import com.ramy.minervue.view.SlipButton.OnChangedListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.ramy.minervue.*;

public class GasActivity extends Activity implements OnClickListener,
		OnChangedListener {
	public static final String TAG = "GasActivity";
	private static final String READ_DATA_CMD = "fe680124008d0d";
	private GasService gasService;
	private GasDatasReaderTask task;
	private Button takePic;
	private Button save;
	private SlipButton slipButton;
	private TextView tv_refresh;
	private TextView tv_ymd;
	private TextView tv_hms;
	private TextView tv_ch4_value;
	private TextView tv_co_value;
	private TextView tv_rh_value;
	private TextView tv_tem_value;
	private TextView tv_h2s_value;
	private EditText et_adress;
	private TextView tv_o2_value;
	private RelativeLayout loading_data;
	private RelativeLayout show_data;
	private String gasContent;
	private boolean isStop;
	private GasQueryTask queryTask;
	private ScheduledExecutorService scheduledExecutorService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gas_activity);
		GpioInit();
		initView();
		setListener();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		startQuery();
	}

	private void startQuery() {
		if (task == null || task.isFinished()) {
			gasService = new GasService();
			task = new GasDatasReaderTask(this);
			task.start(READ_DATA_CMD);
		}
	}

	private void GpioInit() {
		EmGpio.gpioInit();
		EmGpio.setGpioOutput(216);
		EmGpio.setGpioOutput(217);
		EmGpio.setGpioDataLow(217);

		// EmGpio.setGpioDataHigh(217);

	}

	private void setListener() {
		slipButton.SetOnChangedListener(this);
		takePic.setOnClickListener(this);
		save.setOnClickListener(this);
	}

	public void initView() {
		loading_data = (RelativeLayout) findViewById(R.id.loading_data);
		show_data = (RelativeLayout) findViewById(R.id.show_data);
		tv_refresh = (TextView) findViewById(R.id.auto_refresh_tv);
		tv_ymd = (TextView) findViewById(R.id.ymd);
		tv_hms = (TextView) findViewById(R.id.hms);
		tv_ch4_value = (TextView) findViewById(R.id.tv_ch4_value);
		tv_o2_value = (TextView) findViewById(R.id.tv_o2_value);
		tv_h2s_value = (TextView) findViewById(R.id.tv_h2s_value);
		tv_rh_value = (TextView) findViewById(R.id.tv_rh_value);
		tv_tem_value = (TextView) findViewById(R.id.tv_tem_value);
		tv_co_value = (TextView) findViewById(R.id.tv_co_value);
		et_adress = (EditText) findViewById(R.id.et_adress);
		slipButton = (SlipButton) findViewById(R.id.auto_refresh_bt);
		takePic = (Button) findViewById(R.id.takepic);
		save = (Button) findViewById(R.id.save);
		loading_data.setVisibility(View.VISIBLE);
		show_data.setVisibility(View.INVISIBLE);
	}

	private class GasDatasReaderTask extends ATask<String, String, Integer> {
		public static final int OPEN_SERIAL_PORT_FAIL = 1;
		public static final int RECEIVE_DATA_ERROR = 2;
		public static final int RECEIVE_DATA_SUCCESS = 4;
		public static final int RECEIVE_DATA_TIME_OUT = 3;
		private GasActivity activity;
		private AlertDialog.Builder builder;

		public GasDatasReaderTask(GasActivity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.error_tip);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.confirm,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
		}

		protected synchronized Integer doInBackground(String... params) {
			gasService.close();
			if (!gasService.open()) {
				gasService.close();
				return OPEN_SERIAL_PORT_FAIL;
			}
			gasService.sendReadCmd(params[0]);
			return gasService.getReplysData();
		}

		protected void onPostExecute(Integer result) {
			switch (result) {
			case OPEN_SERIAL_PORT_FAIL:
				builder.setMessage(R.string.operation_failed);
				builder.create().show();
				break;
			case RECEIVE_DATA_ERROR:
			case RECEIVE_DATA_TIME_OUT:
				builder.setMessage(R.string.invalid_state);
				builder.create().show();
				break;
			case RECEIVE_DATA_SUCCESS:
				loading_data.setVisibility(View.INVISIBLE);
				show_data.setVisibility(View.VISIBLE);
				setViews();
				break;
			default:
				break;
			}

		}

		private void setViews() {
			DataProcessUtils dp = gasService.getDataProcessUtils();
			StringBuffer buffer = new StringBuffer();
			List<GasData> gasdatas = dp.getGasData();
			for (GasData gd : gasdatas) {
				String gasName = gd.getGasName();
				String gasValue = gd.getGasValue();
				buffer.append(gasName + " :");
				if ("CH4".equals(gasName)) {
					tv_ch4_value.setText(gasValue);
					buffer.append(gasValue + "%");
				} else if ("O 2".equals(gasName)) {
					tv_o2_value.setText(gasValue);
					buffer.append(gasValue + "%");
				} else if ("H2S".equals(gasName)) {
					tv_h2s_value.setText(gasValue);
					buffer.append(gasValue + "ppm");
				} else if ("C O".equals(gasName)) {
					tv_co_value.setText(gasValue);
					buffer.append(gasValue + "ppm");
				} else if ("R H".equals(gasName)) {
					tv_rh_value.setText(gasValue);
					buffer.append(gasValue + "%");
				} else if ("Tem".equals(gasName)) {
					tv_tem_value.setText(gasValue);
					buffer.append(gasValue + "°C");
				}
				buffer.append("\n");
			}

			String ymd = dp.getYMD();
			String hms = dp.getHMS();
			buffer.append(ymd + "  " + hms + "\n");
			setGasContent(new String(buffer));
			tv_ymd.setText(ymd);
			tv_hms.setText(hms);
		}
	}

	@Override
	protected void onPause() {
		finish();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (scheduledExecutorService != null) {
			isStop = true;
			scheduledExecutorService.shutdown();
		}
		if (task != null) {
			task.cancelAndClear(true);
			task = null;
		}
		if (gasService != null) {
			gasService.close();
		}
		if(queryTask!=null){
			queryTask=null;
		}
		EmGpio.gpioUnInit();
		super.onDestroy();
	}

	public void onClick(View v) {
		String adress = et_adress.getText().toString().trim();
		if (TextUtils.isEmpty(adress)) {
			Toast.makeText(this, R.string.adress, 1).show();
			return;
		}
		switch (v.getId()) {
		case R.id.takepic:
			Intent intent = new Intent(GasActivity.this,
					GasPhotographerActivity.class);
			intent.putExtra(getPackageName() + ".GasData", adress + "\n"
					+ getGasContent());
			startActivity(intent);
			finish();
			break;
		case R.id.save:
			MainService service = MainService.getInstance();
			String filename = service.getSyncManager().getLocalFileUtil()
					.generateSensorTxtFilename();
			try {
				FileOutputStream fous = new FileOutputStream(filename);
				fous.write((adress + "\n").getBytes());
				fous.write(getGasContent().getBytes());
				Toast.makeText(this, R.string.file_save_success, 1).show();
				finish();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}

	}

	private String getGasContent() {
		return gasContent;
	}

	private void setGasContent(String gasContent) {
		this.gasContent = gasContent;
	}

	@Override
	public void OnChanged(boolean CheckState) {
		isStop = !CheckState;
		if (CheckState) {
			if (queryTask == null) {
				queryTask = new GasQueryTask();
				scheduledExecutorService.scheduleWithFixedDelay(
						new GasQueryTask(), 3, 3, TimeUnit.SECONDS);
			}
			tv_refresh.setText("自动刷新已开启");
		} else {
			tv_refresh.setText("自动刷新已关闭");
		}
	}

	private class GasQueryTask implements Runnable {
		@Override
		public void run() {
			if (isStop) {
				return;
			}
			startQuery();
		}

	}

}

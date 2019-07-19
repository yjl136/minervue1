package com.ramy.minervue.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.mediatek.engineermode.io.EmGpio;
import com.ramy.minervue.bean.ReplyData;
import com.ramy.minervue.util.DataProcessUtils;
import android.os.SystemClock;
import android_serialport_api.BinaryUtils;
import android_serialport_api.SerialPort;
public class GasService {
	public static final String TAG = "GasService";
	public static final int RECEIVE_DATA_SUCCESS = 4;
	public static final int RECEIVE_DATA_TIME_OUT = 3;
	public static final int RECEIVE_DATA_ERROR = 2;
	private static final String DEVICE_PATH = "/dev/ttyMT0";
	private static final int BAUDRATE = 1200;
	protected SerialPort mSerialPort;
	private OutputStream mOutputStream;
	private InputStream mInputStream;
	private boolean isOpened;
	private DataProcessUtils dataProcessUtils;
	private byte[] buffer;
	private ExecutorService executorService;

	public GasService() {
		executorService = Executors.newFixedThreadPool(1);
		isOpened = false;
		buffer = new byte[64];
	}

	public DataProcessUtils getDataProcessUtils() {
		return dataProcessUtils;
	}
	public void setDataProcessUtils(DataProcessUtils dataProcessUtils) {
		this.dataProcessUtils = dataProcessUtils;
	}

	public boolean open() {
		if (isOpened) {
			return true;
		}
		try {
			this.mSerialPort = new SerialPort(new File(DEVICE_PATH), BAUDRATE,
					0);
			isOpened = true;
			this.mOutputStream = this.mSerialPort.getOutputStream();
			this.mInputStream = this.mSerialPort.getInputStream();
			return true;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void close() {
		if (this.mSerialPort != null) {
			mSerialPort.close();
		}
		this.mSerialPort = null;
		this.isOpened = false;
	}
	public int getReplysData() {
		SPReadTask task = new SPReadTask();
		Future<Integer> future = executorService.submit(task);
		try {
			Integer readNum = future.get(3000, TimeUnit.MILLISECONDS);
			if (readNum != 37) {
				return RECEIVE_DATA_ERROR;
			}
			String content = BinaryUtils.toHex(buffer, readNum);
			String[] result = content.split(" ");
			ReplyData replyData = generateReplyData(result);
			dataProcessUtils = new DataProcessUtils(replyData);
			if (!dataProcessUtils.isDataRight()) {
				return RECEIVE_DATA_ERROR;
			}
			setDataProcessUtils(dataProcessUtils);
			return RECEIVE_DATA_SUCCESS;
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return RECEIVE_DATA_TIME_OUT;

	}

	public ReplyData generateReplyData(String[] result) {
		ReplyData replyData = new ReplyData();
		setResult(result, replyData);
		setFixPart(result, replyData, 0, 5);
		setGasDatas(result, replyData, 5, 29);
		setDate(result, replyData, 29, 35);
		setCS(result, replyData, 35, 36);
		setEFlag(result, replyData, 36, 37);
		return replyData;

	}

	private void setResult(String[] result, ReplyData requestsData) {
		requestsData.setResult(result);
	}

	private void setEFlag(String[] result, ReplyData requestsData, int start,
			int end) {
		requestsData.seteFlag(getStrs(result, start, end));
	}

	private void setCS(String[] result, ReplyData requestsData, int start,
			int end) {
		requestsData.setCs(getStrs(result, start, end));
	}

	private void setDate(String[] result, ReplyData requestsData, int start,
			int end) {
		requestsData.setDate(getStrs(result, start, end));
	}

	private void setGasDatas(String[] result, ReplyData requestsData,
			int start, int end) {
		requestsData.setGasDatas(getStrs(result, start, end));
	}

	private void setFixPart(String[] result, ReplyData requestsData, int start,
			int end) {
		requestsData.setfPart(getStrs(result, start, end));
	}

	private String[] getStrs(String[] result, int start, int end) {
		String[] date = new String[end - start];
		for (int i = start; i < end; i++) {
			if (start == 0) {
				date[i] = result[i];
			} else {
				date[i - start] = result[i];
			}
		}
		return date;
	}
	public void sendReadCmd(String cmd) {
		try {
			byte[] buffer = BinaryUtils.fromHex(cmd);
			EmGpio.setGpioDataHigh(216);
			mOutputStream.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	private class SPReadTask implements Callable<Integer> {

		public Integer call() throws IOException {
			int count = 0;
			int readByteCount = 0;
			int readNum = 0;
			int offset = 0;
			SystemClock.sleep(57);
			EmGpio.setGpioDataLow(216);
			while (true) {
				int available = mInputStream.available();
				if (available != 0) {
					readNum = mInputStream.read(buffer, offset, buffer.length
							- offset);
					readByteCount += readNum;
					offset = readByteCount;
				} else {
					SystemClock.sleep(30);
					count++;
				}
				if (readByteCount == 37 || count>200) {
					return readByteCount;
				}
			}

		}

	}

}

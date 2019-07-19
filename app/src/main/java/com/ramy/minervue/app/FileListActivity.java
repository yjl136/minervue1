package com.ramy.minervue.app;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ramy.minervue.R;
import com.ramy.minervue.dao.PubDao;
import com.ramy.minervue.sync.LocalFileUtil;
import com.ramy.minervue.util.FileUtils;
import com.ramy.minervue.util.PreferenceUtil;
import com.ramy.minervue.util.ConfigUtil.PreferenceListener;

import java.io.File;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by peter on 11/26/13.
 */
public class FileListActivity extends ListActivity implements
AbsListView.MultiChoiceModeListener, AdapterView.OnItemClickListener,
PreferenceUtil.PreferenceListener {

	private static final String TAG = "RAMY-FileListActivity";
	private Button btn_fileOrderByDate,btn_fileOrderByLeve,btn_browse;
	private TextView tv_typeStatus,tv_main_status;
	private File rootFile = null;
	private boolean isPubDir = false;
	private boolean isRootDir = false;
	private boolean isSensorDir = false;
	private boolean isImageDir = false;
	private boolean isVideoDir = false;
	private FileListAdapter adapter = null;

	private MainService service;

	File[] files;
	private void initListContent(String type) {
		// Initialize the list.
		if(rootFile.listFiles()==null){
			return ;
		}
		files= rootFile.listFiles();

		if (files == null) {
			return;
		}
		if (isPubDir) {
			if(isPubDir){
				btn_browse.setVisibility(View.GONE);
				btn_browse.setOnClickListener(new OnClickListener() {
					SharedPreferences userInfo = getSharedPreferences("user_info_check", 0);  
					@Override
					public void onClick(View v) {
						if(btn_browse.getText().toString().equals("横屏浏览")){
							btn_browse.setText(R.string.browse_l);
							userInfo.edit().putString("name", btn_browse.getText().toString()).commit();  
						}else{
							btn_browse.setText(R.string.browse);
							userInfo.edit().putString("name", btn_browse.getText().toString()).commit();  
						}
					}
				});
			}		Arrays.sort(files, new LastModifiedTimeComparator());
		} else {
			if(isVideoDir==true){
				tv_typeStatus.setVisibility(View.VISIBLE);
				tv_typeStatus.setText("当前排序：等级");
				Arrays.sort(files, new NameComparator());
			}
			if(isVideoDir==true||isImageDir==true){
				tv_typeStatus.setVisibility(View.VISIBLE);
				tv_typeStatus.setText("当前排序：时间");//默认排序   
				Arrays.sort(files, new LastModifiedTimeComparator());
				btn_fileOrderByDate.setVisibility(View.VISIBLE);
				btn_fileOrderByLeve.setVisibility(View.VISIBLE);
				//btn_browse.setVisibility(View.VISIBLE);
				btn_browse.setOnClickListener(new OnClickListener() {
					SharedPreferences userInfo = getSharedPreferences("user_info_check", 0);  
					@Override
					public void onClick(View v) {
						if(btn_browse.getText().toString().equals("横屏浏览")){
							btn_browse.setText(R.string.browse_l);
							userInfo.edit().putString("name", btn_browse.getText().toString()).commit();  
						}else{
							btn_browse.setText(R.string.browse);
							userInfo.edit().putString("name", btn_browse.getText().toString()).commit();  
						}
					}
				});
				btn_fileOrderByLeve.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						initListContent("fileOrderByLeve");
					}
				});
				if(type!=null&&type.equals("fileOrderByLeve")){
					tv_typeStatus.setVisibility(View.VISIBLE);
					tv_typeStatus.setText("当前排序：级别");
					Arrays.sort(files, new NameComparator());
				}


				btn_fileOrderByDate.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						initListContent("fileOrderByDate");
					}
				});
				if(type!=null&&type.equals("fileOrderByDate")){
					tv_typeStatus.setVisibility(View.VISIBLE);
					tv_typeStatus.setText("当前排序：时间");
					Arrays.sort(files, new NameComparatorByDate());
				}

			}

		}
		adapter.setFiles(files);
		View view = findViewById(R.id.tv_file_list_nothing_to_show);
		view.setVisibility(files.length == 0 ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_list_activity);
		btn_fileOrderByDate = (Button) findViewById(R.id.btn_fileOrderByDate);
		btn_fileOrderByLeve = (Button) findViewById(R.id.btn_fileOrderByLeve);
		btn_browse = (Button) findViewById(R.id.btn_browse);
		tv_typeStatus = (TextView) findViewById(R.id.tv_typeStatus);
		rootFile = (File) getIntent().getSerializableExtra(
				getPackageName() + ".ListFile");///storage/sdcard0/Android/data/com.ramy.minervue/files
		if (rootFile == null || !rootFile.isDirectory()) {
			return;
		}
		service = MainService.getInstance();
		isPubDir = service.getSyncManager().getLocalFileUtil()
				.isPubDir(rootFile);
		isRootDir = service.getSyncManager().getLocalFileUtil()
				.isRootDir(rootFile);
		isSensorDir = service.getSyncManager().getLocalFileUtil()
				.isSensorDir(rootFile);
		isImageDir = service.getSyncManager().getLocalFileUtil()
				.isImageDir(rootFile);
		isVideoDir = service.getSyncManager().getLocalFileUtil()
				.isVideoDir(rootFile);
		adapter = new FileListAdapter();
		setListAdapter(adapter);
		initListContent("");
		getListView().setOnItemClickListener(this);
		getListView().setMultiChoiceModeListener(this);
		SharedPreferences userInfo = getSharedPreferences("user_info_check", 0);  
		String username = userInfo.getString("name", "横屏浏览");  
		if(username!=null){
			btn_browse.setText(username);
		}
		if (isRootDir) {
			service.getPreferenceUtil().getUnreadDownload(this);
		}

	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position,
			long id, boolean checked) {
		// Nothing to do.
		Log.i(TAG, "on item checked state changed");
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		Log.i(TAG, "onCreateActionMode");
		MenuInflater inflater = mode.getMenuInflater();
		if (inflater != null) {
			inflater.inflate(R.menu.delete_file_menu, menu);
			return true;
		}
		return false;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		Log.i(TAG, "onPrepareActionMode");
		return true;
	}

	private void toast(int resId) {
		Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		Log.i(TAG, "onActionItemClicked");
		if (item.getItemId() == R.id.mn_delete_file) {
			SparseBooleanArray array = getListView().getCheckedItemPositions();
			for (int i = 0; i < array.size(); ++i) {
				int pos = array.keyAt(i);
				if (array.get(pos)) {
					File f = adapter.getItem(pos);
					PubDao pubDao = new PubDao(this);
					if (isPubDir && pubDao.isExist(f.getPath())) {
						pubDao.delete(f.getPath());
					}
					if(isPubDir && !pubDao.isExist(f.getPath())){
						int unread = service.getPreferenceUtil().getUnreadDownload(null);
						MainService service = MainService.getInstance();
						service.getPreferenceUtil().setUnreadDownload(unread-1);
					}
					/*MainService service = MainService.getInstance();
					if (service.getSyncManager().getLocalFileUtil().isPubDir(f)) {
						service.getPreferenceUtil().setUnreadDownload(0);
					}*/
					if (LocalFileUtil.delete(f)) {
						toast(R.string.delete_success);
					} else {
						toast(R.string.delete_fail);
					}
				}
			}
			initListContent("");
			mode.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		Log.i(TAG, "onDestroyActionMode");
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		File file = (File) getListAdapter().getItem(position);
		if (file.isDirectory()) {
			Intent intent = new Intent(FileListActivity.this,
					FileListActivity.class);
			intent.putExtra(getPackageName() + ".ListFile", file);
			startActivity(intent);

		} else {
			PubDao pubDao = new PubDao(this);
			int unread = service.getPreferenceUtil().getUnreadDownload(null);
			if (isPubDir && !pubDao.isExist(file.getPath()) && unread > 0) {
				pubDao.add(file.getPath());
				TextView unreadText = (TextView) view
						.findViewById(R.id.tv_unread_download);

				service.getPreferenceUtil().setUnreadDownload(unread - 1);
				adapter.notifyDataSetChanged();
			}
			String type = URLConnection
					.guessContentTypeFromName(file.getName());
			if (type == null) {
				Toast.makeText(FileListActivity.this, R.string.unknown_file,
						Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				//TODO  横竖屏浏览
				if(btn_browse.getText().toString().equals("竖屏浏览")){
					FileListActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}else if(btn_browse.getText().toString().equals("横屏浏览")){
					FileListActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(file), type);
				startActivity(intent);


			} catch (ActivityNotFoundException e) {
				Toast.makeText(FileListActivity.this, R.string.unknown_file,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void onResume() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		super.onResume();
	}
	private class FileListAdapter extends BaseAdapter {

		private File[] files;

		public FileListAdapter() {
			this.files = new File[0];
		}

		public void setFiles(File[] files) {
			this.files = files;
			notifyDataSetInvalidated();
		}

		@Override
		public int getCount() {
			return files.length;
		}

		@Override
		public File getItem(int position) {
			return files[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint({ "SimpleDateFormat", "ResourceAsColor" })
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.file_list_item, parent,
						false);
			}
			TextView name = (TextView) convertView
					.findViewById(R.id.tv_file_list_name);
			TextView detail = (TextView) convertView
					.findViewById(R.id.tv_file_list_detail);
			TextView tv_file_list_time = (TextView) convertView.findViewById(R.id.tv_file_list_time);
			tv_main_status = (TextView) convertView.findViewById(R.id.tv_main_status);
			File file = files[position];
			String filetime = file.getName();   //3-20140708-092400.mp4
			String num = file.getName().substring(0, 1);
			if(num.equals("1")){
				name.setTextColor(Color.GREEN);
			}else if(num.equals("2")){
				name.setTextColor(Color.YELLOW);
			}else if(num.equals("3")){
				name.setTextColor(Color.RED);
			}
			if (isRootDir) {
				// 根目錄
				if ("pub".equals(file.getName())) {
					TextView unreadText = (TextView) convertView
							.findViewById(R.id.tv_unread_download);
					int unread = service.getPreferenceUtil().getUnreadDownload(
							null);
					Log.i(TAG, unread+"");
					if (unread > 0) {
						unreadText.setVisibility(View.VISIBLE);
						unreadText.setTextColor(Color.parseColor("#CC0033"));
						unreadText.setText(unread + "");
					}else{
						unreadText.setVisibility(View.INVISIBLE);
					}
					//下发文件
					name.setText(R.string.pub_file);

				}
				if("image".equals(file.getName())){
				
					TextView unreadText = (TextView) convertView
							.findViewById(R.id.tv_unread_download);
					unreadText.setVisibility(View.INVISIBLE);
					name.setText(R.string.image_file);
				}
				if ("video".equals(file.getName())) {
					name.setText(R.string.video_file);
				}
				if ("sensor".equals(file.getName())) {
					name.setText(R.string.sensor_file);
				}
			} else {
				name.setTextSize(18f);
				if (isPubDir) {
					// pub目录
					PubDao pubDao = new PubDao(FileListActivity.this);
					ImageView iv_tx = (ImageView) convertView
							.findViewById(R.id.iv_wj);
					String fileType=FileUtils.getFileType(file.getName());

					//根据不同的文件类型，给出不同的图标
					if(FileUtils.isImage(fileType)){
						iv_tx.setImageResource(R.drawable.image_tx);
					}else if(FileUtils.isVedio(fileType)){
						iv_tx.setImageResource(R.drawable.image_sp);
					}else{
						iv_tx.setImageResource(R.drawable.image_un);
					}
					TextView unreadText = (TextView) convertView
							.findViewById(R.id.tv_unread_download);
					int unread = service.getPreferenceUtil().getUnreadDownload(
							FileListActivity.this);
					if (unread > 0 && !pubDao.isExist(file.getPath())) {
						unreadText.setText(R.string.tv_new);
						unreadText.setTextSize(20);
						unreadText.setTextColor(Color.parseColor("#CC0033"));
						unreadText.setVisibility(View.VISIBLE);
					}else{
						unreadText.setVisibility(View.INVISIBLE);
					}
//					name.setTextColor(android.R.color.white);
					name.setTextColor(Color.WHITE);
					name.setTextSize(20f);
					name.setText(file.getName());
				}else if (isImageDir) {
					//图像目录
					ImageView iv_tx = (ImageView) convertView.findViewById(R.id.iv_wj);
					iv_tx.setImageResource(R.drawable.image_tx);
					name.setText(file.getName());
				}
				else if (isSensorDir) {
					//传感文件目录
					ImageView iv_tx = (ImageView) convertView
							.findViewById(R.id.iv_wj);
					iv_tx.setImageResource(R.drawable.image_tx);
					name.setText(file.getName());
				}else if (isVideoDir) {
					//录像目录
					ImageView iv_tx = (ImageView) convertView
							.findViewById(R.id.iv_wj);
					iv_tx.setImageResource(R.drawable.image_sp);
					name.setText(file.getName());
				}else{
					Toast.makeText(FileListActivity.this, "含有未知文件！", Toast.LENGTH_SHORT).show();
				}

			}
			if (file.isDirectory()) {
				detail.setText(R.string.directory);
			} else {
				tv_main_status.setVisibility(View.GONE);
				String []files = filetime.split("-");
				if(files.length>1){
					if(files[2].substring(0, 2).equals("20")){
						String time  = files[2]+files[3];
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
						Calendar nowDate=Calendar.getInstance(),oldDate=Calendar.getInstance();
						nowDate.setTime(new Date());//设置为当前系统时间 
						try {
							oldDate.setTime(sdf.parse(time));
						} catch (ParseException e) {
							e.printStackTrace();
						}
						long timeNow=nowDate.getTimeInMillis();
						long timeOld=oldDate.getTimeInMillis();
						long da=(timeNow-timeOld)/(1000*60*60*24);//化为天
						if(da<0){
							tv_file_list_time.setText("请设置设备正确时间");
						}else if(da==0){
							if((timeNow-timeOld)/(1000)<0){
								tv_file_list_time.setText("请设置设备正确时间");
							}else if((timeNow-timeOld)/(1000)<60){
								tv_file_list_time.setText((timeNow-timeOld)/(1000)+"秒前");
							}else if((timeNow-timeOld)/(1000)>60&&(timeNow-timeOld)/(1000)<3600){
								long m=(timeNow-timeOld)/(1000*60);//化为分钟
								tv_file_list_time.setText(m+"分钟前");
							}else if((timeNow-timeOld)/(1000)<86400&&(timeNow-timeOld)/(1000)>3600){
								long hour=(timeNow-timeOld)/(1000*60*60);//化为小时
								tv_file_list_time.setText(hour+"小时前");
							}
						}else{
							long hour=(timeNow-timeOld)/(1000*60*60*24);//化为天
							tv_file_list_time.setText(hour+"天前");
						}
						//					detail.setTextColor(android.R.color.white);
					}
				}

				//如果是文件的话。获取文件大小： 并且设置文件大小  (1MB)
				detail.setTextSize(19);
				detail.setTextColor(Color.parseColor("#ffffff"));
				detail.setText("大小："+LocalFileUtil.toReadableSize(file.length()));

			}
			return convertView;
		}
	}

	/**
	 * 文件等候排序
	 * @author 周乾龙
	 */
	private class NameComparator implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			String lhName = lhs.getName();
			String rhName = rhs.getName();
			return rhName.compareTo(lhName);
		}
	}

	/**
	 * 根据文件生成时间排序
	 * @author 周乾龙
	 *
	 */
	private class NameComparatorByDate implements Comparator<File> {

		@Override
		public int compare(File lhs, File rhs) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			String lhsName  = lhs.getName().substring(2,lhs.getName().length()-4).replace("-", "");
			String rhsName  = rhs.getName().substring(2,rhs.getName().length()-4).replace("-", "");
			Date lhName;
			Date rhName;
			try {
				lhName = sdf.parse(lhsName);
				rhName = sdf.parse(rhsName);
				if(lhName.after(rhName)) return -1;
				return 1;
			} catch (ParseException e) {
				e.printStackTrace();
				return 0;
			}
		}
	}

@Override
protected void onRestart() {
	Log.i(TAG, "restart");
	adapter.notifyDataSetChanged();
	super.onRestart();
}


	/**
	 * 文件或目录的最后一次修改时间排序。 
	 * @author 周乾龙
	 */
	private class LastModifiedTimeComparator implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			if (lhs.lastModified() > rhs.lastModified()) {
				return -1;
			} else if (lhs.lastModified() < rhs.lastModified()) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	@Override
	public void onPreferenceChanged() {
		Log.i(TAG, "onPreferenceChanged");
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		initListContent("");
		adapter.notifyDataSetChanged();
	}
}

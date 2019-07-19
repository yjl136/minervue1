package com.ramy.minervue.dao;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ramy.minervue.db.PubDBHelper;

public class PubDao {
	public static String TAG="PubDao";
	private final static String table="pub";
	private PubDBHelper pubHelper;
	private Context context;
	public PubDao(Context context){
		this.context=context;
		pubHelper=PubDBHelper.getInstance(context);
	}
	/**
	 * 从数据库中得到所有的数据
	 * @return 返回数据库中的所有数据
	 */
	public HashSet<File> getAllFiles(){
		HashSet<File> files=null;
		SQLiteDatabase db=pubHelper.getWritableDatabase();
		if(db.isOpen()){
			files=new HashSet<File>();
			Cursor cursor=db.query(table,new String[]{"path"}, null, null, null,null, null);
			while (cursor.moveToNext()) {
				String path=cursor.getString(cursor.getColumnIndex("path"));
				File file=new File(path);
				files.add(file);
			}
			cursor.close();
			db.close();
		}
		return files;
	}
	public void delete(String path){
		SQLiteDatabase db=pubHelper.getWritableDatabase();
		if(db.isOpen()){
			db.delete(table, "path=?",new String[]{path});
			db.close();
		}
		
	}
	public boolean isExist(String path){
		File file=new File(path);
		HashSet<File> files=getAllFiles();
		return files.contains(file);
	}
	public void add(String path){
		SQLiteDatabase db=pubHelper.getWritableDatabase();
		if(db.isOpen()){
			ContentValues values=new ContentValues();
			values.put("path", path);
			db.insert(table,null, values);
			db.close();
		}
	}
	
}

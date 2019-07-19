package com.ramy.minervue.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class PubDBHelper extends SQLiteOpenHelper {
	//数据库名
	private final static String name="pub.db";
	//数据库的版本
	private final static int version=1;
	
	private final String sql="create table pub(_id integer primary key autoincrement,path text)";
	
	private  static PubDBHelper mInstance; 
	public static synchronized PubDBHelper getInstance(Context context){
		if(mInstance==null){
			mInstance=new PubDBHelper(context, name, null, version);
		}
		return mInstance;
	}
	private PubDBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(sql);
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}

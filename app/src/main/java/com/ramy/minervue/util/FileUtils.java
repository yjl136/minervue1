package com.ramy.minervue.util;

import java.util.HashMap;
import java.util.Iterator;

public class FileUtils {

	/**
	 * 获取文件后缀名
	 * @param fileName
	 * @return 文件后缀名
	 */
	public static String getFileType(String fileName) {
		if (fileName != null) {
			int typeIndex = fileName.lastIndexOf(".");
			if (typeIndex != -1) {
				String fileType = fileName.substring(typeIndex + 1)
						.toLowerCase();
				return fileType;
			}
		}
		return "";
	}

	/**
	 * 根据后缀名判断是否是图片文件
	 * 
	 * @param type
	 * @return 是否是图片结果true or false
	 */
	public static boolean isImage(String type) {
		if (type != null
				&& (type.equals("jpg") || type.equals("gif")
						|| type.equals("png") || type.equals("jpeg")
						|| type.equals("bmp") || type.equals("wbmp")
						|| type.equals("ico") || type.equals("jpe"))) {
			return true;
		}
		return false;
	}
	public static boolean isVedio(String type){
		if(type!=null && type.equals("mp4")
				&& type.equals("m4v")&& type.equals("3gp")&& type.equals("3gpp")
				&& type.equals("wmv")){
			return true;
		}
		return false;
	}
}

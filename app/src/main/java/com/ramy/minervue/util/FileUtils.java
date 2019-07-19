package com.ramy.minervue.util;

import java.util.HashMap;
import java.util.Iterator;

public class FileUtils {

	/**
	 * ��ȡ�ļ���׺��
	 * @param fileName
	 * @return �ļ���׺��
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
	 * ���ݺ�׺���ж��Ƿ���ͼƬ�ļ�
	 * 
	 * @param type
	 * @return �Ƿ���ͼƬ���true or false
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

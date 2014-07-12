package com.dismantle.mediagrid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.R.integer;


public class GlobalUtil {

	public static final int SOURCE_WEIBO_LIST=1;
	public static final int SOURCE_SQURE=2;
	public static final int SOURCE_WEIBO_DETAIL=3;
	public static final int SOURCE_USER_LIST=4;
	public static final int SOURCE_USER_DETAIL=5;
	public static final int SOURCE_MSG_LIST=6;
	
	public static final int OP_COMMENT=1;
	public static final int OP_FORWARD=2;
	public static final int OP_LIKE=3;
	public static final int OP_SENDMSG=4;
	
	
	//message type for Handler
	public static final int MSG_LOAD_SUCCESS = 1;
	public static final int MSG_LOAD_FAILED = 2;
	public static final int MSG_CREATE_DIR_SUCCESS=3;
	public static final int MSG_CREATE_DIR_FAILED = 4;
	public static final int MSG_UPLOAD_SUCCESS = 5;
	public static final int MSG_UPLOAD_FAILED = 6;
	
	public static final String source_code="source_code";
	public static final String data="data";
	public static final String op_code="op_code";
	
	//private static final int SOURCE_SQURE=2;
	public static String HexEncode(byte[] toencode) {
		StringBuilder sb = new StringBuilder(toencode.length * 2);
		for (byte b : toencode) {
			sb.append(Integer.toHexString((b & 0xf0) >>> 4));
			sb.append(Integer.toHexString(b & 0x0f));
		}
		return sb.toString().toUpperCase();
	}

	public static String MD5Encode(byte[] toencode) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(toencode);
			return HexEncode(md5.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(System.err);
		}
		return "";
	}

	public static String getTimeByFormat(String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		String created_at = formatter.format(curDate);
		
		return created_at;
	}
}

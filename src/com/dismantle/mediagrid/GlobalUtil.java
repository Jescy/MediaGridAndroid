package com.dismantle.mediagrid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

@SuppressLint("DefaultLocale")
public class GlobalUtil {

	public static final int SOURCE_WEIBO_LIST = 1;
	public static final int SOURCE_SQURE = 2;
	public static final int SOURCE_WEIBO_DETAIL = 3;
	public static final int SOURCE_USER_LIST = 4;
	public static final int SOURCE_USER_DETAIL = 5;
	public static final int SOURCE_MSG_LIST = 6;

	public static final int OP_COMMENT = 1;
	public static final int OP_FORWARD = 2;
	public static final int OP_LIKE = 3;
	public static final int OP_SENDMSG = 4;

	// message type for Handler
	public static final int MSG_LOAD_SUCCESS = 1;
	public static final int MSG_LOAD_FAILED = 2;
	public static final int MSG_CREATE_DIR_SUCCESS = 3;
	public static final int MSG_CREATE_DIR_FAILED = 4;
	public static final int MSG_UPLOAD_SUCCESS = 5;
	public static final int MSG_UPLOAD_FAILED = 6;
	public static final int MSG_DOWNLOAD_SUCCESS = 7;
	public static final int MSG_DOWNLOAD_FAILED = 8;
	public static final int MSG_LOGIN_SUCCESS = 9;
	public static final int MSG_LOGIN_FAILED = 10;
	public static final int MSG_SAVE_USER_DOC_SUCCESS = 11;
	public static final int MSG_SAVE_USER_DOC_FAILED = 12;
	public static final int MSG_GET_DB_INFO_SUCCESS = 13;
	public static final int MSG_GET_DB_INFO_FAILED = 14;
	public static final int MSG_POLLING_USER = 15;
	public static final int MSG_POLLING_CHAT = 16;
	public static final int MSG_POLLING_IM = 17;
	public static final int MSG_CHAT_LIST = 18;
	public static final int MSG_REGISTER_NAME_TAKEN = 19;
	public static final int MSG_GET_SESSION_SUCCESS = 20;
	public static final int MSG_GET_SESSION_FAILED = 21;
	public static final int MSG_GET_USER_DOC_SUCCESS = 22;
	public static final int MSG_REGISTER_SUCCESS = 23;

	public static final String source_code = "source_code";
	public static final String data = "data";
	public static final String op_code = "op_code";

	public static final int HEART_BEAT_INTERVAL = 10000;
	public static final int RECONNECT_INTERVAL = 20000;

	// private static final int SOURCE_SQURE=2;
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

	public static String genPublicKey() {
		return "5NCfQnM3buZdfbH2nx7DG=7EJ=sj8Aj5JNqdNufJKD1";
	}

	public static String genPrivateKey() {
		return "18706104611734459693578797982023";
	}

	public static String genRandomPassword() {
		return "123456789";
	}

	public static String genSecKey() {
		return "1234567890";
	}

	public static String genFingerPrint() {
		return "1234567890";
	}

	public static void sleepFor(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	public static void sendMSG(Handler handler, int what, Object obj) {
		Message msg = new Message();
		msg.obj = obj;
		msg.what = what;
		if (obj == null)
			handler.sendEmptyMessage(what);
		else
			handler.sendMessage(msg);
	}
}

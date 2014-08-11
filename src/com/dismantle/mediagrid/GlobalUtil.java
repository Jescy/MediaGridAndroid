package com.dismantle.mediagrid;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;

@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
/**
 * Global utility class to define message type, constants, encoding, time format, encryption and decryption, FontAwesome.
 */
public class GlobalUtil {

	// message type for Handler
	/**
	 * load data success
	 */
	public static final int MSG_LOAD_SUCCESS = 1;
	/**
	 * load data fails
	 */
	public static final int MSG_LOAD_FAILED = 2;
	/**
	 * create directory success
	 */
	public static final int MSG_CREATE_DIR_SUCCESS = 3;
	/**
	 * create directory fails
	 */
	public static final int MSG_CREATE_DIR_FAILED = 4;
	/**
	 * upload file success
	 */
	public static final int MSG_UPLOAD_SUCCESS = 5;
	/**
	 * upload file fails
	 */
	public static final int MSG_UPLOAD_FAILED = 6;
	/**
	 * download file success
	 */
	public static final int MSG_DOWNLOAD_SUCCESS = 7;
	/**
	 * download file fails
	 */
	public static final int MSG_DOWNLOAD_FAILED = 8;
	/**
	 * login success
	 */
	public static final int MSG_LOGIN_SUCCESS = 9;
	/**
	 * login fails
	 */
	public static final int MSG_LOGIN_FAILED = 10;
	/**
	 * save user document success
	 */
	public static final int MSG_SAVE_USER_DOC_SUCCESS = 11;
	/**
	 * save user document fails
	 */
	public static final int MSG_SAVE_USER_DOC_FAILED = 12;
	/**
	 * get database information success
	 */
	public static final int MSG_GET_DB_INFO_SUCCESS = 13;
	/**
	 * get database information fails
	 */
	public static final int MSG_GET_DB_INFO_FAILED = 14;
	/**
	 * long polling user returns
	 */
	public static final int MSG_POLLING_USER = 15;
	/**
	 * long polling chat items returns
	 */
	public static final int MSG_POLLING_CHAT = 16;
	/**
	 * long polling instant message returns
	 */
	public static final int MSG_POLLING_IM = 17;
	/**
	 * getting chat list
	 */
	public static final int MSG_CHAT_LIST = 18;
	/**
	 * registered user name is taken
	 */
	public static final int MSG_REGISTER_NAME_TAKEN = 19;
	/**
	 * get session success
	 */
	public static final int MSG_GET_SESSION_SUCCESS = 20;
	/**
	 * get session failed
	 */
	public static final int MSG_GET_SESSION_FAILED = 21;
	/**
	 * get user document success
	 */
	public static final int MSG_GET_USER_DOC_SUCCESS = 22;
	/**
	 * register name success
	 */
	public static final int MSG_REGISTER_SUCCESS = 23;
	/**
	 * long polling file returns
	 */
	public static final int MSG_POLLING_FILE = 24;
	/**
	 * string for every one
	 */
	public static final String every_one="Everyone";
	/**
	 * heart beat interval
	 */
	public static final int HEART_BEAT_INTERVAL = 10000;
	/**
	 * reconnect network interval
	 */
	public static final int RECONNECT_INTERVAL = 20000;
	/**
	 * interval to shown chat time
	 */
	public static final long CHAT_TIME_INTERVAL =  60*1000;
	
	
	public static String getTimeByFormat(String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		Date curDate = new Date(System.currentTimeMillis());
		String created_at = formatter.format(curDate);

		return created_at;
	}
	/**
	 * generate public key
	 * @return
	 */
	public static String genPublicKey() {
		return "5NCfQnM3buZdfbH2nx7DG=7EJ=sj8Aj5JNqdNufJKD1";
	}
	/**
	 * generate private key
	 * @return
	 */
	public static String genPrivateKey() {
		return "18706104611734459693578797982023";
	}
	/**
	 * generate random password
	 * @return
	 */
	public static String genRandomPassword() {
		return "123456789";
	}
	/**
	 * generate security key
	 * @return
	 */
	public static String genSecKey() {
		return "1234567890";
	}
	/**
	 * generate finger print
	 * @return
	 */
	public static String genFingerPrint() {
		return "1234567890";
	}
	/**
	 * sleep for some time.
	 * @param time
	 */
	public static void sleepFor(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * send message 
	 * @param handler	handler to which the message is sent
	 * @param what	message code
	 * @param obj	message data
	 */
	public static void sendMSG(Handler handler, int what, Object obj) {
		Message msg = new Message();
		msg.obj = obj;
		msg.what = what;
		if (obj == null)
			handler.sendEmptyMessage(what);
		else
			handler.sendMessage(msg);
	}
	
	/**
	 * fontawesome type face
	 */
	private static Typeface mFontAwesome=null;
	/**
	 * get FontAwesome
	 * @param context activity context
	 * @return
	 */
	public static Typeface getFontAwesome(Context context)
	{
		if(mFontAwesome==null)
			mFontAwesome = Typeface.createFromAsset(context.getAssets(), "fontawesome-webfont.ttf");
		return mFontAwesome;
	}
	/**
	 * get formatted date
	 * @param date
	 * @return locale date string
	 */
	@SuppressWarnings("deprecation")
	public static String getFormattedDate(Date date)
	{
		return date.toLocaleString();
	}
}

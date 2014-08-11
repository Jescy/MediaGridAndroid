package com.dismantle.mediagrid;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * CouchDB data provider.
 * 
 * @author Jescy
 * 
 */
public class CouchDB {
	/**
	 * HTTP service provider.
	 */
	private static HttpService mHttpService = HttpService.getInstance();

	/**
	 * login with username and password.
	 * POST /_session 
	 * params: name,password
	 * 
	 * @param username user name
	 * @param password password
	 * @return user's name, role.
	 * @throws JSONException
	 */
	public static JSONObject login(String username, String password)
			throws JSONException {
		JSONObject args = new JSONObject();
		args.put("name", username);
		args.put("password", password);
		JSONObject user = mHttpService.doPost("/_session", args);
		return user;
	}

	/**
	 * log out from CouchDB
	 * DELETE /_session
	 * @return 
	 */
	public static JSONObject logout() {
		JSONObject jsonObject = mHttpService.doDelete("/_session");
		return jsonObject;
	}

	/**
	 * get session from CouchDB, to avoid login every time.
	 * GET /_session
	 * @return if username, then success.
	 */
	public static JSONObject getSession() {
		JSONObject user = mHttpService.doGet("/_session");
		return user;
	}

	/**
	 * get file list.
	 * POST /media/_design/media/_view/files?descending=true&update_seq=true
	 * params: keys
	 * 
	 * @param descending order by descending.
	 * @param update_seq update sequence number.
	 * @param keys paths of the folder.
	 * @return list of files.
	 * @throws Exception
	 */
	public static JSONObject getFiles(boolean descending, boolean update_seq,
			String keys) throws Exception {

		String url = "/media/_design/media/_view/files?";
		url += "descending=" + descending;
		url += "&update_seq=" + update_seq;
		JSONObject args = new JSONObject();

		JSONArray jsonKeys = new JSONArray();
		jsonKeys.put(keys);
		args.put("keys", jsonKeys);

		JSONObject resJson = mHttpService.doPost(url, args);
		return resJson;
	}

	/**
	 * create directory 
	 * POST /media
	 * params: name,dir,type,created_at
	 * @param name name of directory.
	 * @param dir path of the directory
	 * @param created_at created time.
	 * @return JsonObject indicating success or not.
	 * @throws JSONException
	 */
	public static JSONObject createDir(String name, String dir,
			String created_at) throws JSONException {

		String url = "/media";
		JSONObject args = new JSONObject();
		args.put("name", name);
		args.put("dir", dir);
		args.put("type", "DIR");
		args.put("created_at", created_at);

		JSONObject resJson = mHttpService.doPost(url, args);
		return resJson;
	}

	
	/**
	 * create a file document for uploading attachments.
	 * POST /media/_design/media/_update/file
	 * params: type, dir
	 * @param dir path of the file document.
	 * @return	JsonObject indicating success or not.
	 * @throws JSONException
	 */
	public static JSONObject createFileDocument(String dir)
			throws JSONException {
		String url = "/media/_design/media/_update/file";
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("type", "FILE"));
		if (dir != null)
			args.add(new BasicNameValuePair("dir", dir));

		mHttpService.doPostForm(url, args);
		JSONObject resJson = new JSONObject();
		//id and rev information is included in HTTP's header.
		String id = mHttpService.getHeader("X-Couch-Id");
		String rev = mHttpService.getHeader("X-Couch-Update-NewRev");
		resJson.put("id", id);
		resJson.put("rev", rev);
		return resJson;
	}
	/**
	 * download file from CouchDB.
	 * @param url	url of the file in Server.
	 * @param path local path of the file.
	 * @return	true when success
	 */
	public static boolean doDownloadFile(String url, String path) {
		int pos = url.lastIndexOf("/");
		String name = url.substring(pos + 1);
		url = url.substring(0, pos + 1);
		return mHttpService.doDownloadFile(url, name, path);
	}

	
	/**
	 * upload a file to the corresponding document.
	 * POST /media/{id}?_attachments={name}&_rev={rev}
	 * params payload
	 * @param id	document id.
	 * @param rev document version.
	 * @param path local path of the file to upload.
	 * @return	JsonObject indicating success or not.
	 * @throws Exception 
	 */
	public static JSONObject upload(String id, String rev, String path)
			throws Exception {
		JSONObject resJson = null;
		File file = new File(path);
		if (!file.exists()) {
			resJson = new JSONObject();
			resJson.put("error", "file not found");
			return resJson;
		}
		String url = "/media/" + id + "?";
		url += "_attachments=" + file.getName();
		url += "&_rev=" + rev;
		resJson = mHttpService.doPostFile(url, rev, file);
		return resJson;
	}

	// PUT /_users/org.couchdb.user:name
	// params:_id,name,password,roles,type
	/**
	 * register with name and password.
	 * @param name name of user
	 * @param passowrd password of user
	 * @param type user type
	 * @return JsonObject indicating success or not.
	 * @throws JSONException
	 */
	public static JSONObject register(String name, String passowrd, String type)
			throws JSONException {
		String userPrefix = "org.couchdb.user:";
		String userID = userPrefix + name;
		String url = "/_users/" + userID;
		JSONObject args = new JSONObject();
		args.put("_id", userID);
		args.put("name", name);
		args.put("password", passowrd);
		args.put("roles", new JSONArray());
		args.put("type", type);

		JSONObject resJson = mHttpService.doPut(url, args);
		return resJson;
	}


	/** 
	 * get user document.
	 * GET /chat/username
	 * @param username user name
	 * @return JsonObject containing user document.
	 */
	public static JSONObject getUserDoc(String username) {
		String url = "/chat/" + username;
		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}

	// PUT /chat/userid
	// params:_id,_rev,key,rooms,left,type
	/**
	 * save a user's document.
	 * @param userID user's ID
	 * @param rev user document's version.
	 * @param key public key of user
	 * @param type type of user
	 * @param rooms rooms user has logged.
	 * @param left rooms that user has left.
	 * @return	JsonObject indicating success or not.
	 * @throws JSONException
	 */
	public static JSONObject saveUserDoc(String userID, String rev, String key,
			String type, Vector<String> rooms, Vector<String> left)
			throws JSONException {
		String url = "/chat/" + userID;
		JSONObject args = new JSONObject();
		args.put("_id", userID);
		if (rev != null)
			args.put("_rev", rev);
		args.put("key", key);
		args.put("rooms", new JSONArray(rooms));
		args.put("left", new JSONArray(left));
		args.put("type", type);

		JSONObject resJson = mHttpService.doPut(url, args);
		return resJson;
	}

	
	/**
	 * get message of the day.
	 * GET /chat/motd
	 * @return
	 */
	public static JSONObject getMOTD() {
		String url = "/chat/motd";
		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}

	/**
	 * get chat database information(for update sequence number)
	 * GET /chat
	 * @return JsonObject containing database information.
	 */
	public static JSONObject getChatDBInfo() {
		String url = "/chat";
		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}

	/**
	 * long polling for chat items. If chat database changed, returns the
	 * message document's first and last ID.
	 * GET /_changes?heartbeat=10000&filter=chat/chat&room=General&feed=longpoll&since=474
	 * 
	 * @param since update sequence number
	 * @param room room name.
	 * @return If chat database changed, returns the message documents' first and last ID.
	 */
	public static JSONObject longPollingChat(int since, String room) {
		String url = "/chat/_changes?";
		url += "heartbeat=" + GlobalUtil.HEART_BEAT_INTERVAL;
		url += "&filter=" + "chat/chat";
		url += "&room=" + room;
		url += "&feed=" + "longpoll";
		url += "&since=" + since;
		JSONObject resJson = mHttpService.doGetPolling(url);
		return resJson;
	}

	 
	/**
	 * long polling for instant messages. If any, returns the new instant message.
	 * GET /_changes?heartbeat=10000&filter=chat%2Fim&include_docs=true&feed=longpoll&since=407
	 * @param since update sequence number.
	 * @return the new instant message.
	 */
	public static JSONObject longPollingIM(int since) {
		String url = "/chat/_changes?";
		url += "heartbeat=" + GlobalUtil.HEART_BEAT_INTERVAL;
		url += "&filter=" + "chat/im";
		url += "&include_docs=true";
		url += "&feed=" + "longpoll";
		url += "&since=" + since;
		JSONObject resJson = mHttpService.doGetPolling(url);
		return resJson;
	}


	/**
	 * long polling for user list. If user list changed, returns the newly
	 * updated user list.
	 * GET /_changes?heartbeat=10000&filter=chat%2Fuser&include_docs=true&room=General&feed=longpoll&since=476
	 * @param since update sequence number.
	 * @param room room name.
	 * @return the newly updated user list.
	 */
	public static JSONObject longPollingUser(int since, String room) {
		String url = "/chat/_changes?";
		url += "heartbeat=" + GlobalUtil.HEART_BEAT_INTERVAL;
		url += "&filter=" + "chat/user";
		url += "&include_docs=true";
		url += "&room=" + room;
		url += "&feed=" + "longpoll";
		url += "&since=" + since;
		JSONObject resJson = mHttpService.doGetPolling(url);
		return resJson;
	}

	// GET /_changes
	// for file list
	/**
	 * long polling for the file list.
	 * GET /media/_changes?heartbeat=10000&feed=longpoll&since=16
	 * @param since update sequence number
	 * @return the updated file list.
	 */
	public static JSONObject longPollingFile(int since) {
		String url = "/media/_changes?";
		url += "heartbeat=" + GlobalUtil.HEART_BEAT_INTERVAL;
		url += "&feed=" + "longpoll";
		url += "&since=" + since;
		JSONObject resJson = mHttpService.doGetPolling(url);
		return resJson;
	}

	/**
	 * get messages by first and last message ID.
	 * GET /chat/_design/_view/msgs
	 * @param room room name
	 * @param username user name
	 * @param firstMsg first message ID
	 * @param lastMsg last message ID
	 * @return JsonObject containing messages.
	 */
	@SuppressWarnings("deprecation")
	public static JSONObject getMsgs(String room, String username,
			String firstMsg, String lastMsg) {
		String url = "/chat/_design/chat/_view/msgs?";
		JSONArray tmpArray = new JSONArray();
		tmpArray.put(username);
		tmpArray.put(room);
		tmpArray.put(firstMsg);
		String startKey = tmpArray.toString();

		tmpArray = new JSONArray();
		tmpArray.put(username);
		tmpArray.put(room);
		tmpArray.put(lastMsg);
		String endKey = tmpArray.toString();

		url += "startkey=" + URLEncoder.encode(startKey);
		url += "&endkey=" + URLEncoder.encode(endKey);

		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}


	/**
	 * post message to server.
	 * POST /chat/_design/chat/_update/chatitem
	 * @param msg message to post
	 * @return JsonObject indicating success or not.
	 */
	public static JSONObject postMsg(JSONObject msg) {
		String url = "/chat/_design/chat/_update/chatitem";
		JSONObject resJson = mHttpService.doPostForm(url, msg.toString());
		return resJson;
	}

}

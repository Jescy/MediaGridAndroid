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

public class CouchDB {
	private static HttpService mHttpService = HttpService.getInstance();

	// POST /_session
	// params: name,password
	public static JSONObject login(String username, String password)
			throws JSONException {
		JSONObject args = new JSONObject();
		args.put("name", username);
		args.put("password", password);
		JSONObject user = mHttpService.doPost("/_session", args);
		return user;
	}

	// DELETE /_session
	public static JSONObject logout()
	{
		JSONObject jsonObject=mHttpService.doDelete("/_session");
		return jsonObject;
	}
	// GET /_session
	public static JSONObject getSession() {
		JSONObject user = mHttpService.doGet("/_session");
		return user;
	}

	// POST /media/_design/media/_view/files?descending=true&update_seq=true
	// params: keys
	public static JSONObject getFiles(boolean descending, boolean update_seq,
			String keys) throws Exception {

		String url = "/media/_design/media/_view/files";
		JSONObject args = new JSONObject();
		args.put("descending", descending);
		args.put("update_seq", update_seq);

		JSONArray jsonKeys = new JSONArray();
		jsonKeys.put(keys);
		args.put("keys", jsonKeys);

		JSONObject resJson = mHttpService.doPost(url, args);
		return resJson;
	}

	// POST /media
	// params: name,dir,type,created_at:
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

	// POST /media/_design/media/_update/file
	// params: type, dir
	public static JSONObject createFileDocument(String dir)
			throws JSONException {
		String url = "/media/_design/media/_update/file";
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("type", "FILE"));
		if (dir != null)
			args.add(new BasicNameValuePair("dir", dir));

		mHttpService.doPostForm(url, args);
		JSONObject resJson = new JSONObject();
		String id = mHttpService.getHeader("X-Couch-Id");
		String rev = mHttpService.getHeader("X-Couch-Update-NewRev");
		resJson.put("id", id);
		resJson.put("rev", rev);
		return resJson;
	}

	public static boolean doDownloadFile(String url, String path) {
		return mHttpService.doDownloadFile(url, path);
	}

	// POST /media/{id}?_attachments={name}&_rev={rev}
	// params payload
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

	// GET /chat/username
	public static JSONObject getUserDoc(String username) {
		String url = "/chat/" + username;
		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}

	// PUT /chat/userid
	// params:_id,_rev,key,rooms,left,type
	public static JSONObject saveUserDoc(String userID, String rev, String key,
			String type, Vector<String> rooms, Vector<String> left)
			throws JSONException {
		String url = "/chat/" + userID;
		JSONObject args = new JSONObject();
		args.put("_id", userID);
		if(rev!=null)
			args.put("_rev", rev);
		args.put("key", key);
		args.put("rooms", new JSONArray(rooms));
		args.put("left", new JSONArray(left));
		args.put("type", type);

		JSONObject resJson = mHttpService.doPut(url, args);
		return resJson;
	}

	// GET /chat/motd
	public static JSONObject getMOTD() {
		String url = "/chat/motd";
		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}

	// GET /chat
	// database information
	public static JSONObject getChatDBInfo() {
		String url = "/chat";
		JSONObject resJson = mHttpService.doGet(url);
		return resJson;
	}

	// GET /_changes
	// for chat message
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

	// GET /_changes
	// for IM message
	//http://127.0.0.1:5984/chat/_changes?heartbeat=10000&filter=chat%2Fim&include_docs=true&feed=longpoll&since=407
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

	// GET /_changes
	// for user list
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

	// GET /chat/_design/_view/msgs
	// for new message
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

	// POST /chat/_design/chat/_update/chatitem
	// params:
	// post chat message
	public static JSONObject postMsg(JSONObject msg) {
		String url = "/chat/_design/chat/_update/chatitem";
		JSONObject resJson = mHttpService.doPostForm(url, msg.toString());
		return resJson;
	}

	public static JSONArray getFileList() throws Exception {

		JSONArray list = null;
		String strData = "[{'file_name':'Photo',		'file_size':0,		'upload_time':'2013/06/01 10:01'},"
				+ "{'file_name':'Photo',		'file_size':0,		'upload_time':'2013/11/02 09:01'},"
				+ "{'file_name':'Camera',		'file_size':0,		'upload_time':'2013/05/03 09:20'},"
				+ "{'file_name':'MyFile',		'file_size':0,		'upload_time':'2012/03/03 09:40'},"
				+ "{'file_name':'Document.docx','file_size':3068,	'upload_time':'2013/08/18 09:00'},"
				+ " {'file_name':'ReadMe.txt',	'file_size':1234,	'upload_time':'2014/03/03 10:28'}]";
		list = new JSONArray(strData);

		return list;
	}

	public static JSONArray getMsgList() throws Exception {
		JSONArray list = null;
		String strData = "[{'chat_from':'John',		'chat_to':'Jescy',		'chat_time':'03/03 09:00','chat_msg':'How are you, Jescy?'},"
				+ "{'chat_from':'Jescy',	'chat_to':'John',		'chat_time':'03/03 09:02','chat_msg':'Good, would you like to come to my house for a party tonight?'},"
				+ "{'chat_from':'John',		'chat_to':'Jescy',		'chat_time':'03/03 09:00','chat_msg':'How are you, Jescy?'},"
				+ "{'chat_from':'John',		'chat_to':'Jescy',		'chat_time':'03/03 09:00','chat_msg':'How are you, Jescy?'},"
				+ "{'chat_from':'John',		'chat_to':'Jescy',		'chat_time':'03/03 09:00','chat_msg':'How are you, Jescy?'},"
				+ "{'chat_from':'John',		'chat_to':'Jescy',		'chat_time':'03/03 09:00','chat_msg':'How are you, Jescy?'}]";
		list = new JSONArray(strData);
		return list;
	}

	public static JSONArray getMemberList() throws Exception {
		JSONArray list = null;

		String strData = "[{'member_name':'John'},"
				+ "{'member_name':'Jescy'}," + "{'member_name':'Magie'},"
				+ "{'member_name':'Maria'}," + "{'member_name':'Dara'},"
				+ "{'member_name':'John'}," + "{'member_name':'John'},"
				+ "{'member_name':'John'}]";

		list = new JSONArray(strData);
		return list;
	}

	// handle tags
	static String delteTags(String input) {
		int i = 0;
		while (input.charAt(i++) != '<')
			;
		while (input.charAt(i++) != '>')
			;

		while (input.charAt(i++) != '<')
			;
		while (input.charAt(i++) != '>')
			;

		int j = input.length() - 1;
		while (input.charAt(j--) != '>')
			;
		while (input.charAt(j--) != '<')
			;
		j++;
		if (i > j)
			return "";
		input = input.substring(i, j);
		return input;

	}

}

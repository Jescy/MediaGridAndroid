package com.dismantle.mediagrid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.spec.EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.location.Location;
import android.util.Base64;

public class HttpService {
	private String serverIP = "192.168.1.105";
	private int serverPort = 5984;
	private String baseURL = "http://" + serverIP + ":" + serverPort;
	private static HttpService httpService = null;
	private DefaultHttpClient httpClient = null;

	private HttpService() {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, true);
		ConnManagerParams.setTimeout(params, 1000);
		HttpConnectionParams.setConnectionTimeout(params, 2000);
		HttpConnectionParams.setSoTimeout(params, 4000);
		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
				params, schReg);
		httpClient = new DefaultHttpClient(conMgr, params);

	}

	public static HttpService getInstance() {
		if (httpService == null)
			httpService = new HttpService();
		return httpService;
	}

	public HttpService setServer(String ip, int port) {
		serverIP = ip;
		serverPort = port;
		baseURL = "http://" + serverIP + ":" + serverPort;
		return getInstance();
	}

	public String getServerIP() {
		return serverIP;
	}

	public int getServerPort() {
		return serverPort;
	}

	public JSONObject doGet(String url) {
		HttpGet httpGet = new HttpGet(baseURL + url);
		httpGet.setHeader("Content-Type", "application/json");
		JSONObject jsonObject = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);// execute get
			String resultString = EntityUtils.toString(response.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	public JSONObject doPost(String url, JSONObject args) {
		HttpPost httppost = new HttpPost(baseURL + url);
		JSONObject jsonObject = null;
		try {
			httppost.setHeader("Content-Type", "application/json");
			httppost.setEntity(new StringEntity(args.toString(), "UTF-8")); // execute
																			// post
			HttpResponse response = httpClient.execute(httppost);
			String resultString = EntityUtils.toString(response.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	public JSONObject doPut(String url, JSONObject args) {
		HttpPut httpput = new HttpPut(baseURL + url);
		JSONObject jsonObject = null;
		try {
			httpput.setHeader("Content-Type", "application/json");
			httpput.setEntity(new StringEntity(args.toString(), "UTF-8")); // execute
																			// post
			HttpResponse response = httpClient.execute(httpput);
			String resultString = EntityUtils.toString(response.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	public String uploadFile(String fileName, String severPath) {
		HttpResponse response;
		try {
			String posix = fileName.substring(fileName.lastIndexOf('.'));
			File file = new File(fileName);
			long len = file.length();

			FileInputStream fis = new FileInputStream(fileName);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int count = 0;
			while ((count = fis.read(buffer)) >= 0) {
				baos.write(buffer, 0, count);
			}
			String data = new String(Base64.encode(baos.toByteArray(),
					Base64.DEFAULT), "UTF-8"); // ½øÐÐBase64±àÂë

			HttpPost httppost = new HttpPost(baseURL + "/GetPicNew");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

			nameValuePairs
					.add(new BasicNameValuePair("file", severPath + posix));

			nameValuePairs.add(new BasicNameValuePair("fileData", data));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

			response = new DefaultHttpClient().execute(httppost);

			return severPath + posix;
		} catch (Exception e) {
			return null;
		}
	}
}
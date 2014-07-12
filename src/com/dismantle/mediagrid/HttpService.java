package com.dismantle.mediagrid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.http.Header;
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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class HttpService {
	private String serverIP = "192.168.1.100";
	private int serverPort = 5984;
	private String baseURL = "http://" + serverIP + ":" + serverPort;
	private static HttpService httpService = null;
	private DefaultHttpClient httpClient = null;
	private HttpResponse httpResponse = null;

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
			httpResponse = httpClient.execute(httpGet);// execute get
			String resultString = EntityUtils
					.toString(httpResponse.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
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
			httpResponse = httpClient.execute(httppost);
			String resultString = EntityUtils
					.toString(httpResponse.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (JSONException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	//for those only supporting application/x-www-form-urlencoded, I dont know why the application/json is not working
	public JSONObject doPostForm(String url, List<NameValuePair> args) {
		HttpPost httppost = new HttpPost(baseURL + url);
		JSONObject jsonObject = null;
		try {
			httppost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			httppost.setEntity(new UrlEncodedFormEntity(args,"UTF-8")); // execute
																// post
			httpResponse = httpClient.execute(httppost);
			String resultString = EntityUtils
					.toString(httpResponse.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (JSONException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace(System.err);
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
			httpResponse = httpClient.execute(httpput);
			String resultString = EntityUtils
					.toString(httpResponse.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}

	public JSONObject doPostFile(String url, String rev, File uploadFile) {

		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

		final String BOUNDARY = "--------MediaGrid------"; // data boundry
		final String endline = "--" + BOUNDARY + "--\r\n";// 数据结束标志
		JSONObject jsonObject = null;
		try {
			// write file data
			int fileDataLength = 0;
			// file discription
			StringBuilder fileExplain = new StringBuilder();
			fileExplain.append("--");
			fileExplain.append(BOUNDARY);
			fileExplain.append("\r\n");
			fileExplain
					.append("Content-Disposition: form-data;name=\"_attachments\";filename=\""
							+ uploadFile.getName() + "\"\r\n");
			fileExplain
					.append("Content-Type: application/octet-stream\r\n\r\n");
			fileExplain.append("\r\n");
			fileDataLength += fileExplain.length();
			fileDataLength += uploadFile.length();

			arrayOutputStream.write(fileExplain.toString().getBytes());

			byte[] buffer = new byte[1024];
			int len = 0;
			FileInputStream fis = new FileInputStream(uploadFile);
			while ((len = fis.read(buffer, 0, 1024)) != -1) {
				arrayOutputStream.write(buffer, 0, len);
			}
			fis.close();

			arrayOutputStream.write("\r\n".getBytes());

			// rev parameter
			StringBuilder revEntity = new StringBuilder();
			revEntity.append("--");
			revEntity.append(BOUNDARY);
			revEntity.append("\r\n");
			revEntity
					.append("Content-Disposition: form-data; name=\"_rev\"\r\n\r\n");
			revEntity.append(rev);
			revEntity.append("\r\n");

			arrayOutputStream.write(revEntity.toString().getBytes());
			// end line of data
			arrayOutputStream.write(endline.getBytes());
			ByteArrayEntity arrayEntity = new ByteArrayEntity(
					arrayOutputStream.toByteArray());

			HttpPost httpPost = new HttpPost(baseURL + url);
			httpPost.setHeader("Content-Type", "multipart/form-data; boundary="
					+ BOUNDARY);
			//httpPost.setHeader("Content-Length", String.valueOf(dataLength));
			httpPost.setHeader("Connection", "Keep-Alive");
			httpPost.setHeader("Cache-Control", "max-age=0");
			httpPost.setHeader("Referer",baseURL+"/media/_design/media/files.html");
			httpPost.setEntity(arrayEntity);
			httpResponse = httpClient.execute(httpPost);
			String resultString = EntityUtils
					.toString(httpResponse.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}

	public String getHeader(String headerName) {
		Header[] headers = httpResponse.getHeaders(headerName);
		if (headers.length == 0)
			return null;

		return headers[0].getValue();
	}
}
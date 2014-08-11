package com.dismantle.mediagrid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
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

import android.net.Uri;

public class HttpService {
	/**
	 * server ip
	 */
	private String mServerIP = "127.0.0.1";
	/**
	 * server port
	 */
	private int mServerPort = 5984;
	/**
	 * basic url http://ip:port
	 */
	private String mBaseURL = "http://" + mServerIP + ":" + mServerPort;
	/**
	 * http service instance
	 */
	private static HttpService mHttpService = null;
	/**
	 * http client
	 */
	private DefaultHttpClient mHttpClient = null;
	/**
	 * http response
	 */
	private HttpResponse mHttpResponse = null;

	private HttpService() {
		mHttpClient = createHttpClient();
	}
	/**
	 * create http client
	 * @return
	 */
	private DefaultHttpClient createHttpClient() {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, true);
		ConnManagerParams.setTimeout(params, 20000);
		HttpConnectionParams.setConnectionTimeout(params, 20000);
		HttpConnectionParams.setSoTimeout(params, 40000);
		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 5984));
		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
				params, schReg);
		return new DefaultHttpClient(conMgr, params);
	}
	/**
	 * get http service instance
	 * @return
	 */
	public static HttpService getInstance() {
		if (mHttpService == null)
			mHttpService = new HttpService();
		return mHttpService;
	}
	/**
	 * reset server's ip and port
	 * @param ip
	 * @param port
	 * @return
	 */
	public HttpService setServer(String ip, int port) {
		mServerIP = ip;
		mServerPort = port;
		mBaseURL = "http://" + mServerIP + ":" + mServerPort;
		return getInstance();
	}
	/**
	 * get server ip
	 * @return
	 */
	public String getServerIP() {
		return mServerIP;
	}
	/**
	 * get server port
	 * @return
	 */
	public int getServerPort() {
		return mServerPort;
	}
	/**
	 * execute a get request
	 * @param url url address
	 * @return request's result
	 */
	public JSONObject doGet(String url) {
		HttpGet httpGet = new HttpGet(mBaseURL + url);
		httpGet.setHeader("Content-Type", "application/json");
		JSONObject jsonObject = null;
		try {
			mHttpResponse = mHttpClient.execute(httpGet);// execute get
			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	/**
	 * do a long polling get function
	 * @param url url
	 * @return request's result
	 */
	public JSONObject doGetPolling(String url) {

		DefaultHttpClient client = createHttpClient();
		client.setCookieStore(mHttpClient.getCookieStore());
		HttpGet httpGet = new HttpGet(mBaseURL + url);
		httpGet.setHeader("Content-Type", "application/json");
		httpGet.setHeader("Accept", "application/json");
		JSONObject jsonObject = null;
		while (true) {
			try {
				mHttpResponse = client.execute(httpGet);// execute get
				String resultString = EntityUtils.toString(mHttpResponse
						.getEntity());
				jsonObject = new JSONObject(resultString);
				break;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				GlobalUtil.sleepFor(GlobalUtil.RECONNECT_INTERVAL);
			}
		}
		return jsonObject;
	}
	/**
	 * execute a post request
	 * @param url url
	 * @param args post argument
	 * @return request's result
	 */
	public JSONObject doPost(String url, JSONObject args) {
		HttpPost httppost = new HttpPost(mBaseURL + url);
		JSONObject jsonObject = null;
		try {
			httppost.setHeader("Content-Type", "application/json");
			httppost.setEntity(new StringEntity(args.toString(), "UTF-8")); // execute
																			// post
			mHttpResponse = mHttpClient.execute(httppost);

			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (JSONException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}

	// for those only supporting application/x-www-form-urlencoded, I dont know
	// why the application/json is not working
	/**
	 * for those only supporting application/x-www-form-urlencoded, I dont know why the application/json is not working
	 * @param url url
	 * @param args argument
	 * @return request's result
	 */
	public JSONObject doPostForm(String url, List<NameValuePair> args) {
		HttpPost httppost = new HttpPost(mBaseURL + url);
		JSONObject jsonObject = null;
		try {
			httppost.setHeader("Content-Type",
					"application/x-www-form-urlencoded; charset=UTF-8");
			httppost.setEntity(new UrlEncodedFormEntity(args, "UTF-8")); // execute
			// post
			mHttpResponse = mHttpClient.execute(httppost);
			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (JSONException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	/**
	 * post with a string payload
	 * @param url url 
	 * @param payload payload string
	 * @return request's result
	 */
	public JSONObject doPostForm(String url, String payload) {
		HttpPost httppost = new HttpPost(mBaseURL + url);
		JSONObject jsonObject = null;
		try {
			httppost.setHeader("Content-Type",
					"application/x-www-form-urlencoded; charset=UTF-8");
			StringEntity stringEntity = new StringEntity(Uri.encode(payload,
					"UTF-8"));
			httppost.setEntity(stringEntity);
			// execute post
			mHttpResponse = mHttpClient.execute(httppost);
			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (JSONException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	/**
	 * execute a put request
	 * @param url url 
	 * @param args arguments
	 * @return request's result
	 */
	public JSONObject doPut(String url, JSONObject args) {
		HttpPut httpput = new HttpPut(mBaseURL + url);
		JSONObject jsonObject = null;
		try {
			httpput.setHeader("Content-Type", "application/json");
			httpput.setEntity(new StringEntity(args.toString(), "UTF-8")); // execute
																			// post
			mHttpResponse = mHttpClient.execute(httpput);
			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	/**
	 * execute a delete request
	 * @param url url 
	 * @return request's result
	 */
	public JSONObject doDelete(String url) {
		HttpDelete httpDelete = new HttpDelete(mBaseURL + url);
		JSONObject jsonObject = null;
		try {
			mHttpResponse = mHttpClient.execute(httpDelete);
			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	/**
	 * post a file to server
	 * @param url url 
	 * @param rev version
	 * @param uploadFile file to upload
	 * @return request's result
	 */
	public JSONObject doPostFile(String url, String rev, File uploadFile) {

		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

		final String BOUNDARY = "--------MediaGrid------"; // data boundry
		final String endline = "--" + BOUNDARY + "--\r\n";// 数据结束标志
		JSONObject jsonObject = null;
		try {
			// file discription
			StringBuilder fileExplain = new StringBuilder();
			fileExplain.append("--");
			fileExplain.append(BOUNDARY);
			fileExplain.append("\r\n");
			fileExplain
					.append("Content-Disposition: form-data;name=\"_attachments\";filename=\""
							+ uploadFile.getName() + "\"\r\n");
			// MIME type
			fileExplain.append("Content-Type: ");
			fileExplain.append(MIMEType.getMIMEType(uploadFile) + "\r\n\r\n");
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

			HttpPost httpPost = new HttpPost(mBaseURL + url);
			httpPost.setHeader("Content-Type", "multipart/form-data; boundary="
					+ BOUNDARY);
			// httpPost.setHeader("Content-Length", String.valueOf(dataLength));
			httpPost.setHeader("Connection", "Keep-Alive");
			httpPost.setHeader("Cache-Control", "max-age=0");
			httpPost.setHeader("Referer", mBaseURL
					+ "/media/_design/media/files.html");
			httpPost.setEntity(arrayEntity);
			mHttpResponse = mHttpClient.execute(httpPost);
			String resultString = EntityUtils.toString(mHttpResponse
					.getEntity());
			jsonObject = new JSONObject(resultString);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return jsonObject;
	}
	/**
	 * download file
	 * @param urlPath url
	 * @param name name
	 * @param path path
	 * @return true if success
	 */
	public boolean doDownloadFile(String urlPath, String name, String path) {
		try {

			File file = new File(path);
			if (file.exists()) {
				file.delete();
			}
			URL urlURL = new URL(mBaseURL + urlPath + Uri.encode(name));
			URLConnection con = urlURL.openConnection();

			InputStream is = con.getInputStream();
			byte[] bs = new byte[1024];
			int len;
			OutputStream os = new FileOutputStream(file);
			while ((len = is.read(bs)) != -1) {
				os.write(bs, 0, len);
			}
			os.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return false;
		}
		return true;
	}
	/**
	 * get header of the request
	 * @param headerName name of the header
	 * @return header's content
	 */
	public String getHeader(String headerName) {
		Header[] headers = mHttpResponse.getHeaders(headerName);
		if (headers.length == 0)
			return null;

		return headers[0].getValue();
	}
}
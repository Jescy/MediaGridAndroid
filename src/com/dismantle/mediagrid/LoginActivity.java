package com.dismantle.mediagrid;

import java.util.Vector;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dismantle.mediagrid.IPDialog.onIPInputDialogProcess;
import com.google.gson.Gson;

@SuppressLint("HandlerLeak")
public class LoginActivity extends ActionBarActivity {
	/**
	 * error promotion message
	 */
	private TextView mTvMSG = null;
	/**
	 * name buttons
	 */
	private Button[] mBtnNames = null;
	/**
	 * saved names
	 */
	private Vector<String> names = null;
	/**
	 * saved passwords
	 */
	private Vector<String> passwords = null;
	/**
	 * progress dialog 
	 */
	private ProgressDialog progressDialog = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);

		mTvMSG = (TextView) findViewById(R.id.tv_msg);
		
		setTitle(getString(R.string.media_grid));
		
		//get the server configure data from SharedPreferences
		SharedPreferences sp = LoginActivity.this.getSharedPreferences(
				"ServerConfig", MODE_PRIVATE);
		String lastIP = sp.getString("IP", "127.0.0.1");
		int lastPort = sp.getInt("port", 5984);
		HttpService.getInstance().setServer(lastIP, lastPort);
		//set server inital data
		final TextView tvServer = (TextView) findViewById(R.id.tv_server);
		tvServer.setText(lastIP + ":" + lastPort);
		
		Button btnConfig = (Button) findViewById(R.id.btn_config);
		btnConfig.setTypeface(GlobalUtil.getFontAwesome(this));
		btnConfig.setOnClickListener(new OnClickListener() {
			/**
			 * click to show the server configure
			 */
			@Override
			public void onClick(View arg0) {
				SharedPreferences sp = LoginActivity.this.getSharedPreferences(
						"ServerConfig", MODE_PRIVATE);
				String lastIP = sp.getString("IP", "127.0.0.1");

				int lastPort = sp.getInt("port", 5984);

				IPDialog.showIPInputDialog(LoginActivity.this,
						"Configure Server", lastIP, lastPort,
						new onIPInputDialogProcess() {
							@Override
							public void onIPInputConfirm(String ip, int port) {
								HttpService.getInstance().setServer(ip, port);
								SharedPreferences sp = LoginActivity.this
										.getSharedPreferences("ServerConfig",
												MODE_PRIVATE);
								Editor editor = sp.edit();
								editor.putString("IP", ip);
								editor.putInt("port", port);
								editor.commit();
								tvServer.setText(ip + ":" + port);
							}

							@Override
							public void onIPInputCancel() {

							}
						});
			}
		});
		
		Button btnOK = (Button) findViewById(R.id.btn_login);
		btnOK.setTypeface(GlobalUtil.getFontAwesome(this));
		btnOK.setOnClickListener(new OnClickListener() {
			/**
			 * login button listener
			 */
			@Override
			public void onClick(View arg0) {
				//show progress dialog
				final EditText txtUserName = (EditText) findViewById(R.id.txt_username);
				progressDialog = ProgressDialog.show(LoginActivity.this, "Login...", "Please wait to login...");
				progressDialog.setCancelable(true);
				new Thread() {

					@Override
					public void run() {
						
						JSONObject res = null;
						try {
							//generate random password
							String userName = txtUserName.getText().toString();
							String password = GlobalUtil.genRandomPassword();
							// register to server
							res = CouchDB.register(userName,
									GlobalUtil.genRandomPassword(), "user");
							// send result message
							if (res.has("error")) {
								GlobalUtil.sendMSG(handler,
										GlobalUtil.MSG_REGISTER_NAME_TAKEN,
										null);
								txtUserName.setSelectAllOnFocus(true);
								return;
							}
							GlobalUtil.sendMSG(handler,
									GlobalUtil.MSG_REGISTER_SUCCESS, userName
											+ ":" + password);
							// save this to local storage
							saveName(userName, password);

						} catch (Exception e) {
							e.printStackTrace(System.err);
							GlobalUtil.sendMSG(handler,
									GlobalUtil.MSG_LOAD_FAILED,
									null);
						}

					}
				}.start();

			}
		});
		//initialize name buttons
		initNames();
		
	}

	@Override
	/**
	 * when resume, try login with session.
	 */
	protected void onResume() {
		//try login with session.
		loginWithSession();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			mTvMSG.setText("");
			String userName = "";
			String password = "";
			switch (msg.what) {
			case GlobalUtil.MSG_GET_SESSION_FAILED:
				//if get session fails
				break;
			case GlobalUtil.MSG_REGISTER_SUCCESS:
				// if register success, then login with username and password
				String strs[] = msg.obj.toString().split(":");
				userName = strs[0];
				password = strs[1];
				login(userName, password);
				break;
			case GlobalUtil.MSG_GET_SESSION_SUCCESS:
			case GlobalUtil.MSG_LOGIN_SUCCESS:
				userName = msg.obj.toString();
				// get user document
				getUserDoc(userName);
				// dismiss dialog
				progressDialog.dismiss();
				break;
			case GlobalUtil.MSG_GET_USER_DOC_SUCCESS:
				// if get user document success
				UserDoc userDoc = (UserDoc) msg.obj;
				// start the main activity
				Intent intent = new Intent(LoginActivity.this,
						MainActivity.class);
				Bundle bundle = new Bundle();
				bundle.putSerializable("userDoc", userDoc);
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			case GlobalUtil.MSG_REGISTER_NAME_TAKEN:
				// if registered user name is already taken, show error message
				mTvMSG.setText("user name already taken");
				progressDialog.dismiss();
				break;
			case GlobalUtil.MSG_LOAD_FAILED:
				// if failes, then promote to check network or server configure
				mTvMSG.setText("Failed. Check your network or server configure");
				progressDialog.dismiss();
				break;
			default:
				break;
			}
		};
	};
	/**
	 * initialize saved names from SharedPreferences
	 */
	private void initNames() {
		mBtnNames = new Button[4];
		mBtnNames[0] = (Button) findViewById(R.id.btn_name0);
		mBtnNames[1] = (Button) findViewById(R.id.btn_name1);
		mBtnNames[2] = (Button) findViewById(R.id.btn_name2);
		mBtnNames[3] = (Button) findViewById(R.id.btn_name3);
		// get saved names
		SharedPreferences sp = LoginActivity.this.getSharedPreferences(
				"SavedNames", MODE_PRIVATE);
		names = new Vector<String>();
		passwords = new Vector<String>();
		for (int i = 0; i < mBtnNames.length; i++) {
			String name = sp.getString("name" + i, "");
			String password = sp.getString("password" + i, "");
			names.add(name);
			passwords.add(password);
			if (name == "")
				mBtnNames[i].setVisibility(View.INVISIBLE);
			else {
				if (name.length() > 9)
					name = name.substring(0, 9) + "*";
				mBtnNames[i].setText(name);
				mBtnNames[i].setVisibility(View.VISIBLE);
			}
			mBtnNames[i].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View btn) {
					Button curButton = (Button) btn;
					int id = curButton.getId();
					//get index of button
					int index = -1;
					switch (id) {
					case R.id.btn_name0:
						index = 0;
						break;
					case R.id.btn_name1:
						index = 1;
						break;
					case R.id.btn_name2:
						index = 2;
						break;
					case R.id.btn_name3:
						index = 3;
						break;
					default:
						break;
					}
					//then login with name and password
					String name = names.elementAt(index);
					String password = passwords.elementAt(index);
					login(name, password);

				}
			});

		}

	}
	/**
	 * save name and password into local storage
	 * @param name name
	 * @param password password
	 */
	private void saveName(String name, String password) {
		names.insertElementAt(name, 0);
		passwords.removeElementAt(passwords.size() - 1);

		passwords.insertElementAt(password, 0);
		names.removeElementAt(names.size() - 1);

		SharedPreferences sp = LoginActivity.this.getSharedPreferences(
				"SavedNames", MODE_PRIVATE);
		Editor editor = sp.edit();
		for (int i = 0; i < names.size(); i++) {
			editor.putString("name" + i, names.get(i));
			editor.putString("password" + i, passwords.get(i));
		}
		editor.commit();
	}
	/**
	 * login with session
	 */
	private void loginWithSession() {
		new Thread() {

			@Override
			public void run() {
				//get session from server
				JSONObject resJson = CouchDB.getSession();
				try {
					if (resJson != null) {
						JSONObject userCtx = resJson.getJSONObject("userCtx");
						String name = userCtx.getString("name");
						if (!name.equals("") && !name.equals("null")) {

							GlobalUtil.sendMSG(handler,
									GlobalUtil.MSG_GET_SESSION_SUCCESS, name);
							return;
						}
					}
					GlobalUtil.sendMSG(handler,
							GlobalUtil.MSG_GET_SESSION_FAILED, null);

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}.start();
	}
	/**
	 * login with username and password
	 * @param userName user name
	 * @param password password
	 */
	private void login(final String userName, final String password) {
		// show progress dialog
		progressDialog = ProgressDialog.show(LoginActivity.this, "Login...", "Please wait to login...");
		progressDialog.setCancelable(true);
		
		new Thread() {
			@Override
			public void run() {
				// login
				JSONObject res;
				try {
					res = CouchDB.login(userName, password);
					//send result message
					if (res.has("error")) {
						GlobalUtil.sendMSG(handler,
								GlobalUtil.MSG_LOGIN_FAILED, null);
						return;
					}
					GlobalUtil.sendMSG(handler, GlobalUtil.MSG_LOGIN_SUCCESS,
							userName);
				} catch (Exception e) {
					e.printStackTrace();
					GlobalUtil.sendMSG(handler,
							GlobalUtil.MSG_LOAD_FAILED,
							null);
				}

			}
		}.start();
	}
	/**
	 * get user document
	 * @param userName
	 */
	private void getUserDoc(final String userName) {
		new Thread() {

			@Override
			public void run() {
				JSONObject res = CouchDB.getUserDoc(userName);
				UserDoc userDoc = null;
				if (res.has("error")) {// no document, then create a new one 
					userDoc = new UserDoc();
					userDoc.rooms.add("General");
					userDoc._id = userName;
					userDoc.type = "USER";
					userDoc.key = GlobalUtil.genPublicKey();
				} else {
					// existing document
					Gson gson = new Gson();
					userDoc = gson.fromJson(res.toString(), UserDoc.class);
				}
				GlobalUtil.sendMSG(handler,
						GlobalUtil.MSG_GET_USER_DOC_SUCCESS, userDoc);
			}
		}.start();

	}

}

package com.dismantle.mediagrid;

import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;
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

public class LoginActivity extends ActionBarActivity {

	private TextView mTvMSG = null;
	private Button[] mBtnNames = null;
	private Vector<String> names = null;
	private Vector<String> passwords = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);

		mTvMSG = (TextView) findViewById(R.id.tv_msg);

		SharedPreferences sp = LoginActivity.this.getSharedPreferences(
				"ServerConfig", MODE_PRIVATE);
		String lastIP = sp.getString("IP", "127.0.0.1");
		int lastPort = sp.getInt("port", 5984);
		HttpService.getInstance().setServer(lastIP, lastPort);

		final TextView tvServer = (TextView) findViewById(R.id.tv_server);
		tvServer.setText(lastIP + ":" + lastPort);

		Button btnConfig = (Button) findViewById(R.id.btn_config);
		btnConfig.setOnClickListener(new OnClickListener() {
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
		btnOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				final EditText txtUserName = (EditText) findViewById(R.id.txt_username);

				new Thread() {

					@Override
					public void run() {
						// String resString=HttpService.doGet("/_session");
						JSONObject res = null;
						try {
							String userName = txtUserName.getText().toString();
							String password = GlobalUtil.genRandomPassword();
							// register
							res = CouchDB.register(userName,
									GlobalUtil.genRandomPassword(), "user");
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
						}

					}
				}.start();

			}
		});
		initNames();

	}

	@Override
	protected void onResume() {
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

				break;
			case GlobalUtil.MSG_REGISTER_SUCCESS:
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

				break;
			case GlobalUtil.MSG_GET_USER_DOC_SUCCESS:
				UserDoc userDoc = (UserDoc) msg.obj;
				Intent intent = new Intent(LoginActivity.this,
						MainActivity.class);
				Bundle bundle = new Bundle();
				bundle.putSerializable("userDoc", userDoc);
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			case GlobalUtil.MSG_REGISTER_NAME_TAKEN:
				mTvMSG.setText("user name already taken");
				break;
			case GlobalUtil.MSG_LOAD_FAILED:
				mTvMSG.setText("Login in failed");
				break;
			default:
				break;
			}
		};
	};

	private void initNames() {
		mBtnNames = new Button[6];
		mBtnNames[0] = (Button) findViewById(R.id.btn_name0);
		mBtnNames[1] = (Button) findViewById(R.id.btn_name1);
		mBtnNames[2] = (Button) findViewById(R.id.btn_name2);
		mBtnNames[3] = (Button) findViewById(R.id.btn_name3);
		mBtnNames[4] = (Button) findViewById(R.id.btn_name4);
		mBtnNames[5] = (Button) findViewById(R.id.btn_name5);

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
				if(name.length()>6)
					name=name.substring(0,6)+"*";
				mBtnNames[i].setText(name);
				mBtnNames[i].setVisibility(View.VISIBLE);
			}
			mBtnNames[i].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View btn) {
					Button curButton = (Button) btn;
					int id = curButton.getId();
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
					case R.id.btn_name4:
						index = 4;
						break;
					case R.id.btn_name5:
						index = 5;
						break;
					default:
						break;
					}
					String name = names.elementAt(index);
					String password = passwords.elementAt(index);
					login(name, password);
					
				}
			});

		}

	}

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

	private void loginWithSession() {
		new Thread() {

			@Override
			public void run() {
				JSONObject resJson = CouchDB.getSession();
				try {
					JSONObject userCtx = resJson.getJSONObject("userCtx");
					String name = userCtx.getString("name");
					if (!name.equals("") && !name.equals("null")) {

						GlobalUtil.sendMSG(handler,
								GlobalUtil.MSG_GET_SESSION_SUCCESS, name);

					} else {
						GlobalUtil.sendMSG(handler,
								GlobalUtil.MSG_GET_SESSION_FAILED, null);
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}

			}

		}.start();
	}

	private void login(final String userName, final String password) {
		new Thread() {
			@Override
			public void run() {
				// login
				JSONObject res;
				try {
					res = CouchDB.login(userName, password);
					if (res.has("error")) {
						GlobalUtil.sendMSG(handler,
								GlobalUtil.MSG_LOGIN_FAILED, null);
						return;
					}
					GlobalUtil.sendMSG(handler, GlobalUtil.MSG_LOGIN_SUCCESS,
							userName);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		}.start();
	}

	private void getUserDoc(final String userName) {
		new Thread() {

			@Override
			public void run() {
				JSONObject res = CouchDB.getUserDoc(userName);
				UserDoc userDoc = null;
				if (res.has("error")) {// no document
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

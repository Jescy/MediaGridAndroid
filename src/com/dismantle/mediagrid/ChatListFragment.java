package com.dismantle.mediagrid;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;

import com.google.gson.Gson;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChatListFragment extends Fragment {

	private RTPullListView mPullListView = null;

	List<Map<String, Object>> mDatalist = null;
	Map<String, List<Map<String, Object>>> mDatalists = null;
	// private List<String> dataList;
	private SimpleAdapter mAdapter = null;
	private User mUser = null;

	private HashMap<String, Member> mUsers = new HashMap<String, Member>();
	private int mUpdateSeq = 0;

	private boolean mIsUserInitialized = false;
	private Vector<String> mMSGs = new Vector<String>();
	private Vector<JSONObject> mMsgQueue = new Vector<JSONObject>();

	private Button mBtnSend = null;
	private Button mBtnReceiver = null;
	private EditText mTextMsg = null;

	private String mStrReceiver = null;

	private Thread mChatThread = null;
	private Thread mIMThread = null;
	private Thread mUserThread = null;
	private UserDoc mUserDoc = null;

	public ChatListFragment() {

	}

	private void initUser() {
		mUser = new User();
		Activity activity = getActivity();
		Intent intent = activity.getIntent();
		Bundle bundle = intent.getExtras();

		mUserDoc = (UserDoc) bundle.getSerializable("userDoc");

		mUser.pubkey = GlobalUtil.genPublicKey();
		mUser.prikey = GlobalUtil.genPrivateKey();
		mUser.password = GlobalUtil.genRandomPassword();
		mUser.room = "General";
		mUser.username = mUserDoc._id;

		if (!mUserDoc.rooms.contains(mUser.room)) {
			mUserDoc.rooms.add(mUser.room);
		}
		if (mUserDoc.left.contains(mUser.room))
			mUserDoc.left.remove(mUserDoc.left.indexOf(mUser.room));
		mUserDoc.key = mUser.pubkey;

		saveUserDoc(mUserDoc);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		View rootView = inflater.inflate(R.layout.chat_main, container, false);

		initUser();

		mPullListView = (RTPullListView) rootView.findViewById(R.id.chat_list);

		mDatalist = new ArrayList<Map<String, Object>>();

		mAdapter = new SimpleAdapter(thisActivity, mDatalist,
				R.layout.chat_list_item, new String[] { "chat_from", "chat_to",
						"chat_time", "chat_msg" }, new int[] { R.id.chat_from,
						R.id.chat_to, R.id.chat_time, R.id.chat_msg });
		// setListAdapter(adapter);
		mPullListView.setAdapter(mAdapter);

		mPullListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (id <= -1)
					return;
			}

		});

		// choose member button
		mBtnReceiver = (Button) rootView.findViewById(R.id.btn_receiver);
		mBtnReceiver.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(thisActivity,
						ChooseMemberActivity.class);
				intent.putExtra("member", mUsers);
				startActivityForResult(intent, 1);
			}
		});
		mTextMsg = (EditText) rootView.findViewById(R.id.txt_msg);
		mBtnSend = (Button) rootView.findViewById(R.id.btn_send);
		mBtnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String plaintext = mTextMsg.getText().toString();
				if (mStrReceiver == null
						|| mStrReceiver.equals(GlobalUtil.every_one)) {
					queueMessage(plaintext, mUsers, mUser.username, mUser.room,
							false);
				} else {
					/*
					 * recipients[$scope.user.username] =
					 * $scope.users[$scope.user.username];
					 * recipients[$scope.chat.selected] =
					 * $scope.users[$scope.chat.selected];
					 * queueMsg($scope.chat.msg
					 * ,recipients,$scope.user.username);
					 */
					HashMap<String, Member> recipients = new HashMap<String, Member>();
					recipients.put(mUser.username, mUsers.get(mUser.username));
					recipients.put(mStrReceiver, mUsers.get(mStrReceiver));
					queueMessage(plaintext, recipients, mUser.username, null,
							false);
				}

				mTextMsg.setText("");
			}
		});
		mStrReceiver = GlobalUtil.every_one;
		mDatalists = new HashMap<String, List<Map<String, Object>>>();
		return rootView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;
		if (data == null)
			return;
		switch (requestCode) {
		case 1:
			Bundle bundle = data.getExtras();
			String name = bundle.getString("name");
			mBtnReceiver.setText("to: " + name);
			mStrReceiver = name;

			if (!mDatalists.containsKey(mStrReceiver)) {
				mDatalists.put(mStrReceiver, new Vector<Map<String, Object>>());
			}

			mDatalist.clear();
			mDatalist.addAll(mDatalists.get(mStrReceiver));
			mAdapter.notifyDataSetChanged();
			mPullListView.setSelection(mPullListView.getBottom());
			break;

		default:
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	// message handler
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			JSONObject jsonObject = null;
			switch (msg.what) {
			case GlobalUtil.MSG_SAVE_USER_DOC_SUCCESS:
				getSequenceNumber();
				break;
			case GlobalUtil.MSG_GET_DB_INFO_SUCCESS:
				mUpdateSeq = Integer.valueOf(msg.obj.toString());
				longPollingUser(0);
				longPollingChat(mUpdateSeq);
				longPollingIM(mUpdateSeq);
				break;
			case GlobalUtil.MSG_POLLING_USER:
				updateUsers((JSONObject) msg.obj);
				longPollingUser(mUpdateSeq);
				break;
			case GlobalUtil.MSG_POLLING_CHAT:
				jsonObject = (JSONObject) msg.obj;
				try {
					mUpdateSeq = jsonObject.getInt("last_seq");
				} catch (JSONException e) {
					e.printStackTrace(System.err);
					GlobalUtil.sleepFor(GlobalUtil.RECONNECT_INTERVAL);
				}
				getMessages(jsonObject);
				break;
			case GlobalUtil.MSG_POLLING_IM:
				jsonObject = (JSONObject) msg.obj;
				try {
					mUpdateSeq = jsonObject.getInt("last_seq");
					showInstantMessages(jsonObject);

				} catch (JSONException e) {
					e.printStackTrace(System.err);
					GlobalUtil.sleepFor(GlobalUtil.RECONNECT_INTERVAL);
				}
				longPollingIM(mUpdateSeq);

				break;
			case GlobalUtil.MSG_CHAT_LIST:
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> msgs = (List<Map<String, Object>>) msg.obj;
				if (!mDatalists.containsKey(GlobalUtil.every_one)) {
					mDatalists.put(GlobalUtil.every_one,
							new Vector<Map<String, Object>>());
				}
				mDatalists.get(GlobalUtil.every_one).addAll(msgs);
				if (mStrReceiver.equals(GlobalUtil.every_one)) {
					mDatalist.clear();
					mDatalist.addAll(mDatalists.get(mStrReceiver));
					mAdapter.notifyDataSetChanged();
					mPullListView.setSelection(mPullListView.getBottom());
				}
				longPollingChat(mUpdateSeq);

				break;
			default:
				break;
			}
		}

	};

	private void loadData(final int code, final boolean isMore) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				JSONArray jsonArray = null;
				try {
					jsonArray = CouchDB.getMsgList();

					ArrayList<Map<String, Object>> dList = new ArrayList<Map<String, Object>>();

					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonMSG = jsonArray.getJSONObject(i);
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("chat_from", jsonMSG.get("chat_from"));
						map.put("chat_to", jsonMSG.get("chat_to"));
						map.put("chat_time", jsonMSG.get("chat_time"));
						map.put("chat_msg", jsonMSG.get("chat_msg"));

						dList.add(map);
					}
					Message message = new Message();
					Bundle bundle = new Bundle();
					bundle.putSerializable("data", dList);
					message.setData(bundle);
					message.what = code;
					myHandler.sendMessage(message);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					myHandler.sendEmptyMessage(GlobalUtil.MSG_LOAD_FAILED);
				}

			}
		}).start();
	}

	private void saveUserDoc(final UserDoc userDoc) {
		new Thread() {

			@Override
			public void run() {
				try {
					JSONObject resJson = CouchDB.saveUserDoc(userDoc._id,
							userDoc._rev, userDoc.key, userDoc.type,
							userDoc.rooms, userDoc.left);
					if (!resJson.has("error")) {
						String rev = resJson.getString("rev");
						mUserDoc._rev = rev;
						myHandler
								.sendEmptyMessage(GlobalUtil.MSG_SAVE_USER_DOC_SUCCESS);
					} else
						myHandler
								.sendEmptyMessage(GlobalUtil.MSG_SAVE_USER_DOC_FAILED);
				} catch (JSONException e) {
					myHandler
							.sendEmptyMessage(GlobalUtil.MSG_SAVE_USER_DOC_FAILED);
					e.printStackTrace();
				}
			}

		}.start();
	}

	private void getSequenceNumber() {
		new Thread() {

			@Override
			public void run() {
				try {
					JSONObject resJson = CouchDB.getChatDBInfo();
					if (resJson.has("update_seq")) {
						Message msg = new Message();
						msg.what = GlobalUtil.MSG_GET_DB_INFO_SUCCESS;
						msg.obj = resJson.getString("update_seq");
						myHandler.sendMessage(msg);
					} else
						myHandler
								.sendEmptyMessage(GlobalUtil.MSG_GET_DB_INFO_FAILED);
				} catch (JSONException e) {

				}

			}

		}.start();
	}

	private void longPollingUser(final int seq) {
		mUserThread = new Thread() {

			@Override
			public void run() {

				JSONObject resJson = CouchDB.longPollingUser(seq, mUser.room);
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_USER;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		};
		mUserThread.start();
	}

	private void longPollingChat(final int seq) {
		mChatThread = new Thread() {

			@Override
			public void run() {
				JSONObject resJson = CouchDB.longPollingChat(seq, mUser.room);
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_CHAT;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		};
		mChatThread.start();
	}

	private void longPollingIM(final int seq) {
		mIMThread = new Thread() {
			@Override
			public void run() {
				JSONObject resJson = CouchDB.longPollingIM(seq);
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_IM;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		};
		mIMThread.start();
	}

	private void updateUsers(JSONObject jsonUsers) {
		try {
			JSONArray results = jsonUsers.getJSONArray("results");
			for (int i = 0; i < results.length(); i++) {
				JSONObject change = results.getJSONObject(i);
				JSONObject doc = change.getJSONObject("doc");
				String id = change.getString("id");
				UserDoc userDoc = new Gson().fromJson(doc.toString(),
						UserDoc.class);
				if (userDoc.left.contains(mUser.room)
						&& !userDoc.rooms.contains(mUser.room)) {
					if (mUsers.containsKey(id))
						mUsers.remove(id);
					if (mIsUserInitialized)
						mMSGs.add("*" + id + " has left.");

				} else if (!userDoc.left.contains(mUser.room)
						&& userDoc.rooms.contains(mUser.room)) {
					if (!mUsers.containsKey(id)) {
						Member member = new Member();
						member.key = doc.getString("key");
						member.seckey = GlobalUtil.genSecKey();
						member.name = id;
						mUsers.put(id, member);
						if (mIsUserInitialized || id == mUser.username)
							mMSGs.add("*" + id + " has arrived.");
					}
					mUsers.get(id).fingerprint = GlobalUtil.genFingerPrint();

				}
			}
			if (!mIsUserInitialized)
				mIsUserInitialized = true;
			mUpdateSeq = jsonUsers.getInt("last_seq");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		printMSG();
	}

	private void printMSG() {

		if (!mDatalists.containsKey(GlobalUtil.every_one))
			mDatalists.put(GlobalUtil.every_one,
					new Vector<Map<String, Object>>());
		for (String msg : mMSGs) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("chat_from", "message");
			map.put("chat_to", "every one");
			map.put("chat_time", new Date().toGMTString());
			map.put("chat_msg", msg);
			mDatalists.get(GlobalUtil.every_one).add(map);
		}
		if (mStrReceiver.equals(GlobalUtil.every_one)) {
			mDatalist.clear();
			mDatalist.addAll(mDatalists.get(GlobalUtil.every_one));
			mAdapter.notifyDataSetChanged();
			mPullListView.setSelection(mPullListView.getBottom());
		}
		mMSGs.clear();
	}

	private void showInstantMessages(JSONObject jsonObject)
			throws JSONException {
		JSONArray rows = jsonObject.getJSONArray("results");

		for (int i = 0; i < rows.length(); i++) {
			JSONObject row = rows.getJSONObject(i);
			JSONObject doc = row.getJSONObject("doc");
			JSONObject message = doc.getJSONObject("message");
			JSONObject to = message.getJSONObject(mUser.username);

			// deal with each message
			String chat_from = doc.getString("from");
			String chat_to = doc.getString("to");
			String chat_time = new java.util.Date(doc.getLong("created_at"))
					.toGMTString();
			String chat_msg = to.getString("msg");

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("chat_from", chat_from);
			map.put("chat_to", chat_to);
			map.put("chat_time", chat_time);
			map.put("chat_msg", chat_msg);

			String msgWith = chat_from;
			if (chat_from.equals(mUser.username))
				msgWith = chat_to;
			// save received message to mDatalists according to chat_from
			if (!mDatalists.containsKey(msgWith)) {
				mDatalists.put(msgWith, new Vector<Map<String, Object>>());
			}
			mDatalists.get(msgWith).add(map);
			// if chat_from is the current receiver, then show message
			if (msgWith.equals(mStrReceiver)) {
				mDatalist.clear();
				mDatalist.addAll(mDatalists.get(msgWith));
				mAdapter.notifyDataSetChanged();
				mPullListView.setSelection(mPullListView.getBottom());
			}
		}

	}

	private void getMessages(final JSONObject jsonMessages) {
		new Thread() {

			@Override
			public void run() {
				try {
					JSONArray results = jsonMessages.getJSONArray("results");
					String lastMsg = results
							.getJSONObject(results.length() - 1)
							.getString("id");
					String firstMsg = results.getJSONObject(0).getString("id");
					JSONObject resJson = CouchDB.getMsgs(mUser.room,
							mUser.username, firstMsg, lastMsg);
					JSONArray rows = resJson.getJSONArray("rows");

					ArrayList<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
					for (int i = 0; i < rows.length(); i++) {
						JSONObject jsonObject = rows.getJSONObject(i);
						JSONObject value = jsonObject.getJSONObject("value");
						JSONObject message = value.getJSONObject("message");
						JSONObject to = message.getJSONObject(mUser.username);

						// deal with each message
						String chat_from = value.getString("nick");
						String chat_to = mUser.username;
						String chat_time = new java.util.Date(
								value.getLong("created_at")).toGMTString();
						String chat_msg = to.getString("msg");

						Map<String, Object> map = new HashMap<String, Object>();
						map.put("chat_from", chat_from);
						map.put("chat_to", chat_to);
						map.put("chat_time", chat_time);
						map.put("chat_msg", chat_msg);
						msgs.add(map);
					}
					Message msg = new Message();
					msg.what = GlobalUtil.MSG_CHAT_LIST;
					msg.obj = msgs;
					myHandler.sendMessage(msg);
				} catch (JSONException e) {
					e.printStackTrace(System.err);
				}
			}

		}.start();

	}

	private void postQueue() {
		new Thread() {
			@Override
			public void run() {
				try {

					while (!mMsgQueue.isEmpty()) {
						CouchDB.postMsg(mMsgQueue.elementAt(0));
						mMsgQueue.remove(0);
					}

				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

		}.start();
	}

	public void queueMessage(String plaintext,
			HashMap<String, Member> recipients, String username, String room,
			boolean priority) {
		final JSONObject doc = new JSONObject();
		JSONObject msg = new JSONObject();
		String to = username;
		for (String name : recipients.keySet()) {
			if (room == null && name != username) {
				to = name;
			}
			/*
			 * var crypt = Crypto.AES.encrypt(plaintext, Crypto
			 * .util.hexToBytes(user.seckey.substring(0, 64)), { mode: new
			 * Crypto.mode.CBC(Crypto.pad.iso10126) });
			 */
			String crypt = plaintext;
			String hmac = "";// Crypto.HMAC(Whirlpool, crypt,
								// user.seckey.substring(64, 128))

			JSONObject userMsg = new JSONObject();
			try {
				userMsg.put("msg", crypt);
				userMsg.put("hmac", hmac);
				msg.put(name, userMsg);
			} catch (JSONException e) {
				e.printStackTrace(System.err);
			}

		}
		try {
			if (room == null) {
				doc.put("type", "IM");
				doc.put("from", username);
				doc.put("to", to);
				doc.put("message", msg);
			} else {
				doc.put("type", "MSG");
				doc.put("room", room);
				doc.put("nick", username);
				doc.put("message", msg);
			}
		} catch (JSONException e) {
			e.printStackTrace(System.err);
		}

		if (priority) {
			new Thread() {

				@Override
				public void run() {
					CouchDB.postMsg(doc);
				}

			}.start();

		} else {
			mMsgQueue.add(doc);
			postQueue();
		}
	}

	public void switchRoom() {
		final EditText txtRoom = new EditText(getActivity());
		new AlertDialog.Builder(getActivity())
				.setTitle("Switch room")
				.setView(txtRoom)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								String room = txtRoom.getText().toString()
										.trim();
								if (room.equals(""))
									return;
								mMsgQueue.clear();
								mMSGs.clear();
								mDatalist.clear();
								mChatThread.interrupt();
								mIMThread.interrupt();
								mUserThread.interrupt();

								mUser.room = room;
								if (!mUserDoc.rooms.contains(mUser.room)) {
									mUserDoc.rooms.add(mUser.room);
								}
								if (mUserDoc.left.contains(mUser.room))
									mUserDoc.left.remove(mUserDoc.left
											.indexOf(mUser.room));
								saveUserDoc(mUserDoc);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).show();

	}
}
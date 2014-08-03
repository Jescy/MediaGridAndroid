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

import android.annotation.SuppressLint;
import android.app.ActionBar;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.dismantle.mediagrid.RTPullListView.OnRefreshListener;
import com.google.gson.Gson;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChatFragment extends Fragment {

	private RTPullListView mPullListView = null;

	List<ChatItem> mChatItems = null;
	Map<String, List<ChatItem>> mChatItemsMap = null;
	// private List<String> dataList;
	private BaseAdapter mAdapter = null;
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

	private long mLastTime = 0;

	public ChatFragment() {

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

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		View rootView = inflater.inflate(R.layout.chat_main, container, false);

		initUser();

		mPullListView = (RTPullListView) rootView.findViewById(R.id.chat_list);

		mChatItems = new ArrayList<ChatItem>();

		mAdapter = new ChatMsgViewAdapter(thisActivity, mChatItems);

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
		mBtnSend.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnSend.setText(getResources().getString(R.string.fa_paper_plane));
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
		mChatItemsMap = new HashMap<String, List<ChatItem>>();
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

			if (!mChatItemsMap.containsKey(mStrReceiver)) {
				mChatItemsMap.put(mStrReceiver, new ArrayList<ChatItem>());
			}

			mChatItems.clear();
			mChatItems.add(new ChatItem());
			mChatItems.addAll(mChatItemsMap.get(mStrReceiver));
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
				// TODO incoming message
				List<ChatItem> msgs = (List<ChatItem>) msg.obj;
				if (!mChatItemsMap.containsKey(GlobalUtil.every_one)) {
					mChatItemsMap.put(GlobalUtil.every_one,
							new ArrayList<ChatItem>());
				}
				mChatItemsMap.get(GlobalUtil.every_one).addAll(msgs);
				if (mStrReceiver.equals(GlobalUtil.every_one)) {
					mChatItems.clear();
					mChatItems.add(new ChatItem());
					mChatItems.addAll(mChatItemsMap.get(mStrReceiver));
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

				JSONObject resJson = null;
				while (resJson == null)
					resJson = CouchDB.longPollingUser(seq, mUser.room);
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

		if (!mChatItemsMap.containsKey(GlobalUtil.every_one))
			mChatItemsMap.put(GlobalUtil.every_one, new ArrayList<ChatItem>());
		for (String msg : mMSGs) {
			// TODO print message
			ChatItem chatItem = new ChatItem();
			chatItem.itemMsg = msg;
			chatItem.itemType = ChatItem.ITEM_MSG_ALL;
			mChatItemsMap.get(GlobalUtil.every_one).add(chatItem);
		}
		if (mStrReceiver.equals(GlobalUtil.every_one)) {
			mChatItems.clear();
			mChatItems.add(new ChatItem());
			mChatItems.addAll(mChatItemsMap.get(GlobalUtil.every_one));
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
			long chat_time = doc.getLong("created_at");
			String chat_msg = to.getString("msg");

			// Map<String, Object> map = new HashMap<String, Object>();
			// map.put("chat_from", chat_from);
			// map.put("chat_to", chat_to);
			// map.put("chat_time", chat_time);
			// map.put("chat_msg", chat_msg);

			String msgWith = chat_from;
			if (chat_from.equals(mUser.username))
				msgWith = chat_to;
			// save received message to mDatalists according to chat_from
			if (!mChatItemsMap.containsKey(msgWith)) {
				mChatItemsMap.put(msgWith, new ArrayList<ChatItem>());
			}

			// if time different is big enough, then show the chat time.
			long time_diff = chat_time - mLastTime;
			if (time_diff >= GlobalUtil.CHAT_TIME_INTERVAL) {
				ChatItem item2 = new ChatItem();
				item2.itemType = ChatItem.ITEM_MSG_ALL;
				item2.itemMsg = GlobalUtil.getFormattedDate(new java.util.Date(
						chat_time));
				mChatItemsMap.get(msgWith).add(item2);
				mLastTime = chat_time;
			}

			// show IM message
			ChatItem item = new ChatItem();
			item.chatNick = chat_to;
			item.itemMsg = chat_msg;
			item.itemType = chat_from.equals(mUser.username) ? ChatItem.ITEM_MSG_ME
					: ChatItem.ITEM_MSG_USER;
			mChatItemsMap.get(msgWith).add(item);

			// if chat_from is the current receiver, then update message list
			if (msgWith.equals(mStrReceiver)) {
				mChatItems.clear();
				mChatItems.add(new ChatItem());
				mChatItems.addAll(mChatItemsMap.get(msgWith));
				mAdapter.notifyDataSetChanged();
				mPullListView.setSelection(mPullListView.getBottom());
			}
		}

	}

	/**
	 * get chat messages(i.e. public message) by messages' first ID and last ID
	 * 
	 * @param jsonMessages
	 */
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

					ArrayList<ChatItem> msgs = new ArrayList<ChatItem>();
					for (int i = 0; i < rows.length(); i++) {
						JSONObject jsonObject = rows.getJSONObject(i);
						JSONObject value = jsonObject.getJSONObject("value");
						JSONObject message = value.getJSONObject("message");
						JSONObject to = message.getJSONObject(mUser.username);

						// deal with each message
						String chat_nick = value.getString("nick");
						long chat_time = value.getLong("created_at");
						String chat_msg = to.getString("msg");
						boolean chat_from_me = chat_nick.equals(mUser.username);
						// Map<String, Object> map = new HashMap<String,
						// Object>();
						// map.put("chat_from", chat_from);
						// map.put("chat_to", chat_to);
						// map.put("chat_time", chat_time);
						// map.put("chat_msg", chat_msg);

						// if time different is big enough, then show the chat
						// time.
						long time_diff = chat_time - mLastTime;
						if (time_diff >= GlobalUtil.CHAT_TIME_INTERVAL) {
							ChatItem item2 = new ChatItem();
							item2.itemType = ChatItem.ITEM_MSG_ALL;
							item2.itemMsg = GlobalUtil
									.getFormattedDate(new java.util.Date(
											chat_time));
							msgs.add(item2);
							mLastTime = chat_time;
						}

						// show the chat message
						ChatItem item = new ChatItem();
						item.chatNick = chat_nick;
						item.itemMsg = chat_msg;
						item.itemType = chat_from_me ? ChatItem.ITEM_MSG_ME
								: ChatItem.ITEM_MSG_USER;
						msgs.add(item);
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
		txtRoom.setText(mUser.room);
		new AlertDialog.Builder(getActivity())
				.setTitle("Switch room")
				.setView(txtRoom)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								String room = txtRoom.getText().toString()
										.trim();
								if (room.equals(""))
									return;
								mMsgQueue.clear();
								mMSGs.clear();
								mChatItemsMap.clear();
								mChatItems.clear();
								mUsers.clear();
								mAdapter.notifyDataSetChanged();

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
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).show();

	}
}
package com.dismantle.mediagrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.google.gson.Gson;

/**
 * A placeholder fragment containing the chat user interface
 * 
 * @author Jescy
 */
public class ChatFragment extends Fragment {
	/**
	 * list view for chat items.
	 */
	private RTPullListView mPullListView = null;

	/**
	 * list of current shown chat items.
	 */
	List<ChatItem> mChatItems = null;
	/**
	 * map from user name to its corresponding list of chat items.
	 */
	Map<String, List<ChatItem>> mChatItemsMap = null;
	/**
	 * chat item list adapter.
	 */
	private BaseAdapter mAdapter = null;
	/**
	 * send button.
	 */
	private Button mBtnSend = null;
	/**
	 * Recipient button.
	 */
	private Button mBtnReceiver = null;
	/**
	 * message input box.
	 */
	private EditText mTextMsg = null;
	/**
	 * current user information.
	 */
	private User mUser = null;
	/**
	 * map from user name to its own information.
	 */
	private HashMap<String, Member> mUsers = new HashMap<String, Member>();
	/**
	 * chat database update sequence number.
	 */
	private int mUpdateSeq = 0;
	/**
	 * if user list is returned for the first time.
	 */
	private boolean mIsUserInitialized = false;
	/**
	 * time, left or arrival messages.
	 */
	private Vector<String> mMSGs = new Vector<String>();
	/**
	 * Queued messages to send in sequence.
	 */
	private Vector<JSONObject> mMsgQueue = new Vector<JSONObject>();
	/**
	 * current recipient.
	 */
	private String mStrReceiver = null;
	/**
	 * chat long polling thread.
	 */
	private Thread mChatThread = null;
	/**
	 * instant message long polling thread.
	 */
	private Thread mIMThread = null;
	/**
	 * user list long polling thread.
	 */
	private Thread mUserThread = null;
	/**
	 * current user's document.
	 */
	private UserDoc mUserDoc = null;

	/**
	 * when the time message is shown last time.
	 */
	private long mLastTime = 0;

	public ChatFragment() {

	}

	/**
	 * initialize user's information from intent, and save its new document to
	 * server.
	 */
	private void initUser() {
		mUser = new User();
		// get user document in intent.
		Activity activity = getActivity();
		Intent intent = activity.getIntent();
		Bundle bundle = intent.getExtras();
		mUserDoc = (UserDoc) bundle.getSerializable("userDoc");

		// generate public, private key and password for user.
		mUser.pubkey = GlobalUtil.genPublicKey();
		mUser.prikey = GlobalUtil.genPrivateKey();
		mUser.password = GlobalUtil.genRandomPassword();
		mUser.room = "General";
		mUser.username = mUserDoc._id;

		// add or remove room from user's document.
		if (!mUserDoc.rooms.contains(mUser.room)) {
			mUserDoc.rooms.add(mUser.room);
		}
		if (mUserDoc.left.contains(mUser.room))
			mUserDoc.left.remove(mUserDoc.left.indexOf(mUser.room));
		mUserDoc.key = mUser.pubkey;
		// save user document to server.
		saveUserDoc(mUserDoc);

	}

	/**
	 * get the name the prefix of an icon.
	 * 
	 * @param name
	 *            the user name
	 * @return the prefix user name.
	 */
	private String getNameAwesome(String name) {
		if (name.equals(GlobalUtil.every_one)) {
			return getString(R.string.fa_users) + name;
		} else if (name.equals(mUser.username)) {
			return getString(R.string.fa_heart) + name;
		} else
			return getString(R.string.fa_user) + name;
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		/**
		 * Inflate with main layout.
		 */
		View rootView = inflater.inflate(R.layout.chat_main, container, false);
		/**
		 * initialize user information.
		 */
		initUser();

		mPullListView = (RTPullListView) rootView.findViewById(R.id.chat_list);

		mChatItems = new ArrayList<ChatItem>();

		mAdapter = new ChatMsgViewAdapter(thisActivity, mChatItems);

		mPullListView.setAdapter(mAdapter);

		// choose recipient button initialize
		mBtnReceiver = (Button) rootView.findViewById(R.id.btn_receiver);
		mBtnReceiver.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnReceiver.setText(getNameAwesome(GlobalUtil.every_one));
		mBtnReceiver.setOnClickListener(new OnClickListener() {
			/**
			 * show a list of all users in this room.
			 */
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(thisActivity,
						ChooseRecipientActivity.class);
				intent.putExtra("member", mUsers);
				intent.putExtra("user_name", mUser.username);
				startActivityForResult(intent, 1);
			}
		});
		// message input box.
		mTextMsg = (EditText) rootView.findViewById(R.id.txt_msg);

		// send button initialize
		mBtnSend = (Button) rootView.findViewById(R.id.btn_send);
		mBtnSend.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnSend.setText(getResources().getString(R.string.fa_paper_plane));

		// click to queue message
		mBtnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String plaintext = mTextMsg.getText().toString();

				if (mStrReceiver == null
						|| mStrReceiver.equals(GlobalUtil.every_one)) {
					// if public room message.
					queueMessage(plaintext, mUsers, mUser.username, mUser.room,
							false);
				} else {
					// if private instant message.
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

		// initialize recipient to every one.
		mStrReceiver = GlobalUtil.every_one;
		// initialize chat message list of all members.
		mChatItemsMap = new HashMap<String, List<ChatItem>>();

		return rootView;
	}

	/**
	 * deal with the result recipient that user select.
	 */
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
			// change text on recipient to the chosen name.
			mBtnReceiver.setText(getNameAwesome(name));
			mStrReceiver = name;
			// if no chat list map for this name, create one.
			if (!mChatItemsMap.containsKey(mStrReceiver)) {
				mChatItemsMap.put(mStrReceiver, new ArrayList<ChatItem>());
			}
			// set current chat list to the selected name's chat list, and show
			// it with notifyDataSetChanged().
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

	/**
	 * message handler to deal with network access result.
	 */
	@SuppressLint("HandlerLeak")
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			JSONObject jsonObject = null;
			switch (msg.what) {
			case GlobalUtil.MSG_SAVE_USER_DOC_SUCCESS:
				// if save user document successfully.
				// get update sequence number of database.
				getSequenceNumber();
				break;
			case GlobalUtil.MSG_GET_DB_INFO_SUCCESS:
				// if get database sequence number successfully.
				// set sequence number
				mUpdateSeq = Integer.valueOf(msg.obj.toString());
				// long polling for user list.
				longPollingUser(0);
				// long polling for chat items.
				longPollingChat(mUpdateSeq);
				// long polling for instant messages.
				longPollingIM(mUpdateSeq);
				break;
			case GlobalUtil.MSG_POLLING_USER:
				// if long polling for user returns
				// update user list
				updateUsers((JSONObject) msg.obj);
				// long polling for users.
				longPollingUser(mUpdateSeq);
				break;
			case GlobalUtil.MSG_POLLING_CHAT:
				// if long polling for chat items returns
				jsonObject = (JSONObject) msg.obj;
				try {
					// get update sequence
					mUpdateSeq = jsonObject.getInt("last_seq");
				} catch (JSONException e) {
					e.printStackTrace(System.err);
					GlobalUtil.sleepFor(GlobalUtil.RECONNECT_INTERVAL);
				}
				// get message list from server.
				getMessages(jsonObject);
				break;
			case GlobalUtil.MSG_POLLING_IM:
				// if long polling for instant messsage returns.
				jsonObject = (JSONObject) msg.obj;
				try {
					// get update sequence
					mUpdateSeq = jsonObject.getInt("last_seq");
					// show instant message to chat list.
					showInstantMessages(jsonObject);

				} catch (JSONException e) {
					e.printStackTrace(System.err);
					GlobalUtil.sleepFor(GlobalUtil.RECONNECT_INTERVAL);
				}
				// long polling for instant message.
				longPollingIM(mUpdateSeq);

				break;
			case GlobalUtil.MSG_CHAT_LIST:
				// if chat message list returns.
				@SuppressWarnings("unchecked")
				List<ChatItem> msgs = (List<ChatItem>) msg.obj;
				// if no chat items map for this user, create one.
				if (!mChatItemsMap.containsKey(GlobalUtil.every_one)) {
					mChatItemsMap.put(GlobalUtil.every_one,
							new ArrayList<ChatItem>());
				}
				// add message to map.
				mChatItemsMap.get(GlobalUtil.every_one).addAll(msgs);
				// show updated message if required.
				if (mStrReceiver.equals(GlobalUtil.every_one)) {
					mChatItems.clear();
					mChatItems.add(new ChatItem());
					mChatItems.addAll(mChatItemsMap.get(mStrReceiver));
					mAdapter.notifyDataSetChanged();
					mPullListView.setSelection(mPullListView.getBottom());
				}
				// long polling for chat items.
				longPollingChat(mUpdateSeq);

				break;
			default:
				break;
			}
		}

	};

	/**
	 * save user document to server.
	 * 
	 * @param userDoc
	 *            user document to save.
	 */
	private void saveUserDoc(final UserDoc userDoc) {
		new Thread() {

			@Override
			public void run() {
				try {
					// call CouchDB to save document
					JSONObject resJson = CouchDB.saveUserDoc(userDoc._id,
							userDoc._rev, userDoc.key, userDoc.type,
							userDoc.rooms, userDoc.left);
					// send result message.
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

	/**
	 * get update sequence number of chat database.
	 */
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

	/**
	 * long polling for user list. If user list changed, returns the newly
	 * updated user list.
	 * 
	 * @param seq
	 *            current update sequence number.
	 */
	private void longPollingUser(final int seq) {
		mUserThread = new Thread() {

			@Override
			public void run() {

				JSONObject resJson = null;
				// if null returns, then polling again.
				while (resJson == null)
					resJson = CouchDB.longPollingUser(seq, mUser.room);
				// send result message.
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_USER;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		};
		mUserThread.start();
	}

	/**
	 * long polling for chat items. If chat database changed, returns the
	 * message document's first and last ID.
	 * 
	 * @param seq
	 *            current update sequence number.
	 */
	private void longPollingChat(final int seq) {
		mChatThread = new Thread() {

			@Override
			public void run() {
				// polling for chat database, by room name.
				JSONObject resJson = CouchDB.longPollingChat(seq, mUser.room);
				// send result message.
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_CHAT;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		};
		mChatThread.start();
	}

	/**
	 * long polling for instant messages. If any, returns the new instant
	 * message.
	 * 
	 * @param seq
	 */
	private void longPollingIM(final int seq) {
		mIMThread = new Thread() {
			@Override
			public void run() {
				// polling for chat database's instant message.
				JSONObject resJson = CouchDB.longPollingIM(seq);
				// send result message.
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_IM;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		};
		mIMThread.start();
	}

	/**
	 * update users information by returned user document list from
	 * {@link #longPollingUser(int) longPollingUser} method.
	 * 
	 * @param jsonUsers
	 *            returned user document list from {@link #longPollingUser(int)
	 *            longPollingUser} method.
	 */
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
			// set first time user list tag.
			if (!mIsUserInitialized)
				mIsUserInitialized = true;
			// update sequence number.
			mUpdateSeq = jsonUsers.getInt("last_seq");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		printMSG();
	}

	/**
	 * put messages in {@link #mMSGs} into {@link #mChatItemsMap}, then show the
	 * chat list if required.
	 */
	private void printMSG() {

		if (!mChatItemsMap.containsKey(GlobalUtil.every_one))
			mChatItemsMap.put(GlobalUtil.every_one, new ArrayList<ChatItem>());
		for (String msg : mMSGs) {
			ChatItem chatItem = new ChatItem();
			chatItem.itemMsg = msg;
			chatItem.itemType = ChatItem.ITEM_MSG_ALL;
			mChatItemsMap.get(GlobalUtil.every_one).add(chatItem);
		}
		// show the chat list if required.
		if (mStrReceiver.equals(GlobalUtil.every_one)) {
			mChatItems.clear();
			mChatItems.add(new ChatItem());
			mChatItems.addAll(mChatItemsMap.get(GlobalUtil.every_one));
			mAdapter.notifyDataSetChanged();
			mPullListView.setSelection(mPullListView.getBottom());
		}
		mMSGs.clear();
	}

	/**
	 * show instant message returned from {@link #longPollingIM(int)}
	 * 
	 * @param jsonObject
	 *            returned messages from {@link #longPollingIM(int)}
	 * @throws JSONException
	 */
	private void showInstantMessages(JSONObject jsonObject)
			throws JSONException {
		/*
		 * jsonObject is like this {"results":[
		 * {"seq":473,"id":"e443518eb4e1a6f830b8cfa52706f4ed","changes":[{"rev":
		 * "1-cf24837265eab4abb72cd16b67f9cd91"
		 * }],"doc":{"_id":"e443518eb4e1a6f830b8cfa52706f4ed"
		 * ,"_rev":"1-cf24837265eab4abb72cd16b67f9cd91"
		 * ,"type":"IM","from":"Susan"
		 * ,"to":"Susan","message":{"Susan":{"msg":"dfsd"
		 * ,"hmac":""}},"created_at":1407750111408}} ], "last_seq":473}
		 */
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
					/**
					 * the format of json message like this:
					 * {"total_rows":574,"offset":526,"rows":[
					 * {"id":"e443518eb4e1a6f830b8cfa52707574e"
					 * ,"key":["Susan","General"
					 * ,"e443518eb4e1a6f830b8cfa52707574e"
					 * ],"value":{"room":"General"
					 * ,"created_at":1407750227343,"nick"
					 * :"Susan","message":{"Susan"
					 * :{"msg":"this message","hmac":""}}}} ]}
					 */
					JSONArray results = jsonMessages.getJSONArray("results");
					// get first and last message's ID.
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
					// send result message to handler.
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

	/**
	 * post queued messages one by one, to avoid blocking.
	 */
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

	/**
	 * queue a specified message to {@link #mMsgQueue } to send one by one.
	 * 
	 * @param plaintext
	 *            text message.
	 * @param recipients
	 *            recipients of this message.
	 * @param username
	 *            user that sends the message.
	 * @param room
	 *            room name
	 * @param priority
	 *            is prior
	 */
	public void queueMessage(String plaintext,
			HashMap<String, Member> recipients, String username, String room,
			boolean priority) {
		final JSONObject doc = new JSONObject();
		JSONObject msg = new JSONObject();
		// to whom
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
			// no encryption and no hmac
			String crypt = plaintext;
			String hmac = "";// Crypto.HMAC(Whirlpool, crypt,
								// user.seckey.substring(64, 128))
			// compose message.
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
			// compose document message
			if (room == null) {
				// if instant message.
				doc.put("type", "IM");
				doc.put("from", username);
				doc.put("to", to);
				doc.put("message", msg);
			} else {
				// if public room message.
				doc.put("type", "MSG");
				doc.put("room", room);
				doc.put("nick", username);
				doc.put("message", msg);
			}
		} catch (JSONException e) {
			e.printStackTrace(System.err);
		}

		if (priority) {
			// if priority, send directly.
			new Thread() {

				@Override
				public void run() {
					CouchDB.postMsg(doc);
				}

			}.start();

		} else {
			// if not priority, queue it.
			mMsgQueue.add(doc);
			postQueue();
		}
	}

	/**
	 * dialog when switch room button clicked.
	 */
	public void switchRoom() {

		final EditText txtRoom = new EditText(getActivity());
		txtRoom.setText(mUser.room);
		// show dialog to get room's new name.
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
								// clear message queue, chat items map, current
								// chat items.
								mMsgQueue.clear();
								mMSGs.clear();
								mChatItemsMap.clear();
								mChatItems.clear();
								mUsers.clear();
								mAdapter.notifyDataSetChanged();

								// close long polling threads.
								mChatThread.interrupt();
								mIMThread.interrupt();
								mUserThread.interrupt();

								// save new user document, and restart the whole
								// initialize process.
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
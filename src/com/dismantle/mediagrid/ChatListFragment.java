package com.dismantle.mediagrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Global;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.SimpleAdapter;

import com.dismantle.mediagrid.RTPullListView.OnRefreshListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChatListFragment extends Fragment {

	private RTPullListView mPullListView = null;

	List<Map<String, Object>> mDatalist = null;
	// private List<String> dataList;
	private SimpleAdapter mAdapter = null;
	private User mUser = null;

	private HashMap<String, Member> mUsers = new HashMap<String, Member>();
	private int mUpdateSeq = 0;

	private boolean mIsUserInitialized = false;
	private Vector<String> mMSGs = new Vector<String>();

	public ChatListFragment() {

	}

	private void initUser() {
		mUser = new User();
		Activity activity = getActivity();
		Intent intent = activity.getIntent();
		Bundle bundle = intent.getExtras();
		try {
			JSONObject userJsonObject = new JSONObject(
					bundle.getString("userDoc"));
			Gson gson = new Gson();
			final UserDoc userDoc = gson.fromJson(userJsonObject.toString(),
					UserDoc.class);
			mUser.pubkey = GlobalUtil.genPublicKey();
			mUser.prikey = GlobalUtil.genPrivateKey();
			mUser.password = GlobalUtil.genRandomPassword();
			mUser.room = "General";
			mUser.username = userDoc._id;

			if (!userDoc.rooms.contains(mUser.room)) {
				userDoc.rooms.add(mUser.room);
			}
			if (userDoc.left.contains(mUser.room))
				userDoc.left.remove(userDoc.left.indexOf(mUser.room));
			userDoc.key = mUser.pubkey;

			saveUserDoc(userDoc);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		View rootView = inflater.inflate(R.layout.chat_list, container, false);

		initUser();

		mPullListView = (RTPullListView) rootView.findViewById(R.id.chat_list);

		mDatalist = new ArrayList<Map<String, Object>>();

		mAdapter = new SimpleAdapter(thisActivity, mDatalist,
				R.layout.chat_list_item, new String[] { "chat_from", "chat_to",
						"chat_time", "chat_msg" }, new int[] { R.id.chat_from,
						R.id.chat_to, R.id.chat_time, R.id.chat_msg });
		// setListAdapter(adapter);
		mPullListView.setAdapter(mAdapter);

		loadData(GlobalUtil.MSG_LOAD_SUCCESS, false);

		mPullListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (id <= -1)
					return;
			}

		});
		// 下拉刷新监听器
		mPullListView.setonRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {

				loadData(GlobalUtil.MSG_LOAD_SUCCESS, false);

			}
		});

		// choose member button
		Button chooseButton = (Button) rootView
				.findViewById(R.id.btn_choose_from);
		chooseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(thisActivity,
						ChooseMemberActivity.class);
				startActivity(intent);
			}
		});
		return rootView;
	}

	// 结果处理
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			ArrayList<Map<String, Object>> data = (ArrayList<Map<String, Object>>) msg
					.getData().getSerializable("data");
			if (data != null && data.size() != 0) {
				if (msg.what == GlobalUtil.MSG_LOAD_SUCCESS)
					mDatalist.clear();

				mDatalist.addAll(data);
			}
			switch (msg.what) {
			case GlobalUtil.MSG_LOAD_SUCCESS:
				mAdapter.notifyDataSetChanged();
				mPullListView.onRefreshComplete();
				mPullListView.setSelectionAfterHeaderView();

				break;
			case GlobalUtil.MSG_SAVE_USER_DOC_SUCCESS:
				getSequenceNumber();
				break;
			case GlobalUtil.MSG_GET_DB_INFO_SUCCESS:
				mUpdateSeq = Integer.valueOf(msg.obj.toString());
				longPollingUser(0);
				longPollingChat(mUpdateSeq);
				break;
			case GlobalUtil.MSG_POLLING_USER:
				updateUsers((JSONObject) msg.obj);
				longPollingUser(mUpdateSeq);

				break;
			case GlobalUtil.MSG_POLLING_CHAT:
				getMessages((JSONObject) msg.obj);
				int c=0;
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
					if (!resJson.has("error"))
						myHandler
								.sendEmptyMessage(GlobalUtil.MSG_SAVE_USER_DOC_SUCCESS);
					else
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
		new Thread() {

			@Override
			public void run() {

				JSONObject resJson = CouchDB.longPollingUser(seq, mUser.room);
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_USER;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		}.start();
	}

	private void longPollingChat(final int seq) {
		new Thread() {

			@Override
			public void run() {

				JSONObject resJson = CouchDB.longPollingChat(seq, mUser.room);
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_CHAT;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		}.start();
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
						mMSGs.add("*" + id + "has left.");

				} else if (!userDoc.left.contains(mUser.room)
						&& userDoc.rooms.contains(mUser.room)) {
					if (!mUsers.containsKey(id)) {
						Member member = new Member();
						member.key = doc.getString("key");
						member.seckey = GlobalUtil.genSecKey();
						member.name = id;
						mUsers.put(id, member);
						if (mIsUserInitialized || id == mUser.username)
							mMSGs.add("*" + id + "has arrived.");
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
					JSONArray rows=resJson.getJSONArray("rows");
					for(int i=0;i<rows.length();i++)
					{
						JSONObject msg=rows.getJSONObject(i);
						//deal with each message
					}
				} catch (JSONException e) {

				}
			}

		}.start();

	}
}
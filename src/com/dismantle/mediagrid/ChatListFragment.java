package com.dismantle.mediagrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
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

	public ChatListFragment() {

		
	}

	private void initUser() {
		mUser = new User();
		Activity activity=getActivity();
		Intent intent=activity.getIntent();
		Bundle bundle = intent.getExtras();
		try {
			JSONObject userJsonObject = new JSONObject(
					bundle.getString("userDoc"));
			Gson gson = new Gson();
			UserDoc userDoc = gson.fromJson(userJsonObject.toString(),
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
			JSONObject resJson=CouchDB.saveUserDoc(userDoc._id, userDoc._rev, userDoc.key,
					userDoc.type, userDoc.rooms, userDoc.left);

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
}
package com.dismantle.mediagrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;

public class ChooseMemberActivity extends Activity {
	private RTPullListView mPullListView;

	List<Map<String, Object>> mDatalist = null;
	// private List<String> dataList;
	private SimpleAdapter mAdapter;

	HashMap<String, Member> mUsers = null;

	private String mUserName = null;

	public ChooseMemberActivity() {

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.member_main);

		Intent intent = getIntent();
		mUsers = (HashMap<String, Member>) (intent.getExtras().get("member"));
		mUserName = (String) (intent.getExtras().getString("user_name"));

		mPullListView = (RTPullListView) this.findViewById(R.id.member_list);

		mDatalist = new ArrayList<Map<String, Object>>();

		// adapter = new ArrayAdapter<String>(this,
		// android.R.layout.simple_list_item_1, dataList);
		mAdapter = new ChooseMemberSimpleAdapter(this, mDatalist,
				R.layout.member_list_item, new String[] { "user_photo",
						"member_name" }, new int[] { R.id.user_photo,
						R.id.member_name });
		// setListAdapter(adapter);
		mPullListView.setAdapter(mAdapter);

		loadData(GlobalUtil.MSG_LOAD_SUCCESS);

		mPullListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (id <= -1)
					return;
				Intent intent = new Intent();
				intent.putExtra("name",
						mDatalist.get(position - 1).get("member_name")
								.toString());
				ChooseMemberActivity.this.setResult(Activity.RESULT_OK, intent);
				ChooseMemberActivity.this.finish();
			}

		});
		setTitle("Message Recipient:");

	}

	private void loadData(final int code) {

		ArrayList<Map<String, Object>> dList = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();
		dList.add(map);
		map.put("member_name", "Everyone");
		map.put("user_photo", getString(R.string.fa_users));
		for (String key : mUsers.keySet()) {
			Member member = mUsers.get(key);
			map = new HashMap<String, Object>();
			if (member.name.equals(mUserName)) {
				map.put("user_photo", getString(R.string.fa_heart));
			}else
				map.put("user_photo", getString(R.string.fa_user));
			map.put("member_name", member.name);
			dList.add(map);
		}
		mDatalist.clear();
		mDatalist.addAll(dList);
	}
}

class ChooseMemberSimpleAdapter extends SimpleAdapter {

	private Context mContext = null;

	public ChooseMemberSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
		mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View res = super.getView(position, convertView, parent);
		TextView textView = (TextView) res.findViewById(R.id.user_photo);
		textView.setTypeface(GlobalUtil.getFontAwesome(mContext));
		return res;
	}
}

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

/**
 * List view activity shown when choose recipient button clicked.
 * @author Jescy
 *
 */
public class ChooseRecipientActivity extends Activity {
	/**
	 * member list view.
	 */
	private RTPullListView mPullListView;
	/**
	 * member list data.
	 */
	List<Map<String, Object>> mDatalist = null;
	/**
	 * member list adapter.
	 */
	private SimpleAdapter mAdapter;
	/**
	 * member's document, passed from {@link ChatFragment}
	 */
	HashMap<String, Member> mUsers = null;
	/**
	 * current user name.
	 */
	private String mUserName = null;

	public ChooseRecipientActivity() {

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.member_main);

		//initialize users documents and current user name from intent.
		Intent intent = getIntent();
		mUsers = (HashMap<String, Member>) (intent.getExtras().get("member"));
		mUserName = (String) (intent.getExtras().getString("user_name"));

		//initialize list view.
		mPullListView = (RTPullListView) this.findViewById(R.id.member_list);

		mDatalist = new ArrayList<Map<String, Object>>();

		mAdapter = new ChooseMemberSimpleAdapter(this, mDatalist,
				R.layout.member_list_item, new String[] { "user_photo",
						"member_name" }, new int[] { R.id.user_photo,
						R.id.member_name });
		mPullListView.setAdapter(mAdapter);
		
		//load user data.
		loadData();

		/**
		 * if one recipient is clicked, then send back the recipient name to ChatFragment.
		 */
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
				ChooseRecipientActivity.this.setResult(Activity.RESULT_OK, intent);
				ChooseRecipientActivity.this.finish();
			}

		});
		setTitle("Message Recipient:");

	}
	/**
	 * load member list data from {@link #mUsers}}
	 */
	private void loadData() {

		ArrayList<Map<String, Object>> dList = new ArrayList<Map<String, Object>>();
		
		//first load "everyone" item.
		Map<String, Object> map = new HashMap<String, Object>();
		dList.add(map);
		map.put("member_name", "Everyone");
		map.put("user_photo", getString(R.string.fa_users));
		// then load users, including me or others.
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
/**
 * simple adapter, used to set font of user_photo to FontAwesome.
 * @author Jescy
 *
 */
class ChooseMemberSimpleAdapter extends SimpleAdapter {

	private Context mContext = null;

	public ChooseMemberSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
		mContext = context;
	}
	/**
	 * set font of user_photo to FontAwesome.
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		View res = super.getView(position, convertView, parent);
		//set font of user_photo to FontAwesome.
		TextView textView = (TextView) res.findViewById(R.id.user_photo);
		textView.setTypeface(GlobalUtil.getFontAwesome(mContext));
		return res;
	}
}

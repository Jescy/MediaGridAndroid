package com.dismantle.mediagrid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Chat message view adapter, to control chat item list view
 * 
 * @author Jescy
 * 
 */
public class ChatMsgViewAdapter extends BaseAdapter {

	/**
	 * chat list items.
	 */
	List<ChatItem> mChatItems = null;

	private LayoutInflater mInflater;

	/**
	 * @param context
	 *            context activity.
	 * @param datalist
	 *            chat list
	 */
	public ChatMsgViewAdapter(Context context, List<ChatItem> datalist) {
		this.mChatItems = datalist;
		mInflater = LayoutInflater.from(context);
	}

	/**
	 * get count of items.
	 */
	public int getCount() {
		return mChatItems.size();
	}

	/**
	 * get one item.
	 */
	public Object getItem(int position) {
		return mChatItems.get(position);
	}

	/**
	 * get item's id.
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * get item's type defined by {@link ChatItem}, there are three:
	 * ITEM_MSG_ALL, ITEM_MSG_USER, ITEM_MSG_ALL
	 */
	public int getItemViewType(int position) {
		ChatItem entity = mChatItems.get(position);

		return entity.itemType;

	}

	/**
	 * count of view's type.
	 */
	public int getViewTypeCount() {
		return 3;
	}

	/**
	 * return view of an item.
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		// convert view is intended for reusing views.

		// get current position's item.
		ChatItem entity = mChatItems.get(position);

		ViewHolder viewHolder = null;
		int itemType = getItemViewType(position);
		if (convertView == null) {
			viewHolder = new ViewHolder();
			switch (itemType) {
			case ChatItem.ITEM_MSG_ALL:
				// if message is time, left, or arrival message.
				convertView = mInflater.inflate(R.layout.chat_item_all, null);
				viewHolder.tvContent = (TextView) convertView
						.findViewById(R.id.all_msg);
				break;
			case ChatItem.ITEM_MSG_USER:
				// if message is chat message send by others.
				convertView = mInflater.inflate(R.layout.chat_item_left, null);
				viewHolder.tvUserName = (TextView) convertView
						.findViewById(R.id.chat_from);
				viewHolder.tvContent = (TextView) convertView
						.findViewById(R.id.chat_msg);
				break;
			case ChatItem.ITEM_MSG_ME:
				// if message is chat message send by me.
				convertView = mInflater.inflate(R.layout.chat_item_right, null);
				viewHolder.tvUserName = (TextView) convertView
						.findViewById(R.id.chat_from);
				viewHolder.tvContent = (TextView) convertView
						.findViewById(R.id.chat_msg);

				break;
			default:
				break;
			}
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (itemType) {
		case ChatItem.ITEM_MSG_ALL:
			// if message is time, left, or arrival message.
			viewHolder.tvContent.setText(entity.itemMsg);
			break;
		case ChatItem.ITEM_MSG_USER:
			// if message is chat message send by others.
			viewHolder.tvUserName.setText(entity.chatNick);
			viewHolder.tvContent.setText(entity.itemMsg);
			break;
		case ChatItem.ITEM_MSG_ME:
			// if message is chat message send by me.
			viewHolder.tvUserName.setText(entity.chatNick);
			viewHolder.tvContent.setText(entity.itemMsg);
			break;
		default:
			break;
		}
		return convertView;
	}

	/**
	 * ViewHolder to hold views, containing user name and message content.
	 * 
	 * @author Jescy
	 * 
	 */
	static class ViewHolder {
		public TextView tvUserName = null;
		public TextView tvContent = null;
	}

}

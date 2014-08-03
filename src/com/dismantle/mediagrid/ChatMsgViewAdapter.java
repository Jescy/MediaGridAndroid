package com.dismantle.mediagrid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ChatMsgViewAdapter extends BaseAdapter {

	private static final String TAG = ChatMsgViewAdapter.class.getSimpleName();

	List<ChatItem> mChatItems = null;

	private LayoutInflater mInflater;

	public ChatMsgViewAdapter(Context context, List<ChatItem> datalist) {
		this.mChatItems = datalist;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return mChatItems.size();
	}

	public Object getItem(int position) {
		return mChatItems.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public int getItemViewType(int position) {
		ChatItem entity = mChatItems.get(position);

		return entity.itemType;

	}

	public int getViewTypeCount() {
		return 3;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		ChatItem entity = mChatItems.get(position);

		ViewHolder viewHolder = null;
		int itemType = getItemViewType(position);
		if (convertView == null) {
			// if (entity.getClass().equals(ChatItem.class)) {
			// ChatItem chatItem = (ChatItem)entity;
			// boolean isMeMsg = chatItem.isChatFromMe();
			// if (isMeMsg) {
			// convertView = mInflater.inflate(R.layout.chat_item_right,
			// null);
			// } else {
			// convertView = mInflater.inflate(R.layout.chat_item_left,
			// null);
			// }
			//
			// viewHolder = new ViewHolder();
			// // viewHolder.tvSendTime = (TextView)
			// // convertView.findViewById(R.id.tv_sendtime);
			// viewHolder.tvUserName = (TextView) convertView
			// .findViewById(R.id.chat_from);
			// viewHolder.tvContent = (TextView) convertView
			// .findViewById(R.id.chat_msg);
			// viewHolder.isMeMsg = isMeMsg;
			//
			// convertView.setTag(viewHolder);
			// }
			viewHolder = new ViewHolder();
			switch (itemType) {
			case ChatItem.ITEM_MSG_ALL:
				convertView = mInflater.inflate(R.layout.chat_item_all, null);
				viewHolder.tvContent = (TextView) convertView
						.findViewById(R.id.all_msg);
				break;
			case ChatItem.ITEM_MSG_USER:
				convertView = mInflater.inflate(R.layout.chat_item_left, null);
				viewHolder.tvUserName = (TextView) convertView
						.findViewById(R.id.chat_from);
				viewHolder.tvContent = (TextView) convertView
						.findViewById(R.id.chat_msg);
				break;
			case ChatItem.ITEM_MSG_ME:
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
			viewHolder.tvContent.setText(entity.itemMsg);
			break;
		case ChatItem.ITEM_MSG_USER:
			viewHolder.tvUserName.setText(entity.chatNick);
			viewHolder.tvContent.setText(entity.itemMsg);
			break;
		case ChatItem.ITEM_MSG_ME:
			viewHolder.tvUserName.setText(entity.chatNick);
			viewHolder.tvContent.setText(entity.itemMsg);
			break;
		default:
			break;
		}
		return convertView;
	}

	static class ViewHolder {
		// public TextView tvSendTime;
		public TextView tvUserName = null;
		public TextView tvContent = null;
		// public TextView tvMessage = null;
		// public boolean isMeMsg = false;
	}

}

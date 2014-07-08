package com.dismantle.mediagrid;

import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;

public class CouchDBHelper {
	private Handler handler=null;
	public CouchDBHelper(Handler h)
	{
		handler=h;
	}
	public void getSession() {
		new Thread() {

			@Override
			public void run() {
				JSONObject obj=CouchDB.getSession();
				sendMessage(MessageType.GET_SESSION, obj);
			}
			
			
		}.start();

	}
	public void sendMessage(MessageType what,Object obj)
	{
		Message message=new Message();
		message.what=what.ordinal();
		message.obj=obj;
		handler.sendMessage(message);

	}
}

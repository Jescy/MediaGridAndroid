package com.dismantle.mediagrid;

import java.util.Date;

import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class LoginActivity extends ActionBarActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);
		Button btnOK = (Button)findViewById(R.id.btn_login);
		btnOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(LoginActivity.this, MainActivity.class);
				startActivity(intent);
				new Thread() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						//String resString=HttpService.doGet("/_session");
						JSONObject res=null;
						try {
							
							Date date=new Date();
							//login test
							res = CouchDB.login("admin", "jtgpgf");
							assert(!res.has("error"));
							//get session test
							res = CouchDB.getSession();
							assert(!res.has("error"));
//							//get file list test
//							res = CouchDB.getFiles(true, true, null);
//							assert(!res.has("error"));
//							//create dir test
//							res = CouchDB.createDir("mytest_"+date.getMinutes()+"_"+date.getSeconds(), null, "DIR", date.toGMTString());
//							assert(!res.has("error"));
//							//register test
//							res = CouchDB.register("test_user_"+date.getMinutes()+"_"+date.getSeconds(), "kkkkkkkk", "user");
//							assert(!res.has("error"));
//							//get user document
//							res = CouchDB.getUserDoc("guoliang");
//							
//							
//							
//							Message msg = new Message();
//							msg.obj=res;
//							msg.what =0;
//							handler.sendMessage(msg);
							//res = CouchDB.createDir("mytest_"+date.getMinutes()+"_"+date.getSeconds(), null, "DIR", date.toGMTString());
						} catch (Exception e) {
							e.printStackTrace();
						}
						

					}
				}.start();
				
				
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	private Handler handler= new Handler()
	{
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				Toast.makeText(getApplicationContext(), "ddd:"+msg.obj.toString(), Toast.LENGTH_LONG).show();
				break;

			default:
				break;
			}
		};
	};
}

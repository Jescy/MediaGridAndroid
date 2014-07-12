package com.dismantle.mediagrid;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dismantle.mediagrid.RTPullListView.OnRefreshListener;

/**
 * A placeholder fragment containing a simple view.
 */
public class MediaListFragment extends Fragment {

	private RTPullListView pullListView = null;

	private TextView txtPath = null;
	List<Map<String, Object>> datalist = null;
	private SimpleAdapter adapter = null;

	private Vector<String> currentKey = new Vector<String>();

	private Button btnMakeDir = null;
	private Button btnUpload = null;

	public MediaListFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		View rootView = inflater.inflate(R.layout.media_list, container, false);
		pullListView = (RTPullListView) rootView.findViewById(R.id.file_list);
		txtPath = (TextView) rootView.findViewById(R.id.txt_path);
		btnMakeDir = (Button) rootView.findViewById(R.id.btn_mkdir);
		btnUpload = (Button) rootView.findViewById(R.id.btn_upload);

		datalist = new ArrayList<Map<String, Object>>();

		adapter = new SimpleAdapter(thisActivity, datalist,
				R.layout.media_list_item, new String[] { "file_ico","file_name",
						"file_size", "upload_time" }, new int[] {
						R.id.file_ico, R.id.file_name, R.id.file_size,
						R.id.upload_time });
		pullListView.setAdapter(adapter);

		currentKey.clear();
		loadFiles();

		pullListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (id <= -1)
					return;
				int realID = (int) id;
				if (id == 0 && !isHome())// travel back to parent
				{
					currentKey.remove(currentKey.size() - 1);
					loadFiles();
					return;
				}
				Map<String, Object> item = datalist.get(realID);
				String type = item.get("type").toString();
				String fileurl = item.get("file_url").toString();
				if (type.equals("DIR")) {
					currentKey.add(fileurl);
					loadFiles();
				} else if (type.equals("FILE")) {
					Toast.makeText(thisActivity, fileurl, Toast.LENGTH_SHORT)
							.show();
				}
			}

		});
		// pulltorefresh listener
		pullListView.setonRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {

				// loadData(GlobalUtil.LOAD_SUCCESS,false);

			}
		});

		btnMakeDir.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				final EditText inputServer = new EditText(thisActivity);
				AlertDialog.Builder builder = new AlertDialog.Builder(
						thisActivity);
				builder.setTitle("Input directory name")
						.setIcon(android.R.drawable.ic_dialog_info)
						.setView(inputServer).setNegativeButton("Cancel", null);
				builder.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								final String name = inputServer.getText()
										.toString();
								if (isLegalName(name)) {
									new Thread() {
										@Override
										public void run() {
											try {
												JSONObject resJson = CouchDB
														.createDir(
																name,
																keysToPath(),
																new Date()
																		.toLocaleString());
												myHandler
														.sendEmptyMessage(GlobalUtil.MSG_CREATE_DIR_SUCCESS);
											} catch (JSONException e) {
												e.printStackTrace(System.err);
											}
										}

									}.start();

								} else {

								}
							}
						});
				builder.show();
			}
		});
		return rootView;
	}

	private boolean isLegalName(String inputString) {
		Pattern pattern = Pattern.compile("^\\w+$");
		Matcher macher = pattern.matcher(inputString);
		boolean ret = macher.find();

		return ret;
	}

	private String keysToPath() {
		if (isHome())
			return null;
		String path = "";
		for (String str : currentKey) {
			path += str + "/";
		}
		path = path.substring(0, path.length() - 1);
		return path;
	}

	private void updateNavigation() {
		String path = "Home";

		for (String key : currentKey) {
			path += "/" + key;
		}
		txtPath.setText(path);
	}

	private boolean isHome() {
		return currentKey.size() == 0;
	}

	// message handler
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			ArrayList<Map<String, Object>> data = (ArrayList<Map<String, Object>>) msg
					.getData().getSerializable("data");

			switch (msg.what) {
			case GlobalUtil.MSG_LOAD_SUCCESS:
				datalist.clear();
				datalist.addAll(data);
				adapter.notifyDataSetChanged();
				pullListView.onRefreshComplete();
				pullListView.setSelectionAfterHeaderView();
				break;
			case GlobalUtil.MSG_CREATE_DIR_SUCCESS:
				loadFiles();
				break;
			default:
				break;
			}
		}

	};

	private void loadFiles() {
		final String key = keysToPath();
		new Thread(new Runnable() {

			@Override
			public void run() {
				JSONObject jsonObject = null;
				try {
					jsonObject = CouchDB.getFiles(true, true, key);

					JSONArray jsonArray = jsonObject.getJSONArray("rows");
					ArrayList<Map<String, Object>> dList = new ArrayList<Map<String, Object>>();

					Map<String, Object> map = null;
					if (!isHome()) {
						// add .. for travel to parent
						map = new HashMap<String, Object>();
						map.put("id", "");
						map.put("file_name", "..");
						map.put("type", "DIR");
						map.put("file_ico", R.drawable.folder_ico);
						map.put("upload_time", "");
						map.put("file_url", "");
						map.put("file_size", "---------");
						dList.add(map);
					}
					// add file list
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonFile = jsonArray.getJSONObject(i);
						JSONObject jsonValue = jsonFile.getJSONObject("value");
						String id = jsonFile.getString("id");

						map = new HashMap<String, Object>();
						map.put("id", id);
						map.put("file_name", jsonValue.getString("filename"));
						String fileSize = jsonValue.getString("size");
						map.put("type", jsonValue.getString("type"));
						map.put("upload_time", jsonValue.getString("time"));
						map.put("file_url", jsonValue.getString("fileurl"));
						if (fileSize.equals("-")){
							map.put("file_size", "---------");
							map.put("file_ico", R.drawable.folder_ico);
						}
						else{
							map.put("file_size", fileSize + "B");
							map.put("file_ico", R.drawable.file_ico);
						}
						dList.add(map);
					}
					Message message = new Message();
					Bundle bundle = new Bundle();
					bundle.putSerializable("data", dList);
					message.setData(bundle);
					message.what = GlobalUtil.MSG_LOAD_SUCCESS;
					myHandler.sendMessage(message);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					myHandler.sendEmptyMessage(GlobalUtil.MSG_LOAD_FAILED);
				}

			}
		}).start();
		updateNavigation();
	}
}
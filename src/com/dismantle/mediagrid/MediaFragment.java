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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
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
public class MediaFragment extends Fragment {

	private RTPullListView mPullListView = null;

	private TextView mTxtPath = null;
	List<Map<String, Object>> mDatalist = null;
	private SimpleAdapter mAdapter = null;

	private Vector<String> mCurrentKeys = new Vector<String>();

	private Button mBtnMakeDir = null;
	private Button mBtnUpload = null;

	Dialog mUploadFileDialog = null;
	Dialog mDownFileDialog = null;

	public MediaFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		View rootView = inflater.inflate(R.layout.media_main, container, false);
		mPullListView = (RTPullListView) rootView.findViewById(R.id.file_list);
		mTxtPath = (TextView) rootView.findViewById(R.id.txt_path);
		mBtnMakeDir = (Button) rootView.findViewById(R.id.btn_mkdir);
		mBtnMakeDir.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnMakeDir.setText(getResources().getString(R.string.fa_level_down)+mBtnMakeDir.getText());
		
		mBtnUpload = (Button) rootView.findViewById(R.id.btn_upload);
		mBtnUpload.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnUpload.setText(getResources().getString(R.string.fa_cloud_upload)+mBtnUpload.getText());
		mDatalist = new ArrayList<Map<String, Object>>();
		
		

		mAdapter = new MediaSimpleAdapter(thisActivity, mDatalist,
				R.layout.media_list_item, new String[] { "file_ico",
						"file_name", "file_size", "upload_time" }, new int[] {
						R.id.file_ico, R.id.file_name, R.id.file_size,
						R.id.upload_time });
		mPullListView.setAdapter(mAdapter);

		mCurrentKeys.clear();
		loadFiles();

		mPullListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (id <= -1)
					return;
				int realID = (int) id;
				if (id == 0 && !isHome())// travel back to parent
				{
					mCurrentKeys.remove(mCurrentKeys.size() - 1);
					loadFiles();
					return;
				}
				Map<String, Object> item = mDatalist.get(realID);
				String type = item.get("type").toString();
				final String fileurl = item.get("file_url").toString();
				final String filename=item.get("file_name").toString();
				if (type.equals("DIR")) {
					mCurrentKeys.add(fileurl);
					loadFiles();
				} else if (type.equals("FILE")) {
					
					mDownFileDialog = OpenFileDialog.createDialog(getActivity(),
							"choose a directory", new CallbackBundle() {

								@Override
								public void callback(Bundle bundle) {
									mDownFileDialog.dismiss();
									final String path = bundle.getString("path");

									new Thread() {

										@Override
										public void run() {
											try {
												boolean resDownload=false;
												resDownload = CouchDB.doDownloadFile("/media/"+fileurl, path+"/"+filename);
												if (resDownload)
													myHandler
															.sendEmptyMessage(GlobalUtil.MSG_DOWNLOAD_SUCCESS);
												else
													myHandler
															.sendEmptyMessage(GlobalUtil.MSG_DOWNLOAD_FAILED);
											} catch (Exception e) {
												myHandler
														.sendEmptyMessage(GlobalUtil.MSG_DOWNLOAD_FAILED);
												e.printStackTrace(System.err);
											}
										}

									}.start();

								}
							}, null, true);
					mDownFileDialog.show();
					WindowManager.LayoutParams layoutParams = mDownFileDialog
							.getWindow().getAttributes();
					layoutParams.height = LayoutParams.MATCH_PARENT;
					mDownFileDialog.getWindow().setAttributes(layoutParams);
					
					//HttpService.getInstance().doDownloadFile(fileurl, path)
				}
			}

		});
		// pulltorefresh listener
		mPullListView.setonRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {

				// loadData(GlobalUtil.LOAD_SUCCESS,false);

			}
		});

		mBtnUpload.setOnClickListener(new UploadOnclickListener());
		mBtnMakeDir.setOnClickListener(new MakeDirOnclickListener());
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
		for (String str : mCurrentKeys) {
			path += str + "/";
		}
		path = path.substring(0, path.length() - 1);
		return path;
	}

	private void updateNavigation() {
		String path = "Home";

		for (String key : mCurrentKeys) {
			path += "/" + key;
		}
		mTxtPath.setText(path);
	}

	private boolean isHome() {
		return mCurrentKeys.size() == 0;
	}

	// message handler
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			@SuppressWarnings("unchecked")
			ArrayList<Map<String, Object>> dirs = (ArrayList<Map<String, Object>>) msg
					.getData().getSerializable("dirs");
			@SuppressWarnings("unchecked")
			ArrayList<Map<String, Object>> files = (ArrayList<Map<String, Object>>) msg
					.getData().getSerializable("files");
			switch (msg.what) {
			case GlobalUtil.MSG_LOAD_SUCCESS:
				mDatalist.clear();
				mDatalist.addAll(dirs);
				mDatalist.addAll(files);
				mAdapter.notifyDataSetChanged();
				mPullListView.onRefreshComplete();
				mPullListView.setSelectionAfterHeaderView();
				break;
			case GlobalUtil.MSG_CREATE_DIR_SUCCESS:
				loadFiles();
				if (isAdded())
					Toast.makeText(getActivity(),
							getResources().getString(R.string.makedir_success),
							Toast.LENGTH_SHORT).show();
				break;
			case GlobalUtil.MSG_UPLOAD_SUCCESS:
				loadFiles();
				if (isAdded())
					Toast.makeText(getActivity(),
							getResources().getString(R.string.upload_success),
							Toast.LENGTH_SHORT).show();
				break;
			case GlobalUtil.MSG_DOWNLOAD_SUCCESS:
				if (isAdded())
					Toast.makeText(getActivity(),
							getResources().getString(R.string.download_success),
							Toast.LENGTH_SHORT).show();
				break;
			case GlobalUtil.MSG_DOWNLOAD_FAILED:
				if (isAdded())
					Toast.makeText(getActivity(),
							getResources().getString(R.string.download_failed),
							Toast.LENGTH_SHORT).show();
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
					ArrayList<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
					ArrayList<Map<String, Object>> dirs = new ArrayList<Map<String, Object>>();
					Map<String, Object> map = null;
					if (!isHome()) {
						// add .. for travel to parent
						map = new HashMap<String, Object>();
						map.put("id", "");
						map.put("file_name", "..");
						map.put("type", "DIR");
						map.put("file_ico", getString(R.string.fa_level_up));
						map.put("upload_time", "");
						map.put("file_url", "");
						map.put("file_size", "---------");
						dirs.add(map);
					}
					// add file list
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonFile = jsonArray.getJSONObject(i);
						JSONObject jsonValue = jsonFile.getJSONObject("value");
						String id = jsonFile.getString("id");

						map = new HashMap<String, Object>();
						map.put("id", id);
						String fileName = jsonValue.getString("filename");
						map.put("file_name", fileName);
						String fileSize = jsonValue.getString("size");
						map.put("type", jsonValue.getString("type"));
						map.put("upload_time", jsonValue.getString("time"));
						map.put("file_url", jsonValue.getString("fileurl"));
						if (fileSize.equals("-")) {
							map.put("file_size", "---------");
							map.put("file_ico", getString(R.string.fa_folder));
							dirs.add(map);
						} else {
							map.put("file_size", fileSize + "B");
							String posix = fileName.substring(fileName.lastIndexOf("."));
							int iconID = FileTypeIcon.getIcon(posix);
							map.put("file_ico", getString(iconID));
							files.add(map);
						}
						
					}
					Message message = new Message();
					Bundle bundle = new Bundle();
					bundle.putSerializable("files", files);
					bundle.putSerializable("dirs", dirs);
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

	private class MakeDirOnclickListener implements View.OnClickListener {
		@Override
		public void onClick(View arg0) {
			final EditText inputServer = new EditText(getActivity());
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("Input directory name")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setView(inputServer).setNegativeButton("Cancel", null);
			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
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
											if (!resJson.has("error"))
												myHandler
														.sendEmptyMessage(GlobalUtil.MSG_CREATE_DIR_SUCCESS);
											else
												myHandler
														.sendEmptyMessage(GlobalUtil.MSG_CREATE_DIR_FAILED);
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

	}

	private class UploadOnclickListener implements View.OnClickListener {

		@Override
		public void onClick(View arg0) {
			

			mUploadFileDialog = OpenFileDialog.createDialog(getActivity(),
					"choose a file", new CallbackBundle() {

						@Override
						public void callback(Bundle bundle) {
							mUploadFileDialog.dismiss();
							final String path = bundle.getString("path");

							new Thread() {

								@Override
								public void run() {
									try {
										JSONObject resJson = CouchDB
												.createFileDocument(keysToPath());
										String id = resJson.getString("id");
										String rev = resJson.getString("rev");

										resJson = CouchDB.upload(id, rev, path);
										if (!resJson.has("error"))
											myHandler
													.sendEmptyMessage(GlobalUtil.MSG_UPLOAD_SUCCESS);
										else
											myHandler
													.sendEmptyMessage(GlobalUtil.MSG_UPLOAD_FAILED);
									} catch (Exception e) {
										myHandler
												.sendEmptyMessage(GlobalUtil.MSG_UPLOAD_FAILED);
										e.printStackTrace(System.err);
									}
								}

							}.start();

						}
					}, null, false);
			mUploadFileDialog.show();
			WindowManager.LayoutParams layoutParams = mUploadFileDialog
					.getWindow().getAttributes();
			layoutParams.height = LayoutParams.MATCH_PARENT;
			mUploadFileDialog.getWindow().setAttributes(layoutParams);
		}
	}
}
class MediaSimpleAdapter extends SimpleAdapter {

	private Context mContext = null;
	public MediaSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
		mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View res=super.getView(position, convertView, parent);
		TextView textView=(TextView)res.findViewById(R.id.file_ico);
		textView.setTypeface(GlobalUtil.getFontAwesome(mContext));
		textView.setTextColor(Color.parseColor("#66CD00"));
		return res;
	}
}
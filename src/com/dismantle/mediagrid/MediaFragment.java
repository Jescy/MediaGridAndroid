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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view. This view is to show a list
 * of files
 */
@SuppressLint("HandlerLeak")
public class MediaFragment extends Fragment {
	/**
	 * list view of files
	 */
	private RTPullListView mPullListView = null;
	/**
	 * text view to show current path
	 */
	private TextView mTxtPath = null;
	/**
	 * list view's data for files
	 */
	List<Map<String, Object>> mDatalist = null;
	/**
	 * list view adapter
	 */
	private SimpleAdapter mAdapter = null;
	/**
	 * current paths
	 */
	private Vector<String> mCurrentKeys = new Vector<String>();
	/**
	 * make directory button
	 */
	private Button mBtnMakeDir = null;
	/**
	 * upload button
	 */
	private Button mBtnUpload = null;
	/**
	 * local file explorer for uploading file
	 */
	Dialog mUploadFileDialog = null;
	/**
	 * local file explorer for downloading file
	 */
	Dialog mDownFileDialog = null;
	/**
	 * progress dialog
	 */
	private ProgressDialog mProgressDialog = null;
	/**
	 * update sequence number
	 */
	private int mUpdateSeq = 0;

	public MediaFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final FragmentActivity thisActivity = getActivity();
		View rootView = inflater.inflate(R.layout.media_main, container, false);

		mTxtPath = (TextView) rootView.findViewById(R.id.txt_path);
		// initialize make directory button
		mBtnMakeDir = (Button) rootView.findViewById(R.id.btn_mkdir);
		mBtnMakeDir.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnMakeDir.setText(getResources().getString(R.string.fa_level_down)
				+ mBtnMakeDir.getText());
		mBtnMakeDir.setOnClickListener(new MakeDirOnclickListener());

		// initialize upload button
		mBtnUpload = (Button) rootView.findViewById(R.id.btn_upload);
		mBtnUpload.setTypeface(GlobalUtil.getFontAwesome(thisActivity));
		mBtnUpload.setText(getResources().getString(R.string.fa_cloud_upload)
				+ mBtnUpload.getText());
		mBtnUpload.setOnClickListener(new UploadOnclickListener());

		mDatalist = new ArrayList<Map<String, Object>>();
		// create adapter for file list
		mAdapter = new MediaSimpleAdapter(thisActivity, mDatalist,
				R.layout.media_list_item, new String[] { "file_ico",
						"file_name", "file_size", "upload_time" }, new int[] {
						R.id.file_ico, R.id.file_name, R.id.file_size,
						R.id.upload_time });
		// initialize pull list view
		mPullListView = (RTPullListView) rootView.findViewById(R.id.file_list);
		mPullListView.setAdapter(mAdapter);
		mCurrentKeys.clear();
		// load file list
		loadFiles();
		// set onclick listener
		mPullListView.setOnItemClickListener(new MediaItemClickListener());
		return rootView;
	}

	/**
	 * test if directory name is illegal
	 * 
	 * @param inputString
	 * @return true if legal
	 */
	private boolean isLegalName(String inputString) {
		Pattern pattern = Pattern.compile("^\\w+$");
		Matcher macher = pattern.matcher(inputString);
		boolean ret = macher.find();

		return ret;
	}

	/**
	 * convert from keys to path
	 * 
	 * @return
	 */
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

	/**
	 * update the current path text view
	 */
	private void updateNavigation() {
		String path = "Home";

		for (String key : mCurrentKeys) {
			path += "/" + key;
		}
		mTxtPath.setText(path);
	}

	/**
	 * if current path is home path
	 * 
	 * @return
	 */
	private boolean isHome() {
		return mCurrentKeys.size() == 0;
	}

	/**
	 * message handler to handle network access result messages
	 */
	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			@SuppressWarnings("unchecked")
			// get directory list
			ArrayList<Map<String, Object>> dirs = (ArrayList<Map<String, Object>>) msg
					.getData().getSerializable("dirs");
			@SuppressWarnings("unchecked")
			// get file list
			ArrayList<Map<String, Object>> files = (ArrayList<Map<String, Object>>) msg
					.getData().getSerializable("files");
			if (mProgressDialog != null && mProgressDialog.isShowing())
				mProgressDialog.dismiss();
			switch (msg.what) {
			case GlobalUtil.MSG_LOAD_SUCCESS:
				// if load file success, then add dirs and files to list, then
				// refresh
				mDatalist.clear();
				mDatalist.addAll(dirs);
				mDatalist.addAll(files);
				mAdapter.notifyDataSetChanged();
				mPullListView.onRefreshComplete();
				mPullListView.setSelectionAfterHeaderView();

				longPollingFile(mUpdateSeq);
				break;
			case GlobalUtil.MSG_POLLING_FILE:
				// if polling file returns, then load file again to refresh
				// files
				loadFiles();
				break;
			case GlobalUtil.MSG_CREATE_DIR_SUCCESS:
				// if create directory success, then load file again to refresh
				// files, then show a toast promotion.
				loadFiles();
				if (isAdded())
					Toast.makeText(getActivity(),
							getResources().getString(R.string.makedir_success),
							Toast.LENGTH_SHORT).show();
				break;
			case GlobalUtil.MSG_UPLOAD_SUCCESS:
				// if upload file success, then load file again to refresh
				// files, then show a toast promotion.
				loadFiles();
				if (isAdded())
					Toast.makeText(getActivity(),
							getResources().getString(R.string.upload_success),
							Toast.LENGTH_SHORT).show();

				break;
			case GlobalUtil.MSG_DOWNLOAD_SUCCESS:
				// if download file success, then show a toast promotion.
				if (isAdded())
					Toast.makeText(
							getActivity(),
							getResources().getString(R.string.download_success),
							Toast.LENGTH_SHORT).show();
				break;
			case GlobalUtil.MSG_DOWNLOAD_FAILED:
				// if download file fails, then show a toast promotion.
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

	/**
	 * load file list from server
	 */
	private void loadFiles() {
		// get current path
		final String key = keysToPath();
		new Thread(new Runnable() {

			@Override
			public void run() {
				JSONObject jsonObject = null;
				try {
					// call CouchDB's load file
					jsonObject = CouchDB.getFiles(true, true, key);
					// if fails, send result message
					if (jsonObject == null || jsonObject.has("error")) {
						myHandler.sendEmptyMessage(GlobalUtil.MSG_LOAD_FAILED);
						return;
					}
					// get update sequence number
					mUpdateSeq = jsonObject.getInt("update_seq");
					// get file list and directory list
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
						map.put("icon_color", "#628EAE");
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
							map.put("icon_color", "#628EAE");
							dirs.add(map);
						} else {
							map.put("file_size", fileSize + "B");
							String posix = fileName.substring(fileName
									.lastIndexOf("."));
							int iconID = FileTypeIcon.getIcon(posix);
							map.put("file_ico", getString(iconID));
							map.put("icon_color", FileTypeIcon.getColor(posix));
							files.add(map);
						}

					}
					// send result message
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
		// update current path text view
		updateNavigation();
	}

	/**
	 * media item on click listener. Items can be "travel to parent",
	 * "travel into directory" or "download file"
	 * 
	 * @author Jescy
	 * 
	 */
	private class MediaItemClickListener implements
			AdapterView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			if (id <= -1)
				return;
			int realID = (int) id;
			if (id == 0 && !isHome()) {
				// if back to parent, then travel back to parent
				mCurrentKeys.remove(mCurrentKeys.size() - 1);
				loadFiles();
				return;
			}

			Map<String, Object> item = mDatalist.get(realID);
			String type = item.get("type").toString();
			final String fileurl = item.get("file_url").toString();
			final String filename = item.get("file_name").toString();
			if (type.equals("DIR")) {
				// if click a directory, then travel into directory
				mCurrentKeys.add(fileurl);
				loadFiles();
			} else if (type.equals("FILE")) {
				// if click a file, then show a local file explorer
				mDownFileDialog = LocalFileDialog.createDialog(getActivity(),
						"choose a directory", new CallbackBundle() {

							@Override
							public void callback(Bundle bundle) {
								// set the path to the dialog's title
								if (bundle.containsKey("title")) {
									String path = bundle.getString("path");
									if (mDownFileDialog != null && path != null)
										mDownFileDialog.setTitle(path);
									return;
								}
								mDownFileDialog.dismiss();
								// show progress dialog
								mProgressDialog = ProgressDialog.show(
										MediaFragment.this.getActivity(),
										"Downloading...",
										"Please wait for download");
								final String path = bundle.getString("path");
								// create a download file thread
								new Thread() {

									@Override
									public void run() {
										try {
											boolean resDownload = false;

											resDownload = CouchDB
													.doDownloadFile("/media/"
															+ fileurl, path
															+ "/" + filename);
											// send result message
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
				// set the title style
				TextView tvTiltle = (TextView) mDownFileDialog
						.findViewById(MediaFragment.this.getActivity()
								.getResources()
								.getIdentifier("alertTitle", "id", "android"));
				tvTiltle.setEllipsize(TruncateAt.START);
				WindowManager.LayoutParams layoutParams = mDownFileDialog
						.getWindow().getAttributes();
				layoutParams.height = LayoutParams.MATCH_PARENT;
				mDownFileDialog.getWindow().setAttributes(layoutParams);

				// HttpService.getInstance().doDownloadFile(fileurl, path)
			}
		}

	}

	/**
	 * make directory button listener.
	 * 
	 * @author Jescy
	 * 
	 */
	private class MakeDirOnclickListener implements View.OnClickListener {
		@Override
		public void onClick(View arg0) {
			final EditText inputServer = new EditText(getActivity());
			// create a dialog for inputting directory name
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("Input directory name")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setView(inputServer).setNegativeButton("Cancel", null);
			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							final String name = inputServer.getText()
									.toString();
							// check if name is legal
							if (isLegalName(name)) {
								// start a create directory thread
								new Thread() {
									@SuppressWarnings("deprecation")
									@Override
									public void run() {
										try {
											// create directory
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

	/**
	 * upload file button listener
	 * @author Jescy
	 *
	 */
	private class UploadOnclickListener implements View.OnClickListener {

		@Override
		public void onClick(View arg0) {
			//local file explorer for choosing a file
			mUploadFileDialog = LocalFileDialog.createDialog(getActivity(),
					"choose a file", new CallbackBundle() {

						@Override
						public void callback(Bundle bundle) {
							//set dialog title to be the filepath
							if (bundle.containsKey("title")) {
								String path = bundle.getString("path");
								if (mUploadFileDialog != null && path != null)
									mUploadFileDialog.setTitle(path);
								return;
							}
							mProgressDialog = ProgressDialog.show(
									MediaFragment.this.getActivity(),
									"Uploading...", "Please wait for uploading");
				
							mUploadFileDialog.dismiss();
							final String path = bundle.getString("path");
							//start a create file document thread
							new Thread() {

								@Override
								public void run() {
									try {
										//create a file document, then get id an rev of the document
										JSONObject resJson = CouchDB
												.createFileDocument(keysToPath());
										String id = resJson.getString("id");
										String rev = resJson.getString("rev");
										//upload file
										resJson = CouchDB.upload(id, rev, path);
										//send result message
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
			//set upload file style
			TextView tvTiltle = (TextView) mUploadFileDialog
					.findViewById(MediaFragment.this.getActivity()
							.getResources()
							.getIdentifier("alertTitle", "id", "android"));
			tvTiltle.setEllipsize(TruncateAt.START);
			WindowManager.LayoutParams layoutParams = mUploadFileDialog
					.getWindow().getAttributes();
			layoutParams.height = LayoutParams.MATCH_PARENT;
			mUploadFileDialog.getWindow().setAttributes(layoutParams);
		}
	}
	/**
	 * long polling file function
	 * @param seq
	 */
	private void longPollingFile(final int seq) {
		new Thread() {

			@Override
			public void run() {
				//if media database changes, then polling returns
				JSONObject resJson = CouchDB.longPollingFile(seq);
				Message msg = new Message();
				msg.what = GlobalUtil.MSG_POLLING_FILE;
				msg.obj = resJson;
				myHandler.sendMessage(msg);
			}

		}.start();
	}
}
/**
 * Media list view adapter, to set the list item's icon and color.
 * @author Jescy
 *
 */
class MediaSimpleAdapter extends SimpleAdapter {

	private Context mContext = null;
	private List<? extends Map<String, Object>> mDatas = null;

	public MediaSimpleAdapter(Context context,
			List<? extends Map<String, Object>> data, int resource,
			String[] from, int[] to) {
		super(context, data, resource, from, to);
		mDatas = data;
		mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		//set the list item's icon and color.
		Map<String, Object> map = mDatas.get(position);
		View res = super.getView(position, convertView, parent);
		TextView textView = (TextView) res.findViewById(R.id.file_ico);
		textView.setTypeface(GlobalUtil.getFontAwesome(mContext));
		Object color = map.get("icon_color");
		textView.setTextColor(Color.parseColor(color.toString()));
		return res;
	}
}
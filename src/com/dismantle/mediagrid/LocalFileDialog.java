// filename: OpenFileDialog.java
package com.dismantle.mediagrid;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class LocalFileDialog {
	public static String tag = "OpenFileDialog";
	static final public String mPathRoot = Environment
			.getExternalStorageDirectory().getPath();
	static final public String mPathParent = "..";
	static final public String mPathFolder = ".";
	static final public String mPathEmpty = "";
	static final public String mPathDefault = mPathRoot + "/MediaGrid";
	static final private String mMsgError = "No rights to access!";

	/**
	 * 
	 * @param context
	 *            context
	 * @param title
	 *            title for dialog
	 * @param callback
	 *            callback when something is selected
	 * @param suffix
	 *            only shows files with specified suffix
	 * @param selDirectory
	 *            Selection for directory or file.
	 * @param images
	 *            image icons
	 * @return
	 */
	public static Dialog createDialog(Context context, String title,
			final CallbackBundle callback, String suffix, boolean selDirectory) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final FileSelectView fileSelectView = new FileSelectView(context,
				callback, suffix, selDirectory);

		if (selDirectory) {
			RelativeLayout relativeLayout = new RelativeLayout(context);
			Button btnOK = new Button(context);
			btnOK.setText("Select this directory");
			btnOK.setBackgroundResource(R.drawable.button_basic_bg);
			btnOK.setId(12345678);
			btnOK.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Bundle bundle = new Bundle();
					bundle.putString("path", fileSelectView.mPath);

					callback.callback(bundle);
				}
			});
			RelativeLayout.LayoutParams btnLayoutParams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			btnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
					RelativeLayout.TRUE);
			relativeLayout.addView(btnOK, btnLayoutParams);
			RelativeLayout.LayoutParams lstLayoutParams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);

			lstLayoutParams.addRule(RelativeLayout.ABOVE, btnOK.getId());
			relativeLayout.addView(fileSelectView, lstLayoutParams);
			builder.setView(relativeLayout);
		} else {

			builder.setView(fileSelectView);
		}

		Dialog dialog = builder.create();
		dialog.setTitle(title);
		return dialog;
	}

	static class FileSelectView extends ListView implements OnItemClickListener {

		private CallbackBundle mCallback = null;
		private String mPath = mPathDefault;
		private List<Map<String, Object>> mList = null;

		private String mSuffix = null;

		private Context mContext;
		private boolean mSelDirecotry;

		public FileSelectView(Context context, CallbackBundle callback,
				String suffix, boolean selDirecotry) {
			super(context);
			this.mSuffix = suffix == null ? "" : suffix.toLowerCase();
			this.mCallback = callback;
			this.mContext = context;
			this.mSelDirecotry = selDirecotry;
			this.setOnItemClickListener(this);

			File file = new File(mPath);
			if (!file.exists())
				file.mkdirs();
			refreshFileList();
		}

		private String getSuffix(String filename) {
			int dix = filename.lastIndexOf('.');
			if (dix < 0) {
				return "";
			} else {
				return filename.substring(dix + 1);
			}
		}

		private int refreshFileList() {
			// refresh file list
			File[] files = null;
			try {
				files = new File(mPath).listFiles();
				Arrays.sort(files, new Comparator<File>() {

					@Override
					public int compare(File arg0, File arg1) {
						String name0 = arg0.getName();
						String name1 = arg1.getName();
						return name0.compareTo(name1);
					}

				});
			} catch (Exception e) {
				files = null;
			}
			if (files == null) {
				// access error
				Toast.makeText(getContext(), mMsgError, Toast.LENGTH_SHORT)
						.show();
				return -1;
			}
			if (mList != null) {
				mList.clear();
			} else {
				mList = new ArrayList<Map<String, Object>>(files.length);
			}

			// two list for folders and files
			ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();
			ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();

			// add root directory, and back to parent direcotry
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", mPathDefault);
			map.put("path", mPathDefault);
			map.put("img", mContext.getString(R.string.fa_folder));
			map.put("icon_color", "#628EAE");
			mList.add(map);

			map = new HashMap<String, Object>();
			map.put("name", mPathParent);
			map.put("path", mPath);
			map.put("img", mContext.getString(R.string.fa_level_up));
			map.put("icon_color", "#628EAE");
			mList.add(map);

			for (File file : files) {
				if (file.isDirectory() && file.listFiles() != null) {
					// add folders
					map = new HashMap<String, Object>();
					map.put("name", file.getName());
					map.put("path", file.getPath());
					map.put("img", mContext.getString(R.string.fa_folder));
					map.put("icon_color", "#628EAE");
					lfolders.add(map);
				} else if (file.isFile()) {
					// add files
					String sf = getSuffix(file.getName()).toLowerCase();
					if (mSuffix == null
							|| mSuffix.length() == 0
							|| (sf.length() > 0 && mSuffix.indexOf("." + sf
									+ ";") >= 0)) {
						map = new HashMap<String, Object>();

						map.put("path", file.getPath());
						String fileName = file.getName();
						map.put("name", fileName);
						int pos = fileName.lastIndexOf(".");
						if (pos <= 0)
							continue;
						String posix = fileName.substring(pos);
						map.put("img",
								mContext.getString(FileTypeIcon.getIcon(posix)));
						map.put("icon_color", FileTypeIcon.getColor(posix));
						lfiles.add(map);
					}
				}
			}

			mList.addAll(lfolders); // first add folders
			if (!mSelDirecotry)
				mList.addAll(lfiles); // then add files

			setDivider(mContext.getResources().getDrawable(
					android.R.color.black));
			setDividerHeight(1);
			SimpleAdapter adapter = new LocalFileAdatper(getContext(), mList,
					R.layout.file_dialog_item, new String[] { "img", "name" },
					new int[] { R.id.filedialogitem_icon,
							R.id.filedialogitem_name });
			this.setAdapter(adapter);

			return files.length;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {

			String pt = (String) mList.get(position).get("path");
			String fn = (String) mList.get(position).get("name");
			if (fn.equals(mPathDefault) || fn.equals(mPathParent)) {
				// if root or parent directory
				File fl = new File(pt);
				String ppt = fl.getParent();
				if (ppt != null) {
					// back to parent
					mPath = ppt;
				} else {
					// back to root
					mPath = mPathDefault;
				}
			} else {

				File fl = new File(pt);
				if (fl.isFile()) {
					if (mSelDirecotry)
						return;
					// parameters for the callback
					Bundle bundle = new Bundle();
					bundle.putString("path", pt);
					bundle.putString("name", fn);
					// call the callback
					this.mCallback.callback(bundle);
					return;
				} else if (fl.isDirectory()) {
					// if directory
					// go to the direcotry
					mPath = pt;
				}
			}
			this.refreshFileList();
		}
	}
}

class LocalFileAdatper extends SimpleAdapter {

	private Context mContext = null;
	List<Map<String, Object>> mDatas = null;

	public LocalFileAdatper(Context context, List<Map<String, Object>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
		mContext = context;
		mDatas = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Map<String, Object> map = mDatas.get(position);
		View res = super.getView(position, convertView, parent);
		TextView textView = (TextView) res
				.findViewById(R.id.filedialogitem_icon);
		textView.setTypeface(GlobalUtil.getFontAwesome(mContext));
		Object color = map.get("icon_color");
		textView.setTextColor(Color.parseColor(color.toString()));
		return res;
	}

}
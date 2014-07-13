// filename: OpenFileDialog.java
package com.dismantle.mediagrid;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class OpenFileDialog {
	public static String tag = "OpenFileDialog";
	static final public String sRoot = Environment
			.getExternalStorageDirectory().getPath();
	static final public String sParent = "..";
	static final public String sFolder = ".";
	static final public String sEmpty = "";
	static final public String sDefault = sRoot + "/MediaGrid";
	static final private String sOnErrorMsg = "No rights to access!";

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
		Map<String, Integer> images = new HashMap<String, Integer>();
		// ���漸�����ø��ļ����͵�ͼ�꣬ ��Ҫ���Ȱ�ͼ����ӵ���Դ�ļ���
		images.put(OpenFileDialog.sRoot, R.drawable.folder_ico); // ��Ŀ¼ͼ��
		images.put(OpenFileDialog.sParent, R.drawable.folder_ico); // ������һ���ͼ��
		images.put(OpenFileDialog.sFolder, R.drawable.folder_ico); // �ļ���ͼ��
		images.put("wav", R.drawable.file_ico); // wav�ļ�ͼ��
		images.put(OpenFileDialog.sEmpty, R.drawable.file_ico);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final FileSelectView fileSelectView = new FileSelectView(context,
				callback, suffix, selDirectory, images);

		if (selDirectory) {
			RelativeLayout relativeLayout = new RelativeLayout(context);
			Button btnOK = new Button(context);
			btnOK.setText("Select this directory");
			btnOK.setId(12345678);
			btnOK.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Bundle bundle = new Bundle();
					bundle.putString("path", fileSelectView.path);

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
			int id = btnOK.getId();
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

		private CallbackBundle callback = null;
		private String path = sDefault;
		private List<Map<String, Object>> list = null;

		private String suffix = null;

		private Map<String, Integer> imagemap = null;
		private Context context;
		private boolean selDirecotry;

		public FileSelectView(Context context, CallbackBundle callback,
				String suffix, boolean selDirecotry, Map<String, Integer> images) {
			super(context);
			this.imagemap = images;
			this.suffix = suffix == null ? "" : suffix.toLowerCase();
			this.callback = callback;
			this.context = context;
			this.selDirecotry = selDirecotry;
			this.setOnItemClickListener(this);

			File file = new File(path);
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

		private int getImageId(String s) {
			if (imagemap == null) {
				return 0;
			} else if (imagemap.containsKey(s)) {
				return imagemap.get(s);
			} else if (imagemap.containsKey(sEmpty)) {
				return imagemap.get(sEmpty);
			} else {
				return 0;
			}
		}

		private int refreshFileList() {
			// ˢ���ļ��б�
			File[] files = null;
			try {
				files = new File(path).listFiles();
			} catch (Exception e) {
				files = null;
			}
			if (files == null) {
				// ���ʳ���
				Toast.makeText(getContext(), sOnErrorMsg, Toast.LENGTH_SHORT)
						.show();
				return -1;
			}
			if (list != null) {
				list.clear();
			} else {
				list = new ArrayList<Map<String, Object>>(files.length);
			}

			// �����ȱ����ļ��к��ļ��е������б�
			ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();
			ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();

			if (!this.path.equals(sRoot)) {
				// ��Ӹ�Ŀ¼ �� ��һ��Ŀ¼
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", sRoot);
				map.put("path", sRoot);
				map.put("img", getImageId(sRoot));
				list.add(map);

				map = new HashMap<String, Object>();
				map.put("name", sParent);
				map.put("path", path);
				map.put("img", getImageId(sParent));
				list.add(map);
			}

			for (File file : files) {
				if (file.isDirectory() && file.listFiles() != null) {
					// ����ļ���
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("name", file.getName());
					map.put("path", file.getPath());
					map.put("img", getImageId(sFolder));
					lfolders.add(map);
				} else if (file.isFile()) {
					// ����ļ�
					String sf = getSuffix(file.getName()).toLowerCase();
					if (suffix == null
							|| suffix.length() == 0
							|| (sf.length() > 0 && suffix.indexOf("." + sf
									+ ";") >= 0)) {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("name", file.getName());
						map.put("path", file.getPath());
						map.put("img", getImageId(sf));
						lfiles.add(map);
					}
				}
			}

			list.addAll(lfolders); // ������ļ��У�ȷ���ļ�����ʾ������
			if (!selDirecotry)
				list.addAll(lfiles); // ������ļ�

			SimpleAdapter adapter = new SimpleAdapter(
					getContext(),
					list,
					R.layout.file_dialog_item,
					new String[] { "img", "name", "path" },
					new int[] { R.id.filedialogitem_img,
							R.id.filedialogitem_name, R.id.filedialogitem_path });
			this.setAdapter(adapter);
			return files.length;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {

			// ��Ŀѡ��
			String pt = (String) list.get(position).get("path");
			String fn = (String) list.get(position).get("name");
			if (fn.equals(sRoot) || fn.equals(sParent)) {
				// ����Ǹ�Ŀ¼������һ��
				File fl = new File(pt);
				String ppt = fl.getParent();
				if (ppt != null) {
					// ������һ��
					path = ppt;
				} else {
					// ���ظ�Ŀ¼
					path = sRoot;
				}
			} else {
				
				File fl = new File(pt);
				if (fl.isFile()) {
					if (selDirecotry)
						return;
					// ���ûص��ķ���ֵ
					Bundle bundle = new Bundle();
					bundle.putString("path", pt);
					bundle.putString("name", fn);
					// �����������õĻص�����
					this.callback.callback(bundle);
					return;
				} else if (fl.isDirectory()) {
					// ������ļ���
					// ��ô����ѡ�е��ļ���
					path = pt;
				}
			}
			this.refreshFileList();
		}
	}
}

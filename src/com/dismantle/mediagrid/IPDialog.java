package com.dismantle.mediagrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class IPDialog {
	/**
	 * server configure dialog
	 * 
	 * @param context
	 *            activity context
	 * @param title
	 *            title of dialog
	 * @param initIp
	 *            initial ip address
	 * @param initPort
	 *            initial port number
	 * @param listener
	 *            input dialog callback listener
	 */
	public static void showIPInputDialog(final Context context,
			final String title, final String initIp, final int initPort,
			final onIPInputDialogProcess listener) {
		// view layout
		final View viewConfig = LayoutInflater.from(context).inflate(
				R.layout.server_config, null);
		// ip input text box
		final EditText txtIP = (EditText) viewConfig.findViewById(R.id.txt_ip);
		// port input text box
		final EditText txtPort = (EditText) viewConfig
				.findViewById(R.id.txt_port);
		// message box to indicate bad format
		final TextView tvBadFormat = (TextView) viewConfig
				.findViewById(R.id.tv_bad_format);
		// initialize
		txtIP.setText(initIp);
		txtPort.setText(String.valueOf(initPort));
		tvBadFormat.setVisibility(TextView.INVISIBLE);
		// server configure alert dialog
		final AlertDialog alertDialog = new AlertDialog.Builder(context)
				.setTitle(title)
				.setView(viewConfig)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (listener != null)
									listener.onIPInputCancel();
							}
						}).show();
		//positive button on clicker listener
		alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {

						String strIP = txtIP.getText().toString();
						String strPort = txtPort.getText().toString();
						String[] ipNums = strIP.split("\\.");
						boolean isGood = true;
						int portNum = -1;
						try {
							// check the format of ip
							if (ipNums.length == 4) {
								for (int i = 0; i < 4; i++) {
									int tmpNum = Integer.valueOf(ipNums[i]);
									if (!(tmpNum <= 255 && tmpNum >= 0)) {
										isGood = false;
										break;
									}
								}
							} else
								isGood = false;
							// check the format of port
							if (isGood) {
								portNum = Integer.valueOf(strPort);
								if (portNum <= 0)
									isGood = false;
							}
						} catch (NumberFormatException e) {
							isGood = false;
						}
						if (isGood) {// good format, execute callback
							if (listener != null)
								listener.onIPInputConfirm(strIP, portNum);
							alertDialog.dismiss();
						} else
							// bad format
							tvBadFormat.setVisibility(TextView.VISIBLE);

					}
				});
	}

	interface onIPInputDialogProcess {
		void onIPInputConfirm(String ip, int port);

		void onIPInputCancel();
	}
}

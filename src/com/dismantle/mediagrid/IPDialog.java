package com.dismantle.mediagrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class IPDialog {
	// ip input dialog
	public static void showIPInputDialog(final Context context,
			final String title, final String initIp, final int initPort,
			final onIPInputDialogProcess listener) {
		final View viewConfig = LayoutInflater.from(context).inflate(
				R.layout.server_config, null);
		final EditText txtIP = (EditText) viewConfig.findViewById(R.id.txt_ip);
		final EditText txtPort = (EditText) viewConfig
				.findViewById(R.id.txt_port);
		final TextView tvBadFormat = (TextView) viewConfig
				.findViewById(R.id.tv_bad_format);
		txtIP.setText(initIp);
		txtPort.setText(String.valueOf(initPort));
		tvBadFormat.setVisibility(TextView.INVISIBLE);

		final AlertDialog alertDialog = new AlertDialog.Builder(context)
				.setTitle(title)
				.setView(viewConfig)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (listener != null)
									listener.onIPInputCancel();
							}
						}).show();
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
							// format of ip
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
							// format of port

							if (isGood) {
								portNum = Integer.valueOf(strPort);
								if (portNum <= 0)
									isGood = false;
							}
						} catch (NumberFormatException e) {
							isGood = false;
						}
						if (isGood) {// good format
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

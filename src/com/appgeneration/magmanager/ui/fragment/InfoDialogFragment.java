/**
 * 
 */
package com.appgeneration.magmanager.ui.fragment;

import android.R.integer;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.util.GlobalSettings;
import com.appgeneration.magmanager.util.UIUtils;

/**
 * @author miguelferreira
 * 
 */
public class InfoDialogFragment extends DialogFragment {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.DialogFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		int dialogWidthScreenPercentage = (int) getActivity().getResources().getDimension(
				R.dimen.info_dialog_width);
		int dialogHeightScreenPercentage = (int) getActivity().getResources().getDimension(
				R.dimen.info_dialog_height);
		Point windowSize = UIUtils.getWindowSize(getActivity());
		int windowWidth = windowSize.x;
		int windowHeight = windowSize.y;
		getDialog().getWindow().setLayout((int) (windowWidth * 0.6f),
				(int) (windowHeight * 0.8f));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
	 * android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View infoView = inflater.inflate(R.layout.info, container, false);
		TextView infoHyperlinkTextView = (TextView) infoView
				.findViewById(R.id.info_site_hyperlink);
		infoHyperlinkTextView
				.setText(Html
						.fromHtml("<a href=\"http://www.kidsbookmaker.com\">Kids Book Maker</a> "));
		infoHyperlinkTextView.setMovementMethod(LinkMovementMethod
				.getInstance());

		TextView contactUsTextView = (TextView) infoView
				.findViewById(R.id.info_contact_us_text_view);
		if (contactUsTextView != null) {
			contactUsTextView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// open email-send activity
					Intent i = new Intent(Intent.ACTION_SEND);
					i.setType("message/rfc822");
					i.putExtra(Intent.EXTRA_EMAIL,
							new String[] { "info@appgeneration.com" });
					i.putExtra(Intent.EXTRA_SUBJECT, "[KBR-Android]");
					try {
						startActivity(Intent.createChooser(i, "Send mail..."));
					} catch (android.content.ActivityNotFoundException ex) {
						Toast.makeText(getActivity(),
								"There are no email clients installed.",
								Toast.LENGTH_SHORT).show();
					}
				}
			});

		}

		Button editImageView = (Button) infoView
				.findViewById(R.id.info_edit_button);

		if (editImageView != null) {

			if (GlobalSettings.isUserInEditMode) {
				editImageView.setText(getResources().getString(R.string.read));
			} else {
				editImageView.setText(getResources().getString(R.string.edit));
			}
			editImageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// open email-send activity
					Intent i = new Intent(GlobalSettings.BROADCAST_EDIT_ACTION);
					LocalBroadcastManager.getInstance(getActivity())
							.sendBroadcast(i);
					getActivity().getSupportFragmentManager().popBackStack();
				}
			});
		}

		Button cancelImageView = (Button) infoView
				.findViewById(R.id.info_cancel_button);

		if (cancelImageView != null) {
			cancelImageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					getActivity().getSupportFragmentManager().popBackStack();
				}
			});
		}

		return infoView;
	}
}

/**
 * 
 */
package com.appgeneration.magmanager.ui.fragment;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.appgeneration.magmanager.interfaces.IssueUIHandler;
import com.appgeneration.magmanager.interfaces.MainActivityInterface;
import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.model.Issue;
import com.appgeneration.magmanager.tracking.AnalyticsTracking;
import com.appgeneration.magmanager.ui.adapter.IssueGridAdapter;
import com.appgeneration.magmanager.util.UIUtils;

/**
 * @author miguelferreira
 * 
 */
public class PreviewIssueFragment extends DialogFragment {

	private Issue issueToPreview = null;
	private WeakReference<IssueUIHandler> issueControllerHandler = null;

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
		View issuePreviewView = inflater.inflate(R.layout.issue_preview,
				container, false);

		if (issueToPreview == null) {
			throw new AssertionError("Impossible to preview a null issue");

		}

		TextView issueDescriptionTextView = (TextView) issuePreviewView
				.findViewById(R.id.issue_preview_description_text_view);

		issueDescriptionTextView.setText(issueToPreview.getPreviewText());

		ImageView issuePreviewCoverImageView = (ImageView) issuePreviewView
				.findViewById(R.id.issue_preview_cover_image_view);

		TextView issueDeleteTextView = (TextView) issuePreviewView
				.findViewById(R.id.issue_preview_delete_text_view);

		TextView isseTitleTextView = (TextView) issuePreviewView
				.findViewById(R.id.issue_preview_title_text_view);
		if (isseTitleTextView != null) {
			isseTitleTextView.setText(issueToPreview.getTitle());
		}

		issueDescriptionTextView.setText(issueToPreview.getPreviewText());

		if (issueDeleteTextView != null) {
			if (issueToPreview.canBeDeleted()) {
				issueDeleteTextView.setVisibility(View.VISIBLE);
				issueDeleteTextView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						onDeleteTextViewClicked();
					}
				});
			} else {
				issueDeleteTextView.setVisibility(View.INVISIBLE);
			}
		}

		TextView issueActionTextView = (TextView) issuePreviewView
				.findViewById(R.id.issue_preview_price_and_action_text_view);

		IssueGridAdapter.setPriceTextViewContentForIssue(getActivity(),
				issueToPreview, issueActionTextView, true);

		issueActionTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				getIssueControllerHandler().get().onIssueActionClickedForIssue(
						issueToPreview);
				getActivity().getSupportFragmentManager().popBackStack();
			}
		});

		new DownloadImageTask(issuePreviewCoverImageView)
				.execute(issueToPreview.getPreviewCover());

		// analytics tracking
		AnalyticsTracking.trackIssuePreview(issueToPreview);

		return issuePreviewView;
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

		Point size = UIUtils.getWindowSize(getActivity());
		int windowWidth = size.x;
		int windowHeight = size.y;
		getDialog().getWindow().setLayout((int) (windowWidth * 0.7f),
				(int) (windowHeight * 0.8f));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.DialogFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_TITLE, 0);
	}

	private void onDeleteTextViewClicked() {
		((MainActivityInterface) getActivity())
				.setIssueIdBeingDeleted(issueToPreview.getId());
		DeletionConfirmationDialogFragment newFragment = DeletionConfirmationDialogFragment
				.newInstance(R.string.alert_dialog_delete_issues_text,
						(MainActivityInterface) getActivity());

		newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
		getActivity().getSupportFragmentManager().popBackStack();
	}

	/**
	 * @return the issueToPreview
	 */
	public Issue getIssueToPreview() {
		return issueToPreview;
	}

	/**
	 * @param issueToPreview
	 *            the issueToPreview to set
	 */
	public void setIssueToPreview(Issue issueToPreview) {
		this.issueToPreview = issueToPreview;
	}

	/**
	 * @return the issueControllerHandler
	 */
	public WeakReference<IssueUIHandler> getIssueControllerHandler() {
		return issueControllerHandler;
	}

	/**
	 * @param issueControllerHandler
	 *            the issueControllerHandler to set
	 */
	public void setIssueControllerHandler(
			WeakReference<IssueUIHandler> issueControllerHandler) {
		this.issueControllerHandler = issueControllerHandler;
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		WeakReference<ImageView> bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = new WeakReference<ImageView>(bmImage);
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				if (e!= null && e.getMessage()!=null) {
					Log.e("Error", e.getMessage());
					e.printStackTrace();
				}
				
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			if (bmImage.get() != null) {
				bmImage.get().setImageBitmap(result);
			}
		}
	}
}

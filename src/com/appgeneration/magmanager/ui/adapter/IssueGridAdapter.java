/**
 * 
 */
package com.appgeneration.magmanager.ui.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.appgeneration.magmanager.imagefetcher.ImageFetcher;
import com.appgeneration.magmanager.interfaces.IssueUIHandler;
import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.model.DocumentStatus;
import com.appgeneration.magmanager.model.Issue;
import com.appgeneration.magmanager.model.ItemStatus;
import com.appgeneration.magmanager.util.GlobalSettings;
import com.appgeneration.magmanager.util.InflateUtils;

/**
 * @author miguelferreira
 * 
 */
public class IssueGridAdapter extends BaseAdapter {

	private List<Issue> issues;
	private ImageFetcher imageFetcher;
	private LayoutInflater layoutInflater;
	private IssueUIHandler issueUIHandler;
	private Context context;

	// click listener
	private OnClickListener issueActionImageViewOnCLickListener;
	private OnClickListener issueCoverImageViewOnCLickListener;

	public IssueGridAdapter(Context context, List<Issue> issues,
			ImageFetcher imageFetcher, IssueUIHandler issueUIHandler) {
		this.context = context;
		this.issues = issues;
		this.imageFetcher = imageFetcher;
		this.issueUIHandler = issueUIHandler;

		layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		issueActionImageViewOnCLickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				onIssueActionViewClicked(v);
			}
		};

		issueCoverImageViewOnCLickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				onIssueCoverViewClicked(v);
			}
		};
	}

	// UI LISTENER HELPERS
	private void onIssueActionViewClicked(View issueActionImageView) {
		if (issueUIHandler != null) {
			issueUIHandler
					.onIssueActionClickedForIssue((Issue) issueActionImageView
							.getTag());
		}
	}

	private void onIssueCoverViewClicked(View issueCoverImageView) {
		if (issueUIHandler != null) {
			issueUIHandler
					.onIssueCoverClickedForIssue((Issue) issueCoverImageView
							.getTag());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return issues.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		return issues.get(position);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		IssueCellViewHolder issueCellViewHolder = null;
		View returnView = convertView;
		Issue currentIssue = issues.get(position);

		if (returnView == null) {
			returnView = layoutInflater.inflate(R.layout.issue_cell, parent,
					false);
			IssueCellViewHolder issueCellViewHolderTmp = new IssueCellViewHolder();
			issueCellViewHolderTmp.issueActionImageView = (ImageView) InflateUtils
					.findViewByIdOrReturnNull(returnView,
							R.id.issue_action_image_view);
			issueCellViewHolderTmp.issueCoverImageView = (ImageView) InflateUtils
					.findViewByIdOrReturnNull(returnView, R.id.issue_cover);
			issueCellViewHolderTmp.issueDownloadProgressBar = (ProgressBar) InflateUtils
					.findViewByIdOrReturnNull(returnView,
							R.id.issue_progress_bar);
			issueCellViewHolderTmp.issueSpecialStatusTextView = (TextView) InflateUtils
					.findViewByIdOrReturnNull(returnView,
							R.id.issue_special_status_text_view);
			issueCellViewHolderTmp.issueTitleTextView = (TextView) InflateUtils
					.findViewByIdOrReturnNull(returnView, R.id.issue_title);
			issueCellViewHolderTmp.issuePriceTextView = (TextView) InflateUtils
					.findViewByIdOrReturnNull(returnView,
							R.id.issue_price_text_view);
			issueCellViewHolderTmp.issueExtractProgressBar = (ProgressBar) returnView
					.findViewById(R.id.issue_cell_unzip_progress);
			issueCellViewHolderTmp.issueDeleteImageView = (ImageView) returnView
					.findViewById(R.id.issue_cell_delete_image_view);

			issueCellViewHolder = issueCellViewHolderTmp;
			returnView.setTag(issueCellViewHolderTmp);
		} else {
			issueCellViewHolder = (IssueCellViewHolder) returnView.getTag();
		}

		issueCellViewHolder.issuesListPosition = position;
		issueCellViewHolder.issue = currentIssue;
		ImageView issueCoverImageView = issueCellViewHolder.issueCoverImageView;
		TextView issueTitleTextView = issueCellViewHolder.issueTitleTextView;
		ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;
		ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
		TextView issuePriceTextView = issueCellViewHolder.issuePriceTextView;
		TextView issueSpecialStatusTextView = issueCellViewHolder.issueSpecialStatusTextView;
		ProgressBar issueExtractProgressBar = issueCellViewHolder.issueExtractProgressBar;
		ImageView issueDeleteImageView = issueCellViewHolder.issueDeleteImageView;

		if (issueExtractProgressBar != null) {
			issueExtractProgressBar.setVisibility(View.GONE);
		}

		// helper tags
		if (issueActionImageView != null) {
			issueActionImageView.setTag(currentIssue);
		}
		if (issuePriceTextView != null) {
			issuePriceTextView.setTag(currentIssue);
		}
		if (issueCoverImageView != null) {
			issueCoverImageView.setTag(currentIssue);
		}

		if (imageFetcher != null) {
			imageFetcher.loadImage(currentIssue.getCover(),
					issueCoverImageView, R.drawable.cover_placeholder);
		}

		if (issueTitleTextView != null) {
			issueTitleTextView.setText(currentIssue.getTitle());
		}

		// queued?
		if (issueSpecialStatusTextView != null) {
			if (currentIssue.getDocumentStatus() == DocumentStatus.QUEUED) {
				issueSpecialStatusTextView.setVisibility(ViewGroup.VISIBLE);
				issueSpecialStatusTextView.setText(R.string.issue_queued);
			} else {
				issueSpecialStatusTextView.setVisibility(ViewGroup.GONE);
			}
		}

		// reset progress bar
		if (issueDownloadProgressBar != null) {
			issueDownloadProgressBar.setVisibility(ViewGroup.GONE);
			issueDownloadProgressBar.setProgress(0);
		}

		if (issueActionImageView != null) {
			issueActionImageView.setVisibility(ViewGroup.VISIBLE);
		}

		if (currentIssue.getDocumentStatus() == DocumentStatus.AVAILABLE) {
			onDownloadReadyToStart(returnView);
		} else if (currentIssue.getDocumentStatus() == DocumentStatus.READY) {
			onDownloadFinished(returnView);
		} else if (currentIssue.getDocumentStatus() == DocumentStatus.DOWNLOADING) {
			if (issueActionImageView != null) {
				issueActionImageView.setImageResource(R.drawable.issue_pause);
			}

			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.VISIBLE);
				issueDownloadProgressBar.setProgress(currentIssue
						.getDownloadProgress());
			}

			changeImageViewAlpha(issueCoverImageView, 0.4f);

		} else if (currentIssue.getDocumentStatus() == DocumentStatus.PAUSED) {
			if (issueActionImageView != null) {
				issueActionImageView
						.setImageResource(R.drawable.issue_download);
			}

			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.VISIBLE);
				issueDownloadProgressBar.setProgress(currentIssue
						.getDownloadProgress());
			}

			changeImageViewAlpha(issueCoverImageView, 0.4f);
		} else if (currentIssue.getDocumentStatus() == DocumentStatus.QUEUED) {
			onDownloadQueued(returnView);
		} else if (currentIssue.getDocumentStatus() == DocumentStatus.UNCOMPRESSING) {
			onUncompressStarted(returnView);
		}

		// price-related stuff
		IssueGridAdapter.setPriceTextViewContentForIssue(context, currentIssue,
				issuePriceTextView, false);

		// set images on click callbacks
		if (issueActionImageView != null) {
			issueActionImageView
					.setOnClickListener(issueActionImageViewOnCLickListener);
		}

		if (issuePriceTextView != null) {
			issuePriceTextView
					.setOnClickListener(issueActionImageViewOnCLickListener);
		}

		if (issueCoverImageView != null) {
			issueCoverImageView
					.setOnClickListener(issueCoverImageViewOnCLickListener);
		}

		// editing?

		if (GlobalSettings.isUserInEditMode && currentIssue.isReadyToBeRead()) {
			if (issueDeleteImageView!=null) {
				issueDeleteImageView.setVisibility(View.VISIBLE);
			}
			
			changeImageViewAlpha(issueCoverImageView, 0.4f);
		} else {
			if (issueDeleteImageView!=null) {
				issueDeleteImageView.setVisibility(View.GONE);
			}
			//changeImageViewAlpha(issueCoverImageView, 1.0f);
		}

		return returnView;
	}

	public static void setPriceTextViewContentForIssue(Context context,
			Issue currentIssue, TextView issuePriceTextView,
			boolean isTheIssueBeingPreviewed) {
		if (issuePriceTextView == null) {
			return;
		}
		if (currentIssue.isReadyToBeRead()) {
			if (isTheIssueBeingPreviewed) {
				issuePriceTextView.setText(context.getResources().getString(
						R.string.read));
			} else {
				issuePriceTextView.setText(context.getResources().getString(
						R.string.issue_owned));
			}

		} else {
			if (currentIssue.isFree()) {
				if (isTheIssueBeingPreviewed) {
					issuePriceTextView.setText(context.getResources()
							.getString(R.string.download));
				} else {
					issuePriceTextView.setText(context.getResources()
							.getString(R.string.price_free));
				}

			} else if (currentIssue.isPaid()) {

				if (isTheIssueBeingPreviewed) {
					if (currentIssue.getItemStatus() == ItemStatus.BOUGHT) {
						issuePriceTextView.setText(context.getResources()
								.getString(R.string.download));
					} else {
						issuePriceTextView.setText(context.getResources()
								.getString(R.string.buy));
					}

				} else {
					if (currentIssue.getItemStatus() == ItemStatus.LOADING) {
						issuePriceTextView.setText(context.getResources()
								.getString(R.string.price_loading));
					} else if (currentIssue.getItemStatus() == ItemStatus.AVAILABLE_TO_BUY) {
						issuePriceTextView.setText(currentIssue.getPrice());
					} else if (currentIssue.getItemStatus() == ItemStatus.UNAVAILABE) {
						issuePriceTextView.setText(context.getResources()
								.getString(R.string.price_unavailable));
					} else if (currentIssue.getItemStatus() == ItemStatus.BOUGHT) {
						issuePriceTextView.setText(context.getResources()
								.getString(R.string.price_bought));
					}
				}

			}
		}
	}

	private void changeImageViewAlpha(ImageView view, float alpha) {
		if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) < 11) {
			view.setAlpha((int) (alpha * 255));
		} else {
			view.setAlpha(alpha);
		}
	}

	/**
	 * DOWNLOAD VISUAL FEEDBACK HANDLER
	 */

	public void onDownloadQueued(View issueQueuedView) {
		if (issueQueuedView != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issueQueuedView
					.getTag();
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setVisibility(ViewGroup.GONE);
			}

			ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;

			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.GONE);
			}

			ImageView issueImageView = issueCellViewHolder.issueCoverImageView;
			changeImageViewAlpha(issueImageView, 0.4f);

			TextView issueQueuedTextView = issueCellViewHolder.issueSpecialStatusTextView;

			if (issueQueuedTextView != null) {
				issueQueuedTextView.setVisibility(ViewGroup.VISIBLE);
				issueQueuedTextView.setText(R.string.issue_queued);
			}

		}
	}

	public void onDownloadStarted(View issueDownloadStartedView) {
		if (issueDownloadStartedView != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issueDownloadStartedView
					.getTag();
			TextView issueQueuedTextView = issueCellViewHolder.issueSpecialStatusTextView;
			if (issueQueuedTextView != null) {
				issueQueuedTextView.setVisibility(ViewGroup.GONE);
			}

			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setVisibility(ViewGroup.VISIBLE);
				issueActionImageView.setImageResource(R.drawable.issue_pause);
			}

		}
	}

	public void postDownloadProgress(int progress,
			View currentIssueBeingDownloaded) {
		if (currentIssueBeingDownloaded != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) currentIssueBeingDownloaded
					.getTag();
			ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;

			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.VISIBLE);
				issueDownloadProgressBar.setProgress(progress);
			}

			ImageView IssueCoverImageView = issueCellViewHolder.issueCoverImageView;
			changeImageViewAlpha(IssueCoverImageView, 0.4f);
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setVisibility(ViewGroup.VISIBLE);
				issueActionImageView.setImageResource(R.drawable.issue_pause);
			}

		}

	}

	public void onDownloadFinished(View currentIssueBeingDownloaded) {
		if (currentIssueBeingDownloaded != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) currentIssueBeingDownloaded
					.getTag();
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setImageResource(R.drawable.issue_read);
				issueActionImageView.setVisibility(View.VISIBLE);
			}

			TextView issueSpecialStatusTextView = issueCellViewHolder.issueSpecialStatusTextView;
			if (issueSpecialStatusTextView != null) {
				issueSpecialStatusTextView.setVisibility(ViewGroup.GONE);
			}

			ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;
			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.GONE);
			}

			ImageView IssueCoverImageView = issueCellViewHolder.issueCoverImageView;
			changeImageViewAlpha(IssueCoverImageView, 1.0f);

			ProgressBar unzipProgressBar = issueCellViewHolder.issueExtractProgressBar;
			unzipProgressBar.setVisibility(View.GONE);
		}
	}

	public void onDownloadFailedOrPaused(View issueDownloadFailedOrPausedView) {
		if (issueDownloadFailedOrPausedView != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issueDownloadFailedOrPausedView
					.getTag();
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setVisibility(ViewGroup.VISIBLE);
				issueActionImageView
						.setImageResource(R.drawable.issue_download);
			}

			ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;
			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.VISIBLE);
			}

		}
	}

	public void onDownloadNotStartedDueToNoInternet(
			View issueDownloadCancelledView) {
		if (issueDownloadCancelledView != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issueDownloadCancelledView
					.getTag();
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setVisibility(ViewGroup.VISIBLE);
				issueActionImageView
						.setImageResource(R.drawable.issue_download);
			}

		}
	}

	/*
	 * ISSUE UNCOMPRESS CALLBACKS
	 */

	public void onUncompressStarted(View issueUncompressStartedView) {
		if (issueUncompressStartedView != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issueUncompressStartedView
					.getTag();
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView.setVisibility(ViewGroup.GONE);
			}

			ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;
			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.GONE);
			}

			ImageView issueImageView = issueCellViewHolder.issueCoverImageView;
			changeImageViewAlpha(issueImageView, 0.4f);
			ProgressBar unzipProgressBar = issueCellViewHolder.issueExtractProgressBar;
			unzipProgressBar.setVisibility(View.VISIBLE);

		}
	}

	public void onUncompressFinished(View issueUncompressStartedView) {
		onDownloadFinished(issueUncompressStartedView);
	}

	public void onDownloadReadyToStart(View issuePurchasedView) {
		if (issuePurchasedView != null) {
			IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issuePurchasedView
					.getTag();
			ImageView issueActionImageView = issueCellViewHolder.issueActionImageView;
			if (issueActionImageView != null) {
				issueActionImageView
						.setImageResource(R.drawable.issue_download);
			}

			TextView issueSpecialStatusTextView = issueCellViewHolder.issueSpecialStatusTextView;
			if (issueSpecialStatusTextView != null) {
				issueSpecialStatusTextView.setVisibility(ViewGroup.GONE);
			}

			ProgressBar issueDownloadProgressBar = issueCellViewHolder.issueDownloadProgressBar;
			if (issueDownloadProgressBar != null) {
				issueDownloadProgressBar.setVisibility(ViewGroup.GONE);
			}

			ImageView IssueCoverImageView = issueCellViewHolder.issueCoverImageView;
			changeImageViewAlpha(IssueCoverImageView, 1.0f);
			TextView issuePriceTextView = issueCellViewHolder.issuePriceTextView;
			if (issuePriceTextView != null) {
				setPriceTextViewContentForIssue(context,
						issueCellViewHolder.issue, issuePriceTextView, false);
			}

			ImageView deleteImageView = issueCellViewHolder.issueDeleteImageView;
			if (deleteImageView != null) {
				deleteImageView.setVisibility(View.GONE);
			}

		}

	}

	public void onIssueDeleted(View issueDeletedView) {
		if (issueDeletedView != null) {
			onDownloadReadyToStart(issueDeletedView);
		}
	}

}

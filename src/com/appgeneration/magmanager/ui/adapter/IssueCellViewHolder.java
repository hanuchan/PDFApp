/**
 * 
 */
package com.appgeneration.magmanager.ui.adapter;

import com.appgeneration.magmanager.model.Issue;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @author miguelferreira
 * 
 */
public class IssueCellViewHolder {

	// view
	public ImageView issueActionImageView = null;
	public ProgressBar issueDownloadProgressBar = null;
	public ProgressBar issueExtractProgressBar = null;
	public TextView issueSpecialStatusTextView = null;
	public ImageView issueCoverImageView = null;
	public TextView issueTitleTextView = null;
	public TextView issuePriceTextView = null;
	public ImageView issueDeleteImageView = null;
	public int issuesListPosition = -1;
	public Issue issue;

}

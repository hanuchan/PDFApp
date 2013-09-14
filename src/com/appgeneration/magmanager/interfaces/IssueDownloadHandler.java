/**
 * 
 */
package com.appgeneration.magmanager.interfaces;

/**
 * @author Miguel
 * 
 */
public interface IssueDownloadHandler {

	public void postDownloadProgress(int progress, long issueId);

	public void onDownloadQueued(long issueId);

	public void onDownloadStarted(long issueId);

	public void onDownloadFinished(long issueId);

	public void onDownloadFailed(long issueId);

	public void onDownloadCancelled(long issueId);

	public void onDownloadNotStartedDueToNoInternet(long issueId);

}

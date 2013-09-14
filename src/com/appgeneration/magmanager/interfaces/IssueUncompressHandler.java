/**
 * 
 */
package com.appgeneration.magmanager.interfaces;

/**
 * @author Miguel
 *
 */
public interface IssueUncompressHandler {
	public void onUncompressStarted(long issueId);
	public void onUncompressFinished(long issueId);
	public void onUncompressFailed(long issueId);
	public void onUncompressNotStartedDueToCongestion(long issueId);
}

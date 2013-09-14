/**
 * 
 */
package com.appgeneration.magmanager.interfaces;

import java.util.List;

import com.appgeneration.magmanager.model.Issue;

/**
 * @author Miguel
 * 
 */
public interface IssueChangesHandler {

	/**
	 * Handles issues creation, update or deletion, by receiving the new list of
	 * issues everytime they change
	 * 
	 * @param currentIssues The new list of issues
	 * @param areIssuesLocal TODO
	 */
	public void onIssuesChanged(List<Issue> currentIssues, boolean areIssuesLocal);
	
	public void onIssuesNotLoadedDueToNoInternetConnection();
	public void onIssuesNotLoadedDueToServerResponseError();
	public void onIssuesNotLoadedDueToUnknownError();

}

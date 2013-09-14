package com.appgeneration.magmanager.interfaces;

import com.appgeneration.magmanager.model.Issue;

/**
 * 
 * @author Miguel
 * 
 *         This interface defines a set of methods that an ui issue handler must
 *         override.
 * 
 */
public interface IssueUIHandler {
	public void onIssueActionClickedForIssue(Issue issueClicked);
	public void onIssueCoverClickedForIssue(Issue issueClicked);
}

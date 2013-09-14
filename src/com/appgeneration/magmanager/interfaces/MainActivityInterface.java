/**
 * 
 */
package com.appgeneration.magmanager.interfaces;

import com.appgeneration.magmanager.model.Issue;

/**
 * @author miguelferreira
 *
 */
public interface MainActivityInterface {
	public void previewIssue(Issue clickedIssue);
	public void setIssueIdBeingDeleted(long id);
	public void onClickDeleteIssue();
	public void onClickNotDeleteIssue();
	public void startReadingBookWithPath(String bookPath);
}

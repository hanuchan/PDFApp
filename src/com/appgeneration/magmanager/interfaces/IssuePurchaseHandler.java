/**
 * 
 */
package com.appgeneration.magmanager.interfaces;


/**
 * @author Miguel
 * 
 */
public interface IssuePurchaseHandler {

	public void onIssuePurchased(long issueId);
	public void onIssueNotPurchasedDueToUserOption(long issueId);
	public void onIssueNotPurchasedDueToFailure(long issueId);
	public void onIssueNotPurchasedDueToInvalidReponseKey(long issueId);
	public void onIssueNotPurchasedDueToAPurchaseAlreadyInProgress(long currentIssueIdBeingBought);
}

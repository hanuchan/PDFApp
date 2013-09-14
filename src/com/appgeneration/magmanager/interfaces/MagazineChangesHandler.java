/**
 * 
 */
package com.appgeneration.magmanager.interfaces;

import com.appgeneration.magmanager.model.Magazine;

/**
 * @author Miguel
 * 
 */
public interface MagazineChangesHandler {

	/**
	 * Handles issues creation, update or deletion, by receiving the magazine everytime it changes
	 * 
	 * @param currentIssues The new list of issues
	 */
	public void magazineChanged(Magazine currentMagazine);

}

/**
 * 
 */
package com.appgeneration.magmanager.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.appgeneration.magmanager.interfaces.MainActivityInterface;
import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.ui.activities.StoreActivity;

/**
 * @author miguelferreira
 * 
 */
public class DeletionConfirmationDialogFragment extends DialogFragment {

	private static MainActivityInterface storeActivity = null;

	public static DeletionConfirmationDialogFragment newInstance(int title,
			MainActivityInterface storeActivityP) {
		DeletionConfirmationDialogFragment frag = new DeletionConfirmationDialogFragment();
		Bundle args = new Bundle();
		args.putInt("title", title);
		frag.setArguments(args);
		storeActivity = storeActivityP;
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int title = getArguments().getInt("title");

		return new AlertDialog.Builder(getActivity())
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(title)
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								storeActivity.onClickDeleteIssue();
							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								storeActivity.onClickNotDeleteIssue();
							}
						}).create();
	}
}

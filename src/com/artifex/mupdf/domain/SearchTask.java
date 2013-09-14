package com.artifex.mupdf.domain;


import com.appgeneration.magmanager.library.R;
import com.artifex.mupdf.AsyncTask;
import com.artifex.mupdf.MuPDFCore;
import com.library.activity.MuPDFActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.Handler;

class ProgressDialogX extends ProgressDialog {
	public ProgressDialogX(Context context) {
		super(context);
	}

	private boolean mCancelled = false;

	public boolean isCancelled() {
		return mCancelled;
	}

	@Override
	public void cancel() {
		mCancelled = true;
		super.cancel();
	}
}

public abstract class SearchTask {
	private static final int SEARCH_PROGRESS_DELAY = 200;
	private final Context mContext;
	private final MuPDFCore mCore;
	private final Handler mHandler;
	private final AlertDialog.Builder mAlertBuilder;
	private int countMatches = 0 ;//nhi add for search all pages
	private  String pageListStr ="";//nhi add for search all pages
	private AsyncTask<Void,Integer,SearchTaskResult> mSearchTask;

	public SearchTask(Context context, MuPDFCore core) {
		mContext = context;
		mCore = core;
		mHandler = new Handler();
		mAlertBuilder = new AlertDialog.Builder(context);
	}

	protected abstract void onTextFound(SearchTaskResult result);

	public void stop() {
		if (mSearchTask != null) {
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
		if( MuPDFActivity.searchAllPages)
		{
			countMatches=0;
			pageListStr="";
		}
	}

	public void go(final String text, int direction, int displayPage, final int searchPage) {
		if (mCore == null)
			return;
		stop();

		final int increment = direction;
		final int startIndex = searchPage == -1 ? displayPage : searchPage + increment;

		final ProgressDialogX progressDialog = new ProgressDialogX(mContext);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setTitle(mContext.getString(R.string.searching_));
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				stop();
			}
		});
		progressDialog.setMax(mCore.countPages());

		mSearchTask = new AsyncTask<Void,Integer,SearchTaskResult>() {
			@Override
			protected SearchTaskResult doInBackground(Void... params) {
				if( !MuPDFActivity.searchAllPages) //search on one page
				{
					if(mCore.getDisplayPages()==1 )
					{
						int index = startIndex;
						while (0 <= index && index <mCore.countSinglePages() && !isCancelled()) {
							publishProgress(index);
							
							RectF searchHits[] = mCore.searchPage(index, text);
							if (searchHits != null && searchHits.length > 0)
								return SearchTaskResult.init(text, index, searchHits);
							
							index += increment;
						}
					}
					else
					{
	
						int index = startIndex;
						while ( index >= 0 && index < mCore.countPages()&& !isCancelled()) 
						{
							publishProgress(index);
							
							RectF searchHitsL[] = mCore.searchPage(index*2-1, text);
							
							RectF searchHitsR[] = mCore.searchPage(index*2, text);
							RectF searchHits[]=null;
							
							
							int l = searchHitsL != null?searchHitsL.length:0;
							int r = searchHitsR != null?searchHitsR.length:0;
							SearchTaskResult.lengthLeftBoxs = l;
							if( l +r >0)
							{
								searchHits = new RectF[l+r];
								for( int i = 0 ; i< l; i++)
								{
									searchHits[i] = searchHitsL[i];
								}
								for( int i = 0 ; i< r; i++)
								{
									searchHits[l+i] = searchHitsR[i];
								}
								return SearchTaskResult.init(text, index, searchHits);
							}
							index += increment;
						}
						
						
					}
				}
				else //for all page
				{
					int index = 0;
					if( MuPDFActivity.countResultOnePage == null )
					{
						MuPDFActivity.countResultOnePage = new int[mCore.countSinglePages()];
						MuPDFActivity.searchAllPageList =new SearchTaskResult[mCore.countSinglePages()];
					}
					while (0 <= index && index <mCore.countSinglePages() && !isCancelled()) 
					{
						publishProgress(index);
						
						RectF searchHits[] = mCore.searchPage(index, text);
						
						if( searchHits != null )
						{
							
							MuPDFActivity.countResultOnePage[index] = searchHits.length;
							MuPDFActivity.searchAllPageList[index] = SearchTaskResult.init(text, index, searchHits);
							countMatches += searchHits.length;
							if(searchHits.length > 0)
							{
								if( pageListStr.equals(""))
								{
									pageListStr += ""+ (index + 1);
								}
								else
									pageListStr += ", "+ (index + 1);
							}
						}
						else
						{
							MuPDFActivity.searchAllPageList[index] = null;
						}

						index++;
					}
					
				}
				if( countMatches > 0)
				{
					for( int i =0; i< mCore.countSinglePages(); i++)
					{
						if(MuPDFActivity.countResultOnePage[i] > 0 )
							return SearchTaskResult.init(text, i, MuPDFActivity.searchAllPageList[i].searchBoxes);
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(SearchTaskResult result) {
				progressDialog.cancel();
				if (result != null) {
					if( MuPDFActivity.searchAllPages)//nhi add for search all pages
					{
						mAlertBuilder.setTitle( " Found: "+ result.txt +", count "+ countMatches +" matches.");
					
						mAlertBuilder.setMessage("On pages: "+ pageListStr+".");
						AlertDialog alert = mAlertBuilder.create();
						alert.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
								(DialogInterface.OnClickListener)null);
						alert.show();
					}
					onTextFound(result);
				} else {
			
					mAlertBuilder.setTitle(SearchTaskResult.get() == null ? R.string.text_not_found : R.string.no_further_occurences_found);
					mAlertBuilder.setMessage(null);//nhi add for search all pages
					AlertDialog alert = mAlertBuilder.create();
					alert.setButton(AlertDialog.BUTTON_POSITIVE, "Dismiss",
							(DialogInterface.OnClickListener)null);
					alert.show();
				}
			}

			@Override
			protected void onCancelled() {
				progressDialog.cancel();
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				progressDialog.setProgress(values[0].intValue());
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				mHandler.postDelayed(new Runnable() {
					public void run() {
						if (!progressDialog.isCancelled())
						{
							progressDialog.show();
							progressDialog.setProgress(startIndex);
						}
					}
				}, SEARCH_PROGRESS_DELAY);
			}
		};

		mSearchTask.execute();
	}
}

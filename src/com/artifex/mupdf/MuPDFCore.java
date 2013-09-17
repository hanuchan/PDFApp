package com.artifex.mupdf;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class MuPDFCore {
	/* load our native library */
	static {
		System.loadLibrary("mupdf");
	}

	private static final String TAG = "MuPDFCore";

	/* Readable members */
	private int pageNum = -1;;
	private int numPages = -1;
	private int displayPages = 1;
	public float pageWidth;
	public float pageHeight;
	private String mFileName;

	private long globals;

	/* The native functions */
	/* The native functions */
	private native long openFile(String filename);
	private native long openBuffer();
	private native int countPagesInternal();
	private native void gotoPageInternal(int localActionPageNum);
	private native float getPageWidth();
	private native float getPageHeight();
	private native void drawPage(Bitmap bitmap,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native void updatePageInternal(Bitmap bitmap,
			int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native RectF[] searchPage(String text);
	private native int passClickEventInternal(int page, float x, float y);
	private native void setFocusedWidgetChoiceSelectedInternal(String [] selected);
	private native String [] getFocusedWidgetChoiceSelected();
	private native String [] getFocusedWidgetChoiceOptions();
	private native int setFocusedWidgetTextInternal(String text);
	private native String getFocusedWidgetTextInternal();
	private native int getFocusedWidgetTypeInternal();
	private native LinkInfo [] getPageLinksInternal(int page);
	private native RectF[] getWidgetAreasInternal(int page);
	private native OutlineItem [] getOutlineInternal();
	private native boolean hasOutlineInternal();
	private native boolean needsPasswordInternal();
	private native boolean authenticatePasswordInternal(String password);
	private native MuPDFAlertInternal waitForAlertInternal();
	private native void replyToAlertInternal(MuPDFAlertInternal alert);
	private native void startAlertsInternal();
	private native void stopAlertsInternal();
	private native void destroying();
	private native boolean hasChangesInternal();
	private native void saveInternal();

	public MuPDFCore(String filename) throws Exception {
		mFileName = filename;
		globals = openFile(filename);
		if (globals == 0)
		{
			throw new Exception("Failed to open "+filename);
		}
		onDestroy = false;
	}

	public String getFileName() {
		return mFileName;
	}

	public String getFileDirectory() {
		return (new File(getFileName())).getParent();
	}

	public int countPages() {
		if (numPages < 0)
			numPages = countPagesSynchronized();
		if(displayPages == 1)
			return numPages;
		if(numPages % 2 == 0) {
			return numPages / 2 + 1;
		}
		int toReturn = numPages / 2;
		return toReturn + 1;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	public void gotoPage(int page) {
		if (page > numPages - 1)
			page = numPages - 1;
		else if (page < 0)
			page = 0;
//		if (this.pageNum == page)
//			return;
		gotoPageInternal(page);
		this.pageNum = page;
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();
	}

	public synchronized PointF getPageSize(int page) {
		// If we have only one page (portrait), or if is the first or the last page, we show only one page (centered).
		if( displayPages == 1 )//||  page == 0 || (displayPages == 2 && page == numPages/2)
		{
			gotoPage(page);
			return new PointF(pageWidth, pageHeight);
		}
		else
		{
			gotoPage(page);
			if( page == numPages -1 || page == 0) 
			{
				return new PointF(pageWidth*2, pageHeight);
			}
			//if( page == numPages -1 & numPages%2 != 0) // odd number pages
			//{
			//	return new PointF(pageWidth*2, pageHeight);
			//}
			float leftWidth = pageWidth;
			float leftHeight = pageHeight;
			gotoPage(page+1);
			float screenWidth = leftWidth + pageWidth;
			float screenHeight = Math.max(leftHeight, pageHeight);
			return new PointF(screenWidth, screenHeight);
			
		}
	}
	public static boolean onDestroy = false;
	public synchronized void onDestroy() {
		onDestroy = true;
		destroying();
		globals = 0;
	}
	
	public synchronized PointF getSinglePageSize(int page) {
		gotoPage(page);
		return new PointF(pageWidth, pageHeight);
	}
	
	public synchronized void drawPageSynchrinized(int page, Bitmap bitmap, int pageW,
			int pageH, int patchX, int patchY, int patchW, int patchH) {
		gotoPage(page);
		Log.d(TAG,"drawPageSynchrinized page:"+page);
		drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
	}
	
	public synchronized void drawSinglePage(int page, Bitmap bitmap, int pageW,
			int pageH) {

				drawPageSynchrinized(page, bitmap, pageW, pageH, 0, 0, pageW, pageH);
	}

	public synchronized Bitmap drawPage(final int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		
		// new , show 2 pages
		Canvas canvas = null;
		Bitmap bitmap =null;
		try
		{
			bitmap = Bitmap.createBitmap( patchW, patchH, Config.ARGB_8888);
			canvas = new Canvas( bitmap);
			canvas.drawColor(Color.TRANSPARENT);
			//Log.d("draw page", " " + page);
			//draw 1 page on portrait, or first page 
			if( displayPages == 1) // || page == 0
			{
				gotoPage(page);
				//Log.i("draw 1 page: "+ pageW+"; h : "+ pageH, "patchW: "+ patchW+" ; h: "+ patchH);
				drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
				return bitmap;
			} // else if have 2 pages mode, at last page, show 1 page
		//	else if( displayPages == 2 && ( page == numPages/2 && numPages%2 != 0)) // odd number pages
		//	{
		//		gotoPage(page*2 + 1);
		//		drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
		//		return bitmap;
		//	}
			else // draw 2 page
			{
				final int drawPage = (page == 0)? 0 : page*2 - 1;
				int leftPageW = pageW/2;
				int rightPageW = pageW - leftPageW;
				
				// if patch overlaps both bitmap ( left and right ) 
				// return width of overlap left bitmap part patch
				// or return full patch width if it's full inside left bitmap
				
				int leftBmWidth = Math.min( leftPageW, leftPageW - patchX);
				
				//set the right part of the patch width, as a rest of the patch
				leftBmWidth = (leftBmWidth < 0) ? 0 : leftBmWidth;
				
				int rightBmWidth = patchW - leftBmWidth;
				//Log.i("draw 2 page: "+ leftPageW+"; h : "+ pageH, "patchW: "+ patchW+" ; h: "+ patchH);
				if( drawPage == numPages -1 ) // odd number pages, && numPages%2 != 0
				{
					//draw only left page
					canvas.drawColor(Color.BLACK);
					if( leftBmWidth > 0)
					{
						
						Bitmap leftBm = Bitmap.createBitmap(leftBmWidth, patchH, Config.ARGB_8888);
						gotoPage(drawPage);
						drawPage(leftBm, leftPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, leftBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
						leftBm = null;
					}
				}
				else if( drawPage == 0)
				{
					//draw only right page
					
					canvas.drawColor(Color.BLACK);
					if( rightBmWidth > 0)
					{
						Bitmap rightBm = Bitmap.createBitmap(rightBmWidth, patchH, Config.ARGB_8888);
						gotoPage(drawPage);
						drawPage(rightBm, rightPageW, pageH,(leftBmWidth==0)? patchX - leftPageW: 0,
									patchY, rightBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(rightBm, leftBmWidth, 0, paint);
						rightBm.recycle();
						rightBm =null;
					}
				}
				else
				{
					// draw 2 pages : left and right
					
					Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
					if( leftBmWidth > 0) // left draw odd page number :0, 1, 3, 5, ...
					{
						Bitmap leftBm = Bitmap.createBitmap(leftBmWidth, patchH, Config.ARGB_8888);
						gotoPage(drawPage);
						drawPage(leftBm, leftPageW, pageH, patchX, patchY, leftBmWidth, patchH);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
						leftBm = null;
					}
					if( rightBmWidth > 0) // right draw page : 2, 4, 6, ...
					{
						Bitmap rightBm = Bitmap.createBitmap(rightBmWidth, patchH, Config.ARGB_8888);
						gotoPage(drawPage+1);
						drawPage(rightBm, rightPageW, pageH, (leftBmWidth==0)?patchX - leftPageW: 0, 
									patchY, rightBmWidth, patchH);
						canvas.drawBitmap(rightBm, leftBmWidth, 0, paint);

						rightBm.recycle();
						rightBm =null;
					}
				}
				return bitmap;
				
			}
		}
		catch( Exception e)
		{
			if( canvas != null )
				canvas.drawColor(Color.TRANSPARENT);
			return bitmap;
		}
		//end new
	}
	
	public synchronized Bitmap updatePage(BitmapHolder h, int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		Bitmap bm = null;
		Bitmap old_bm = h.getBm();

		if (old_bm == null || old_bm.isRecycled()) // for 2 pages
			return null;

		bm = old_bm.copy(Bitmap.Config.ARGB_8888, true);// old is false
		old_bm = null;
// old, 1 page
		//updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH);

/// new , for 2 pages
		
		Canvas canvas  = null;
		try
		{
			canvas = new Canvas(bm);
			canvas.drawColor(Color.TRANSPARENT);
			
			if( displayPages == 1)
			{
				updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH);
				return bm;
			}
			else
			{
				page = ( page == 0 ) ? 0: page*2 - 1;
				int leftPageW = pageW/2;
				int rightPageW = pageW - leftPageW;
				
				int leftBmWidth = Math.min(leftPageW, leftPageW - patchX);
				leftBmWidth = ( leftBmWidth < 0) ? 0: leftBmWidth;
				
				int rightBmWidth = patchW - leftBmWidth;
				
				if( page == numPages -1) // odd number pages,  && numPages%2 != 0
				{
					// draw left
					if( leftBmWidth > 0)
					{
						Bitmap leftBm = Bitmap.createBitmap(bm, 0, 0, leftBmWidth, patchH);
						updatePageInternal(leftBm, page, leftPageW, pageH,
								(leftBmWidth == 0) ? patchX - leftPageW : 0,
								patchY, leftBmWidth, patchH);
						Paint paint = new Paint( Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
						leftBm = null;
					}
				}
				else if( page == 0)
				{
					// draw right
					if( rightBmWidth > 0)
					{
						Bitmap rightBm = Bitmap.createBitmap(bm, leftBmWidth, 0, rightBmWidth, patchH);
						gotoPage(page);
						updatePageInternal(rightBm, page, rightPageW, pageH, 
										(leftBmWidth==0)?patchX - leftPageW:0,
										patchY, rightBmWidth, patchH);
						Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
						canvas.drawBitmap(rightBm, leftBmWidth, 0, paint);
						rightBm.recycle();
						rightBm = null;
					}
				}
				else
				{
					//draw 2 pages
					
					Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
					if( leftBmWidth > 0)
					{
						Bitmap leftBm = Bitmap.createBitmap(bm, 0, 0,
								(leftBmWidth< bm.getWidth())?leftBmWidth:bm.getWidth(), patchH);
						updatePageInternal(leftBm, page, leftPageW, pageH, patchX, patchY, leftBmWidth, patchH);
						canvas.drawBitmap(leftBm, 0, 0, paint);
						leftBm.recycle();
						leftBm = null;
					}
					
					if( rightBmWidth > 0)
					{
						Bitmap rightBm = Bitmap.createBitmap(bm, leftBmWidth, 0, rightBmWidth, patchH);
						updatePageInternal(rightBm, page, rightPageW, pageH,
								(leftBmWidth==0)?patchX - leftPageW:0, patchY, rightBmWidth, patchH);
						canvas.drawBitmap(rightBm, leftBmWidth, 0, paint);
				
						rightBm.recycle();
						rightBm = null;
					}
				}
				return bm;
			}
		}
		catch( Exception e)
		{
			if( canvas != null)
				canvas.drawColor(Color.TRANSPARENT);
			return bm;
		}
	}
	
	
	
	public synchronized int hitLinkPage(int page, float x, float y) {
		LinkInfo[] pageLinks = getPageLinks(page);
		for(LinkInfo pageLink: pageLinks) {
			if(pageLink instanceof LinkInfoInternal) {
				LinkInfoInternal internalLink = (LinkInfoInternal) pageLink;
				if(internalLink.rect.contains(x, y))
					return internalLink.pageNumber;
			}
		}
		return -1;
	}


	public synchronized LinkInfo[] getPageLinks(int page) {
		if(displayPages == 1)
			return getPageLinksInternal(page);
		LinkInfo[] leftPageLinkInfo = new LinkInfo[0];
		LinkInfo[] rightPageLinkInfo = new LinkInfo[0];
		LinkInfo[] combinedLinkInfo;
		int combinedSize = 0;
		int rightPage = page * 2;
		int leftPage = rightPage - 1;
		int count = countPages() * 2;
		if( leftPage > 0 ) {
			LinkInfo[] leftPageLinkInfoInternal = getPageLinksInternal(leftPage);
			if (null != leftPageLinkInfoInternal) {
				leftPageLinkInfo = leftPageLinkInfoInternal;
				combinedSize += leftPageLinkInfo.length;
			}
		}
		if( rightPage < count ) {
			LinkInfo[] rightPageLinkInfoInternal = getPageLinksInternal(rightPage);
			if (null != rightPageLinkInfoInternal) {
				rightPageLinkInfo = rightPageLinkInfoInternal;
				combinedSize += rightPageLinkInfo.length;
			}
		}
		
		combinedLinkInfo = new LinkInfo[combinedSize];
		for(int i = 0; i < leftPageLinkInfo.length; i++) {
			combinedLinkInfo[i] = leftPageLinkInfo[i];
		}
		
		LinkInfo temp;
		for(int i = 0, j = leftPageLinkInfo.length; i < rightPageLinkInfo.length; i++, j++) {
			temp = rightPageLinkInfo[i];
			temp.rect.left += pageWidth;
			temp.rect.right += pageWidth;
			combinedLinkInfo[j] = temp;
		}
		for (LinkInfo linkInfo: combinedLinkInfo) {
			if(linkInfo instanceof LinkInfoExternal)
				Log.d(TAG, "return " + ((LinkInfoExternal)linkInfo).url);
		}
		return combinedLinkInfo;
	}

//	public synchronized LinkInfo[] getPageURIs(int page) {
//		return getPageURIsInternal(page);
//	}

	public synchronized RectF[] searchPage(int page, String text) {
		if( page < 0 || page > numPages) // nhi add for search 2 page
			return null;
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized boolean hasOutline() {
		return hasOutlineInternal();
	}

	public synchronized OutlineItem[] getOutline() {
		return getOutlineInternal();
	}

	public synchronized boolean needsPassword() {
		return needsPasswordInternal();
	}

	public synchronized boolean authenticatePassword(String password) {
		return authenticatePasswordInternal(password);
	}
	
	public int getDisplayPages() {
		return displayPages;
	}
	
	private Config getBitmapConfig(){
		return Config.ARGB_8888;
	} 

	/**
	 * @return
	 */
	public int countDisplays() {
		int pages = countPages();
		if(pages % 2 == 0) {
			return pages / 2 + 1;
		} else 
			return pages / 2;
	}
	
	public void setDisplayPages(int pages) throws IllegalStateException {
		if(pages <=0 || pages > 2) {
			throw new IllegalStateException("MuPDFCore can only handle 1 or 2 pages per screen!");
		}
		displayPages = pages;
	}

	/**
	 * @return
	 */
	public int countSinglePages() {
		// TODO Auto-generated method stub
		return numPages;
	}
	public float getCorePageWidth()
	{
		return this.pageWidth;
	}
	public float getCorePageHeight()
	{
		return this.pageHeight;
	}
}

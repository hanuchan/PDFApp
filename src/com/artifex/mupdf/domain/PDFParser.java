/**
 * 
 */
package com.artifex.mupdf.domain;

import java.util.ArrayList;

import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.LinkInfoExternal;
import com.artifex.mupdf.MuPDFCore;



import android.net.Uri;
import android.util.SparseArray;


public class PDFParser {
	private com.artifex.mupdf.MuPDFCore mCore;
	private SparseArray<com.artifex.mupdf.LinkInfoExternal []> mLinkInfo = new SparseArray<LinkInfoExternal []>();
	private SparseArray<ArrayList<String>> mLinkUrls = new SparseArray<ArrayList<String>>();
	
	/**
	 * 
	 * @param pathToPDF - path within filesystem to PDF file
	 * @throws IllegalStateException - if some error occurs
	 */
	public PDFParser(String pathToPDF) throws IllegalStateException {
		try {
			mCore = new MuPDFCore(pathToPDF);
			parseLinkInfo();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}
	
	private void parseLinkInfo() {
		int nPages = mCore.countPages();
		for(int page = 0; page < nPages; page++) {
			LinkInfo [] mPageLinkInfo = mCore.getPageLinks(page);
			if(mPageLinkInfo != null && mPageLinkInfo.length > 0) {
				ArrayList<LinkInfoExternal> fixedLinkInfo = new ArrayList<LinkInfoExternal>();
				LinkInfoExternal current;
				for(int i = 0; i < mPageLinkInfo.length; i++) {
					if( !(mPageLinkInfo[i] instanceof LinkInfoExternal))
						continue;
					current = (LinkInfoExternal)mPageLinkInfo[i];
					if(current.url != null && current.url.startsWith("http://localhost")) {
						String path = Uri.parse(current.url).getPath();
						if(path.contains("_") && ( path.contains("jpg") || path.contains("png"))) {
							// ops... we have a slideshow here
							int mSlideshowCount = Integer
									.valueOf(path.split("_")[1].split("\\.")[0]);
							String mSlideshowPreffix = path.split("_")[0];
							String mSlideshowSuffix = path.split("_")[1].split("\\.")[1];
                            for (int j = 1; j <= mSlideshowCount; j++) {
                                LinkInfoExternal newLink = new LinkInfoExternal(current.rect.left, current.rect.top,
                                        current.rect.right, current.rect.bottom, "http://localhost" +
                                        mSlideshowPreffix + "_" + String.valueOf(j) + "." + mSlideshowSuffix);
                                fixedLinkInfo.add(newLink);
                            }
                        } else {
							fixedLinkInfo.add(current);
						}
					}else {
						fixedLinkInfo.add(current);
					}
				}
				
				mLinkInfo.put(page, fixedLinkInfo.toArray(new LinkInfoExternal[fixedLinkInfo.size()]));
			}
		}
	}
	
	/**
	 * get all URI links from PDF document
	 * @return SparseArray with all URLs by page. Each item in the array has LinkInfo[] array with links or null
	 * @see LinkInfo
	 */
	public SparseArray<LinkInfoExternal []> getLinkInfo() {
		return mLinkInfo;
	}
	
}

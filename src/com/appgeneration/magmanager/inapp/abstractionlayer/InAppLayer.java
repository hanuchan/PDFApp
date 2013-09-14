package com.appgeneration.magmanager.inapp.abstractionlayer;

import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;

public abstract class InAppLayer {
	
	public enum InAppSystem {
	    NONE, GOOGLE, SAMSUNG
	}
	
	protected ArrayList<InAppLayerItem> mItemList = null;
	private InAppLayerListener mInAppLayerListener = null;
	protected boolean mWereItemsLoaded = false;
	
	abstract public boolean initInAppPurchasing(Activity sourceActivity)throws InAppLayerException;
	public boolean isItemAlreadyBought(String itemId,StringBuilder itemPriceString) throws InAppLayerException
	{
		if (!mWereItemsLoaded) {
			throw new InAppLayerSystemNotReadyException();
		}
		if (mItemList == null) {
			throw new InAppLayerItemNotFoundException();
		}
		
		for (InAppLayerItem currentItem : mItemList) {
			if (currentItem.getItemId().equals(itemId) ) {
				if (currentItem.wasAlreadyBoughtByTheUser()) {
					return true;
				}
				else {
					//fill the item's price
					itemPriceString.append(currentItem.getPriceHumanReadable());
					
					return false;
				}
				
			}
		}
		throw new InAppLayerItemNotFoundException();
	}
	abstract public boolean startPurchaseProcessForItem(String itemId) throws InAppLayerException;
	abstract public boolean handleActivityResult(int _requestCode,int _resultCode,Intent _intent);
	abstract public void dispose();
	public InAppLayerListener getmInAppLayerListener() {
		return mInAppLayerListener;
	}
	public void setmInAppLayerListener(InAppLayerListener mInAppLayerListener) {
		this.mInAppLayerListener = mInAppLayerListener;
	}
}

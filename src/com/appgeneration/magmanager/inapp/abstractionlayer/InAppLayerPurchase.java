package com.appgeneration.magmanager.inapp.abstractionlayer;

public class InAppLayerPurchase {
	
	private boolean wasPurchaseCompletedByUser;
	
	public InAppLayerPurchase(boolean wasPurchaseCompletedByUser) {
		super();
		this.wasPurchaseCompletedByUser = wasPurchaseCompletedByUser;
	}
	
	public boolean wasPurchaseCompletedByUser() {
		return wasPurchaseCompletedByUser;
	}
	
}

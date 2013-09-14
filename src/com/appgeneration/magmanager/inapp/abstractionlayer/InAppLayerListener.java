package com.appgeneration.magmanager.inapp.abstractionlayer;

public interface InAppLayerListener {
	public void onInAppLayerSucessfullyStarted();
	public void onInAppLayerFailedToStart();
	public void onInAppLayerItemsAllLoaded();
	public void onInAppLayerItemsStatusChanged();
}

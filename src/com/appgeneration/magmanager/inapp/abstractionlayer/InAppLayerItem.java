package com.appgeneration.magmanager.inapp.abstractionlayer;

import com.appgeneration.magmanager.model.ItemStatus;
import com.sec.android.iap.sample.vo.ItemVO;


public class InAppLayerItem {

	private String itemId;
	private String priceHumanReadable;
	private String description;
	private String title;
	private ItemStatus itemStatus;
	
	/**
	 * Generates an abstract item from a samsung item
	 * @param samsungItem
	 */
	public InAppLayerItem(ItemVO samsungItem)
	{
		itemId = samsungItem.getItemId();
		title = samsungItem.getItemName();
		priceHumanReadable = samsungItem.getItemPriceString();
		description = samsungItem.getItemDesc();
	}
	
	public InAppLayerItem(String itemId, String priceHumanReadable,
			String description, String title) {
		super();
		this.itemId = itemId;
		this.priceHumanReadable = priceHumanReadable;
		this.description = description;
		this.title = title;
	}

	public InAppLayerItem(String itemId, String priceHumanReadable,
			String description, String title, ItemStatus itemStatus) {
		super();
		this.itemId = itemId;
		this.priceHumanReadable = priceHumanReadable;
		this.description = description;
		this.title = title;
		this.setItemStatus(itemStatus);
	}

	public String getItemId() {
		return itemId;
	}

	public String getPriceHumanReadable() {
		return priceHumanReadable;
	}

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}
	
	public boolean wasAlreadyBoughtByTheUser() {
		return getItemStatus() == ItemStatus.BOUGHT;
	}

	public ItemStatus getItemStatus() {
		return itemStatus;
	}

	public void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}
}

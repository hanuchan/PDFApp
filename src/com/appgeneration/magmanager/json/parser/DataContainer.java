/**
 * 
 */
package com.appgeneration.magmanager.json.parser;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.appgeneration.magmanager.model.DataStatus;
import com.appgeneration.magmanager.model.Issue;
import com.appgeneration.magmanager.model.Magazine;
import com.appgeneration.magmanager.model.Subscription;

/**
 * @author miguelferreira This class is responsible for creating model objects
 *         from a given json that shall meet the needed requirements
 */
public class DataContainer {

	// container status
	private DataStatus containerStatus = DataStatus.OK;

	private Magazine magazine;
	private List<Issue> issues;
	private List<Subscription> subscriptions;

	private static final String MAGAZINE_KEY = "magazine";
	private static final String MAGAZINE_TITLE_KEY = "title";
	private static final String MAGAZINE_BANNERS_KEY = "banners";
	private static final String MAGAZINE_BANNERS_LEFT_KEY = "left";
	private static final String MAGAZINE_BANNERS_RIGHT_KEY = "right";
	private static final String MAGAZINE_BANNERS_BOTTOM_KEY = "bottom";
	private static final String MAGAZINE_BANNERS_TOP_KEY = "top";

	private static final String ISSUES_KEY = "issues";
	private static final String ISSUES_NAME_KEY = "name";
	private static final String ISSUES_ID_KEY = "id";
	private static final String ISSUES_TITLE_KEY = "title";
	private static final String ISSUES_COVER_KEY = "cover";
	private static final String ISSUES_DOCUMENT_KEY = "document";
	private static final String ISSUES_PUBLICATION_DATE_KEY = "publication_date";
	private static final String ISSUES_PRICE_KEY = "price";
	private static final String ISSUES_PREVIEW_TEXT_KEY = "preview_text";
	private static final String ISSUES_PREVIEW_COVER_KEY = "preview_cover";

	private static final String SUBSCRIPTIONS_KEY = "subscriptions";
	private static final String SUBSCRIPTIONS_TITLE_KEY = "title";
	private static final String SUBSCRIPTIONS_DURATION_KEY = "duration";
	private static final String SUBSCRIPTIONS_COVER_KEY = "cover";
	private static final String SUBSCRIPTIONS_PRICE_KEY = "price";

	/**
	 * @return the magazine
	 */
	public Magazine getMagazine() {
		return magazine;
	}

	/**
	 * @return the issues
	 */
	public List<Issue> getIssues() {
		return issues;
	}

	/**
	 * @return the subscriptions
	 */
	public List<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public DataContainer() {
		
		this.containerStatus = DataStatus.INTERNET_PROBLEM;
	}
	
	public DataContainer(DataStatus status) {
		if (status == DataStatus.OK) {
			throw new AssertionError(
					"Data container cant be initialized with OK status, this constructor is meant only for connection problems");
		}
		this.containerStatus = status;
	}

	public DataContainer(JSONObject jsonObject) throws JSONException {

		// magazine

		if (!jsonObject.has(MAGAZINE_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_KEY + " from "
					+ jsonObject);
		}

		JSONObject magazineObject = jsonObject.getJSONObject(MAGAZINE_KEY);
		if (!magazineObject.has(MAGAZINE_TITLE_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_TITLE_KEY + " from "
					+ magazineObject);
		}
		if (!magazineObject.has(MAGAZINE_BANNERS_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_BANNERS_KEY
					+ " from " + magazineObject);
		}

		JSONObject magazineBannersJsonObject = magazineObject
				.getJSONObject(MAGAZINE_BANNERS_KEY);
		if (!magazineBannersJsonObject.has(MAGAZINE_BANNERS_LEFT_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_BANNERS_LEFT_KEY
					+ " from " + magazineBannersJsonObject);
		}
		if (!magazineBannersJsonObject.has(MAGAZINE_BANNERS_RIGHT_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_BANNERS_RIGHT_KEY
					+ " from " + magazineBannersJsonObject);
		}
		if (!magazineBannersJsonObject.has(MAGAZINE_BANNERS_BOTTOM_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_BANNERS_BOTTOM_KEY
					+ " from " + magazineBannersJsonObject);
		}
		if (!magazineBannersJsonObject.has(MAGAZINE_BANNERS_TOP_KEY)) {
			throw new JSONException("Missing " + MAGAZINE_BANNERS_KEY
					+ " from " + magazineBannersJsonObject);
		}

		magazine = new Magazine(
				null,
				magazineObject.getString(MAGAZINE_TITLE_KEY),
				magazineBannersJsonObject.getString(MAGAZINE_BANNERS_LEFT_KEY),
				magazineBannersJsonObject.getString(MAGAZINE_BANNERS_RIGHT_KEY),
				magazineBannersJsonObject.getString(MAGAZINE_BANNERS_TOP_KEY),
				magazineBannersJsonObject
						.getString(MAGAZINE_BANNERS_BOTTOM_KEY));

		// issues
		if (!jsonObject.has(ISSUES_KEY)) {
			throw new JSONException("Missing " + ISSUES_KEY + " from "
					+ jsonObject);
		}

		issues = new ArrayList<Issue>();

		JSONArray issuesJsonArray = jsonObject.getJSONArray(ISSUES_KEY);

		for (int i = 0; i < issuesJsonArray.length(); i++) {
			JSONObject issueJsonObject = (JSONObject) issuesJsonArray.get(i);

			if (!issueJsonObject.has(ISSUES_NAME_KEY)) {
				throw new JSONException("Missing " + ISSUES_NAME_KEY + " from "
						+ issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_ID_KEY)) {
				throw new JSONException("Missing " + ISSUES_ID_KEY + " from "
						+ issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_TITLE_KEY)) {
				throw new JSONException("Missing " + ISSUES_TITLE_KEY
						+ " from " + issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_COVER_KEY)) {
				throw new JSONException("Missing " + ISSUES_COVER_KEY
						+ " from " + issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_DOCUMENT_KEY)) {
				throw new JSONException("Missing " + ISSUES_DOCUMENT_KEY
						+ " from " + issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_PUBLICATION_DATE_KEY)) {
				throw new JSONException("Missing "
						+ ISSUES_PUBLICATION_DATE_KEY + " from "
						+ issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_PRICE_KEY)) {
				throw new JSONException("Missing " + ISSUES_PRICE_KEY
						+ " from " + issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_PREVIEW_TEXT_KEY)) {
				throw new JSONException("Missing " + ISSUES_PREVIEW_TEXT_KEY
						+ " from " + issueJsonObject);
			}

			if (!issueJsonObject.has(ISSUES_PREVIEW_COVER_KEY)) {
				throw new JSONException("Missing " + ISSUES_PREVIEW_COVER_KEY
						+ " from " + issueJsonObject);
			}

			// TODO handle publication date correctly
			Issue issue = new Issue(Long.decode(issueJsonObject
					.getString(ISSUES_ID_KEY)),
					issueJsonObject.getString(ISSUES_NAME_KEY),
					issueJsonObject.getString(ISSUES_TITLE_KEY),
					issueJsonObject.getString(ISSUES_COVER_KEY),
					issueJsonObject.getString(ISSUES_DOCUMENT_KEY), null,
					issueJsonObject.getString(ISSUES_PRICE_KEY),
					issueJsonObject.getString(ISSUES_PREVIEW_TEXT_KEY),
					issueJsonObject.getString(ISSUES_PREVIEW_COVER_KEY));

			issues.add(issue);

		}

		// subscriptions

		subscriptions = new ArrayList<Subscription>();

		JSONArray subscriptionsJsonArray = jsonObject
				.optJSONArray(SUBSCRIPTIONS_KEY);
		if (subscriptionsJsonArray != null) {
			for (int i = 0; i < subscriptionsJsonArray.length(); i++) {
				JSONObject subscriptionJsonObject = (JSONObject) subscriptionsJsonArray
						.get(i);

				if (!subscriptionJsonObject.has(SUBSCRIPTIONS_COVER_KEY)) {
					throw new JSONException("Missing "
							+ SUBSCRIPTIONS_COVER_KEY + " from "
							+ subscriptionJsonObject);
				}

				if (!subscriptionJsonObject.has(SUBSCRIPTIONS_DURATION_KEY)) {
					throw new JSONException("Missing "
							+ SUBSCRIPTIONS_DURATION_KEY + " from "
							+ subscriptionJsonObject);
				}

				if (!subscriptionJsonObject.has(SUBSCRIPTIONS_PRICE_KEY)) {
					throw new JSONException("Missing "
							+ SUBSCRIPTIONS_PRICE_KEY + " from "
							+ subscriptionJsonObject);
				}

				if (!subscriptionJsonObject.has(SUBSCRIPTIONS_TITLE_KEY)) {
					throw new JSONException("Missing "
							+ SUBSCRIPTIONS_TITLE_KEY + " from "
							+ subscriptionJsonObject);
				}

				// TODO handle duration
				Subscription subscription = new Subscription(null,
						subscriptionJsonObject
								.getString(SUBSCRIPTIONS_TITLE_KEY), null,
						subscriptionJsonObject
								.getString(SUBSCRIPTIONS_COVER_KEY),
						subscriptionJsonObject
								.getString(SUBSCRIPTIONS_PRICE_KEY));
				subscriptions.add(subscription);
			}
		}

	}

	/**
	 * @param containerStatus
	 *            the containerStatus to set
	 */
	public void setContainerStatus(DataStatus containerStatus) {
		this.containerStatus = containerStatus;
	}

	public boolean isContainerStatusOK() {
		return containerStatus == DataStatus.OK;
	}

	public boolean wasTheProblemDueToTheInternet() {
		return containerStatus == DataStatus.INTERNET_PROBLEM;
	}

	public boolean wasTheProblemDueToTServerResponse() {
		return containerStatus == DataStatus.SERVER_RESPONSE_PROBLEM;
	}

	public DataStatus getDataStatus() {
		return containerStatus;
	}
}

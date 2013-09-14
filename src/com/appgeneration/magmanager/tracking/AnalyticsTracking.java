package com.appgeneration.magmanager.tracking;

import android.content.Context;

import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.model.Issue;
import com.google.analytics.tracking.android.EasyTracker;

public class AnalyticsTracking {

	// categories
	private static final String CATEGORY_UI = "UI";
	private static final String CATEGORY_SYSTEM = "System";
	private static final String CATEGORY_ISSUE = "Issue";

	// actions
	private static final String ACTION_ISSUE_READ = "issueRead";
	private static final String ACTION_ISSUE_DOWNLOAD = "issueDownload";
	private static final String ACTION_ISSUE_DELETE = "issueDelete";
	private static final String ACTION_ISSUE_PREVIEW = "issuePreview";

	private static final String ACTION_UI_PRESS = "buttonPress";

	// labels
	private static final String LABEL_UI_INFO = "Info";
	private static final String LABEL_UI_REFRESH = "Refresh";

	private enum TrackingState {
		TrackingStateUnverified, TrackingStateUnavailable, TrackingStateAvailable
	}

	private static TrackingState trackingState = TrackingState.TrackingStateUnverified;

	public static void trackIssueRead(Issue issue) {
		EasyTracker.getTracker().sendEvent(CATEGORY_ISSUE, ACTION_ISSUE_READ,
				issue.getTitle(), (long) 1);
	}

	public static void trackIssueDownload(Issue issue) {
		EasyTracker.getTracker().sendEvent(CATEGORY_ISSUE,
				ACTION_ISSUE_DOWNLOAD, issue.getTitle(), (long) 1);
	}

	public static void trackIssueDelete(Issue issue) {
		EasyTracker.getTracker().sendEvent(CATEGORY_ISSUE, ACTION_ISSUE_DELETE,
				issue.getTitle(), (long) 1);
	}

	public static void trackIssuePreview(Issue issue) {
		EasyTracker.getTracker().sendEvent(CATEGORY_ISSUE,
				ACTION_ISSUE_PREVIEW, issue.getTitle(), (long) 1);
	}

	// UI

	public static void trackClickInfo() {
		EasyTracker.getTracker().sendEvent(CATEGORY_UI, ACTION_UI_PRESS,
				LABEL_UI_INFO, (long) 1);
	}

	public static void trackClickRefresh() {
		EasyTracker.getTracker().sendEvent(CATEGORY_UI, ACTION_UI_PRESS,
				LABEL_UI_REFRESH, (long) 1);
	}
}

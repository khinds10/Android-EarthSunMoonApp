package com.kevinhinds.riseandset.widget;

import java.util.LinkedList;
import java.util.Queue;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.widget.RemoteViews;

/**
 * Background service to build any requested widget updates.
 */
public class UpdateService extends Service implements Runnable {

	public static class AppWidgets implements BaseColumns {
		public static final String AUTHORITY = "com.kevinhinds.riseandset";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/appwidgets");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/appwidget";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/appwidget";
	}

	/**
	 * Lock used when maintaining queue of requested updates.
	 */
	private static Object sLock = new Object();

	/**
	 * Flag if there is an update thread already running. We only launch a new thread if one isn't
	 * already running.
	 */
	private static boolean sThreadRunning = false;

	/**
	 * Internal queue of requested widget updates. You <b>must</b> access through
	 * {@link #requestUpdate(int[])} or {@link #getNextUpdate()} to make sure your access is
	 * correctly synchronized.
	 */
	private static Queue<Integer> sAppWidgetIds = new LinkedList<Integer>();

	/**
	 * Request updates for the given widgets. Will only queue them up, you are still responsible for
	 * starting a processing thread if needed, usually by starting the parent service.
	 */
	public static void requestUpdate(int[] appWidgetIds) {
		synchronized (sLock) {
			for (int appWidgetId : appWidgetIds) {
				sAppWidgetIds.add(appWidgetId);
			}
		}
	}

	/**
	 * Peek if we have more updates to perform. This method is special because it assumes you're
	 * calling from the update thread, and that you will terminate if no updates remain. (It
	 * atomically resets {@link #sThreadRunning} when none remain to prevent race conditions.)
	 */
	private static boolean hasMoreUpdates() {
		synchronized (sLock) {
			boolean hasMore = !sAppWidgetIds.isEmpty();
			if (!hasMore) {
				sThreadRunning = false;
			}
			return hasMore;
		}
	}

	/**
	 * Poll the next widget update in the queue.
	 */
	private static int getNextUpdate() {
		synchronized (sLock) {
			if (sAppWidgetIds.peek() == null) {
				return AppWidgetManager.INVALID_APPWIDGET_ID;
			} else {
				return sAppWidgetIds.poll();
			}
		}
	}

	/**
	 * Start this service, creating a background processing thread, if not already running. If
	 * started with {@link #ACTION_UPDATE_ALL}, will automatically add all widgets to the requested
	 * update queue.
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		requestUpdate(manager.getAppWidgetIds(new ComponentName(this, MoonWidget.class)));

		// Only start processing thread if not already running
		synchronized (sLock) {
			if (!sThreadRunning) {
				sThreadRunning = true;
				new Thread(this).start();
			}
		}
	}

	/**
	 * Main thread for running through any requested widget updates until none remain. Also sets
	 * alarm to perform next update.
	 */
	public void run() {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

		while (hasMoreUpdates()) {
			int appWidgetId = getNextUpdate();
			Uri appWidgetUri = ContentUris.withAppendedId(AppWidgets.CONTENT_URI, appWidgetId);

			// Process this update through the correct provider
			AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
			String providerName = info.provider.getClassName();
			RemoteViews updateViews = null;

			// based on providerName update the correct widget
			if (providerName.equals(MoonWidget.class.getName())) {
				updateViews = MoonWidget.buildUpdate(this, appWidgetUri);
			} else if (providerName.equals(SunWidget.class.getName())) {
				updateViews = SunWidget.buildUpdate(this, appWidgetUri);
			}

			// Push this update to surface
			if (updateViews != null) {
				appWidgetManager.updateAppWidget(appWidgetId, updateViews);
			}
		}

		// No updates remaining, so stop service
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
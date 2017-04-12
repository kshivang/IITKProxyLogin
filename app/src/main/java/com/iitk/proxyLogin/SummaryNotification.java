package com.iitk.proxyLogin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

import static android.app.Notification.*;

/**
 * Created by kshivang on 12/04/17.
 *
 */

public class SummaryNotification {
    private static final String TAG;
    private static int notificationId;
    private boolean alert;
    Context context;
    ArrayList<String> lines;
    Builder mBuilder;
    private String message;
    private Bitmap picture;
    private String summaryMessage;
    private String tag;

    static {
        TAG = SummaryNotification.class.getSimpleName();
        notificationId = 1001;
    }

    public SummaryNotification(Context context, String title, String message, String ticker,
                               int iconResource, String tag) {
        this.alert = false;
        this.picture = null;
        this.tag = tag;
        this.context = context;
        this.message = message;
        this.lines = new ArrayList();
        this.mBuilder = new Builder(context).setContentTitle(title)
                .setContentText(message).setSmallIcon(iconResource)
                .setTicker(ticker).setAutoCancel(true);
        if (message != null) {
            this.lines.add(message);
        }
    }

    public void setContentIntent(PendingIntent intent) {
        this.mBuilder.setContentIntent(intent);
    }

    public void addAction(int iconResource, String actionName, PendingIntent intent) {
        this.mBuilder.addAction(iconResource, actionName, intent);
    }

    public void updateTitle(String title) {
        this.mBuilder.setContentTitle(title);
    }

    public void setNotificationId(int notificationId) {
        notificationId = notificationId;
    }

    public void show() {
        if (this.picture == null) {
            InboxStyle inboxStyle = new InboxStyle();
            Iterator it = this.lines.iterator();
            while (it.hasNext()) {
                inboxStyle.addLine((String) it.next());
            }
            if (this.summaryMessage != null) {
                inboxStyle.setSummaryText(this.summaryMessage);
            }
            this.mBuilder.setStyle(inboxStyle);
        } else {
            BigPictureStyle bigPictureStyle = new BigPictureStyle();
            bigPictureStyle.bigPicture(this.picture);
            if (this.summaryMessage != null) {
                bigPictureStyle.setSummaryText(this.summaryMessage);
            }
            this.mBuilder.setStyle(bigPictureStyle);
        }
        Notification notification = null;
        try {
            notification = this.mBuilder.build();
        } catch (NullPointerException ex) {
            Log.e(TAG, "Got NPE inboxStyle lines " + this.lines, ex);
        }
        if (notification != null) {
            if (this.alert) {
                notification.defaults |= -1;
            }
            ((NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(this.tag, notificationId, notification);
        }
    }

    public static void cancelNotification(Context context, String tag, int id) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(tag, id);
    }

    public void setLargeIcon(Bitmap icon) {
        this.mBuilder.setLargeIcon(icon);
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.mBuilder.setContentText(message);
    }

    public void setProgress(int progress, int max) {
        this.mBuilder.setProgress(max, progress, false);
    }

    public void addLineToBigView(String line) {
        this.lines.add(line);
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }

    public void setContentInfo(String contentInfo) {
        this.mBuilder.setContentInfo(contentInfo);
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }
}

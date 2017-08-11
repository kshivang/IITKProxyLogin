package com.iitk.proxyLogin.notfication

import android.app.Notification
import android.app.Notification.*
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.util.*

/**
 * Created by kshivang on 12/04/17.

 */

class SummaryNotification(var context: Context, title: String, private val message: String?, ticker: String,
                          iconResource: Int, private var tag: String?) {
    private var alert: Boolean = false
    var lines: ArrayList<String>
    var mBuilder: Builder
    private var picture: Bitmap? = null
    private var summaryMessage: String? = null

    init {
        this.alert = false
        this.picture = null
        this.lines = ArrayList()
        this.mBuilder = Builder(context).setContentTitle(title)
                .setContentText(message).setSmallIcon(iconResource)
                .setTicker(ticker).setAutoCancel(true)
        if (message != null) {
            this.lines.add(message)
        }
    }

    fun setContentIntent(intent: PendingIntent) {
        this.mBuilder.setContentIntent(intent)
    }

    fun addAction(iconResource: Int, actionName: String, intent: PendingIntent) {
        this.mBuilder.addAction(iconResource, actionName, intent)
    }

    fun updateTitle(title: String) {
        this.mBuilder.setContentTitle(title)
    }

    fun setNotificationId(notificationId: Int) {
        var notificationId = notificationId
        notificationId = notificationId
    }

    fun show() {
        if (this.picture == null) {
            val inboxStyle = InboxStyle()
            val it = this.lines.iterator()
            while (it.hasNext()) {
                inboxStyle.addLine(it.next())
            }
            if (this.summaryMessage != null) {
                inboxStyle.setSummaryText(this.summaryMessage)
            }
            this.mBuilder.setStyle(inboxStyle)
        } else {
            val bigPictureStyle = BigPictureStyle()
            bigPictureStyle.bigPicture(this.picture)
            if (this.summaryMessage != null) {
                bigPictureStyle.setSummaryText(this.summaryMessage)
            }
            this.mBuilder.setStyle(bigPictureStyle)
        }
        var notification: Notification? = null
        try {
            notification = this.mBuilder.build()
        } catch (ex: NullPointerException) {
            Log.e(TAG, "Got NPE inboxStyle lines " + this.lines, ex)
        }

        if (notification != null) {
            if (this.alert) {
                notification.defaults = notification.defaults or -1
            }
            (this.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(this.tag, notificationId, notification)
        }
    }

    fun setLargeIcon(icon: Bitmap) {
        this.mBuilder.setLargeIcon(icon)
    }

    fun getMessage(): String? {
        return this.message
    }

    fun setMessage(message: String) {
        this.mBuilder.setContentText(message)
    }

    fun setProgress(progress: Int, max: Int) {
        this.mBuilder.setProgress(max, progress, false)
    }

    fun addLineToBigView(line: String) {
        this.lines.add(line)
    }

    fun setSummaryMessage(summaryMessage: String) {
        this.summaryMessage = summaryMessage
    }

    fun setContentInfo(contentInfo: String) {
        this.mBuilder.setContentInfo(contentInfo)
    }

    fun setAlert(alert: Boolean) {
        this.alert = alert
    }

    fun setTag(tag: String) {
        this.tag = tag
    }

    fun setPicture(picture: Bitmap) {
        this.picture = picture
    }

    companion object {
        private val TAG: String
        private var notificationId: Int = 0

        init {
            TAG = SummaryNotification::class.java.simpleName
            notificationId = 1001
        }

        fun cancelNotification(context: Context, tag: String, id: Int) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(tag, id)
        }
    }
}

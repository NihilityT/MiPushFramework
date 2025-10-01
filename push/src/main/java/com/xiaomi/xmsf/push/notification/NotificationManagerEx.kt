package com.nihility.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import com.elvishew.xlog.XLog

object NotificationManagerEx {
    private const val TAG = "NotificationManagerEx"

    private lateinit var notificationManager: NotificationManager

    @JvmField
    var isHooked: Boolean = false

    @JvmStatic
    fun init(context: Context) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun notify(
        packageName: String,
        tag: String?, id: Int, notification: Notification
    ) {
        XLog.d(TAG, "notify() called with: packageName = $packageName, tag = $tag, id = $id, notification = $notification")
        notificationManager.notify(tag, id, notification)
    }

    fun cancel(
        packageName: String,
        tag: String?, id: Int
    ) {
        XLog.d(TAG, "cancel() called with: packageName = $packageName, tag = $tag, id = $id")
        notificationManager.cancel(tag, id)
    }

    fun createNotificationChannels(
        packageName: String,
        channels: List<NotificationChannel?>
    ) {
        XLog.d(TAG, "createNotificationChannels() called with: packageName = $packageName, channels = $channels")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannels(channels)
        }
    }

    fun getNotificationChannel(
        packageName: String,
        channelId: String?
    ): NotificationChannel? {
        XLog.d(TAG, "createNotificationChannels() called with: packageName = $packageName, channelId = $channelId")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(channelId)
        } else {
            null
        }
    }

    fun getNotificationChannels(
        packageName: String
    ): List<NotificationChannel?>? {
        XLog.d(TAG, "getNotificationChannels() called with: packageName = $packageName")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannels()
        } else {
            emptyList()
        }
    }

    fun deleteNotificationChannel(
        packageName: String,
        channelId: String?
    ) {
        XLog.d(TAG, "deleteNotificationChannel() called with: packageName = $packageName, channelId = $channelId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelId)
        }
    }


    fun createNotificationChannelGroups(
        packageName: String,
        groups: List<NotificationChannelGroup?>
    ) {
        XLog.d(TAG, "createNotificationChannelGroups() called with: packageName = $packageName, groups = $groups")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroups(groups)
        }
    }

    fun getNotificationChannelGroup(
        packageName: String,
        groupId: String?
    ): NotificationChannelGroup? {
        XLog.d(TAG, "getNotificationChannelGroup() called with: packageName = $packageName, groupId = $groupId")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TODO("compile error")
            //notificationManager.getNotificationChannelGroup(groupId)
        } else {
            null
        }
    }

    fun getNotificationChannelGroups(
        packageName: String
    ): List<NotificationChannelGroup?>? {
        XLog.d(TAG, "getNotificationChannelGroups() called with: packageName = $packageName")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannelGroups()
        } else {
            emptyList()
        }
    }

    fun deleteNotificationChannelGroup(
        packageName: String,
        groupId: String?
    ) {
        XLog.d(TAG, "deleteNotificationChannelGroup() called with: packageName = $packageName, groupId = $groupId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannelGroup(groupId)
        }
    }

    fun areNotificationsEnabled(
        packageName: String
    ): Boolean {
        XLog.d(TAG, "areNotificationsEnabled() called with: packageName = $packageName")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    fun getActiveNotifications(
        packageName: String
    ): Array<StatusBarNotification?>? {
        XLog.d(TAG, "getActiveNotifications() called with: packageName = $packageName")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.getActiveNotifications()
        } else {
            emptyArray()
        }
    }

}
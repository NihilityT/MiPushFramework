package com.nihility.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.os.Build;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;

import com.nihility.Global;
import com.xiaomi.xmsf.R;

public class ForegroundHelper {
    public static final String CHANNEL_STATUS = "status";
    public static final int NOTIFICATION_ALIVE_ID = 1;
    private final Service service;

    public ForegroundHelper(Service service) {
        this.service = service;
    }

    public void startForeground() {
        createNotificationGroupForPushStatus();
        if (Global.ConfigCenter().isStartForegroundService()) {
            showForegroundNotificationToKeepAlive();
        } else {
            stopForegroundNotification();
        }
    }

    public void stopForegroundNotification() {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE);
    }

    void showForegroundNotificationToKeepAlive() {
        //if (ConfigCenter.getInstance().foregroundNotification || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        {
            Notification notification = new NotificationCompat.Builder(service,
                    CHANNEL_STATUS)
                    .setContentTitle(service.getString(R.string.notification_alive))
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true)
                    .setShowWhen(true)
                    .build();

            service.startForeground(NOTIFICATION_ALIVE_ID, notification);
        }
    }

    void createNotificationGroupForPushStatus() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(service.getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String groupId = "status_group";
            NotificationChannelGroupCompat.Builder group =
                    new NotificationChannelGroupCompat.Builder(groupId)
                            .setName(CHANNEL_STATUS);
            manager.createNotificationChannelGroup(group.build());

            NotificationChannelCompat.Builder channel = new NotificationChannelCompat.Builder(
                    CHANNEL_STATUS, NotificationManager.IMPORTANCE_MIN)
                    .setName(service.getString(R.string.notification_category_alive)).setGroup(groupId);
            manager.createNotificationChannel(channel.build());
        }
    }
}
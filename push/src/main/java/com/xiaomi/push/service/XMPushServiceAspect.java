package com.xiaomi.push.service;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.P;
import static top.trumeet.common.Constants.TAG_CONDOM;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.oasisfeng.condom.CondomContext;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.network.Fallback;
import com.xiaomi.network.HostManager;
import com.xiaomi.push.revival.NotificationRevival;
import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.smack.packet.Message;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.control.XMOutbound;
import com.xiaomi.xmsf.utils.ConfigCenter;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.apache.thrift.TBase;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Map;

import top.trumeet.common.Constants;
import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.type.RegistrationType;
import top.trumeet.mipush.provider.register.RegisteredApplication;

/**
 * @author Trumeet
 * @date 2018/1/19
 * <p>
 * PayLoad 主要在 {@link com.xiaomi.mipush.sdk.PushServiceClient#sendMessage(TBase, ActionType, PushMetaInfo)} 等重载方法中处理。
 * 具体转换： {@link com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils#convertByteArrayToThriftObject(TBase, byte[])} 中。
 * <p>
 * 0. 万恶之源
 * 主要 intent 处理是在 {@link XMPushService#handleIntent(Intent)}
 * <p>
 * 1. 向服务器发送
 * <strong>向服务器</strong>发送消息是 {@link XMPushService#sendMessage(Intent)}（解析 Intent -> Package）
 * 对应服务启动方：{@link com.xiaomi.push.service.ServiceClient#sendMessage(Message, boolean)}
 * Action: {@link PushConstants#ACTION_SEND_MESSAGE}
 * Extras: {@link PushConstants#MIPUSH_EXTRA_PAYLOAD}, {@link PushConstants#MIPUSH_EXTRA_MESSAGE_CACHE}, {@link PushConstants#MIPUSH_EXTRA_APP_PACKAGE}
 * <p>
 * {@link XMPushService#sendMessage(String, byte[], boolean)}（解析 payload (byte[]）-> 未知）
 * 对应启动方： {@link com.xiaomi.mipush.sdk.PushServiceClient#sendMessage(TBase, ActionType, PushMetaInfo)}
 * Action: {@link PushConstants#MIPUSH_ACTION_SEND_MESSAGE}
 * <p>
 * {@link XMPushService#sendMessages(Intent)}（和第一个一样，批量）
 * 对应启动方： {@link com.xiaomi.push.service.ServiceClient#batchSendMessage(Message[], boolean)}
 * Action: {@link PushConstants#ACTION_BATCH_SEND_MESSAGE}
 * <p>
 * 由此推测，XMPushService 具有<strong>向服务器</strong>发送消息的功能（三种途径），其控制方是 {@link com.xiaomi.push.service.ServiceClient} 和
 * {@link com.xiaomi.mipush.sdk.PushServiceClient}。
 * <p>
 * 看具体的 Job（{@link com.xiaomi.push.service.SendMessageJob}， {@link com.xiaomi.push.service.BatchSendMessageJob}），
 * 都是 {@link XMPushService} 收到 Intent 之后，启动 job，然后 job 回掉 XMPushService 进行 send packet，然后应该是发送给服务器了..
 * 所以这里的发送消息是通过 {@link XMPushService} 向服务器发送 Packet / Blob。
 * <p>
 * 2. 本地发送
 * 根据发送消息中出错的 Stacktrace，我们可以轻松找出发送消息的下游处理。
 * 首先，在 {@link XMPushService#connectBySlim} 连接时，注册了包监听器 {@link XMPushService#mPacketListener}。
 * 它在监听到 Blob 和 Packet 后启动 {@link BlobReceiveJob} 和 {@link PacketReceiveJob}。
 * 这两个 Job 都将数据交给 {@link PacketSync} 处理。但两者最后都经过检测 / 处理，将数据交给 {@link ClientEventDispatcher#notifyPacketArrival} 处理。
 * <p>
 * 3. 其他
 * 还有其他处理也在这里，比如注册 App（ {@link PushConstants#MIPUSH_ACTION_REGISTER_APP}）等，暂未支持。
 * <p>
 * 监听解决方案：
 * 所有包都会经过 {@link com.xiaomi.smack.PacketListener}，官方有一个处理器（C00621），持有在非静态 Field mPacketListener 中。
 * 所以我们只需创造一个自己的 {@link ClientEventDispatcher}，重写相关方法，即可完成处理。
 * 这种方式相比自己反射修改 {@link com.xiaomi.smack.PacketListener}，更优雅（无需反射，同时小米为我们提供了 {@link com.xiaomi.push.service.XMPushService#createClientEventDispatcher()} 方法），
 * 同时还能通过官方提供的处理逻辑（{@link PacketSync}），直接处理消息通知包。
 */

@Aspect
public class XMPushServiceAspect {
    private static final String TAG = XMPushServiceAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();
    public static final String CHANNEL_STATUS = "status";
    public static final int NOTIFICATION_ALIVE_ID = 1;
    public static XMPushService xmPushService;

    public static String connectionStatus;
    @RequiresApi(N) private NotificationRevival mNotificationRevival;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), PushConstants.ACTION_RESET_CONNECTION)) {
                xmPushService.executeJob(new XMPushService.Job(XMPushService.Job.TYPE_RESET_CONNECT) {
                    @Override
                    public String getDesc() {
                        return "reset connection";
                    }

                    @Override
                    public void process() {
                        Fallback fallback = HostManager.getInstance().getFallbacksByHost(ConnectionConfiguration.getXmppServerHost(), false);
                        JavaCalls.setField(fallback, "timestamp", 0);
                        HostManager.getInstance().getFallbacksByHost(ConnectionConfiguration.getXmppServerHost(), true);
                        xmPushService.disconnect(11, null);
                        xmPushService.scheduleConnect(true);
                    }
                });
            }
            sendSetConnectionStatus();
        }
    };

    @Around("execution(* com.xiaomi.push.service.XMPushService.onCreate(..)) && this(pushService)")
    public void onCreate(final ProceedingJoinPoint joinPoint, XMPushService pushService) throws Throwable {
        logger.d(joinPoint.getSignature());
        Context mBase = pushService.getBaseContext();
        JavaCalls.setField(pushService, "mBase",
                CondomContext.wrap(mBase, TAG_CONDOM, XMOutbound.create(mBase, TAG)));
        joinPoint.proceed();
        xmPushService = pushService;

        logger.d("Service started");

        startForeground();
        if (SDK_INT > P) BackgroundActivityStartEnabler.initialize(xmPushService);
        if (SDK_INT >= N) {
            mNotificationRevival = new NotificationRevival(xmPushService, sbn -> sbn.getTag() == null);  // Only push notifications (tag == null)
            mNotificationRevival.initialize();
        }

        LocalBroadcastManager.getInstance(xmPushService).registerReceiver(mMessageReceiver,
                new IntentFilter("getConnectionStatus"));
        LocalBroadcastManager.getInstance(xmPushService).registerReceiver(mMessageReceiver,
                new IntentFilter(PushConstants.ACTION_RESET_CONNECTION));
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onStartCommand(..))")
    public void onStartCommand(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        startForeground();
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onStart(..))")
    public void onStart(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        Intent intent = (Intent) joinPoint.getArgs()[0];
        logIntent(intent);
        recordRegisterRequest(intent);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onBind(..)) && args(intent)")
    public void onBind(final JoinPoint joinPoint, Intent intent) {
        logger.d(joinPoint.getSignature());
        logIntent(intent);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onDestroy(..))")
    public void onDestroy(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        logger.d("Service stopped");
        xmPushService.stopForeground(true);

        if (SDK_INT >= N) mNotificationRevival.close();
    }

    @Before("execution(* com.xiaomi.smack.Connection.setConnectionStatus(..))")
    public void setConnectionStatus(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        Object[] args = joinPoint.getArgs();
        int newStatus = (int) args[0];
        int reason = (int) args[1];
        Exception e = (Exception) args[2];
        connectionStatus = getDesc(newStatus);

        sendSetConnectionStatus();
        if (newStatus == 1) {
            xmPushService.executeJob(new PullAllApplicationDataJob());
        }
    }

    private void sendSetConnectionStatus() {
        Intent intent = new Intent("setConnectionStatus");
        intent.putExtra("status", connectionStatus);
        if (xmPushService.getCurrentConnection() != null) {
            intent.putExtra("host", xmPushService.getCurrentConnection().getHost());
        }
        LocalBroadcastManager.getInstance(xmPushService).sendBroadcast(intent);
    }

    private String getDesc(int var1) {
        String var2;
        if (var1 == 1) {
            var2 = "connected";
        } else if (var1 == 0) {
            var2 = "connecting";
        } else if (var1 == 2) {
            var2 = "disconnected";
        } else {
            var2 = "unknown";
        }

        return var2;
    }

    private void startForeground() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(xmPushService.getApplicationContext());
        if (SDK_INT >= O) {
            String groupId = "status_group";
            NotificationChannelGroupCompat.Builder group =
                    new NotificationChannelGroupCompat.Builder(groupId)
                            .setName(CHANNEL_STATUS);
            manager.createNotificationChannelGroup(group.build());

            NotificationChannelCompat.Builder channel = new NotificationChannelCompat
                    .Builder(CHANNEL_STATUS, NotificationManager.IMPORTANCE_MIN)
                    .setName(xmPushService.getString(R.string.notification_category_alive)).setGroup(groupId);
            manager.createNotificationChannel(channel.build());

        }
        //if (ConfigCenter.getInstance().foregroundNotification || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        {
            Notification notification = new NotificationCompat.Builder(xmPushService,
                    CHANNEL_STATUS)
                    .setContentTitle(xmPushService.getString(R.string.notification_alive))
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true)
                    .setShowWhen(true)
                    .build();

            xmPushService.startForeground(NOTIFICATION_ALIVE_ID, notification);
        }
    }

    private void logIntent(Intent intent) {
        logger.d("Intent" + " " + ConvertUtils.toJson(intent));
    }


    private void recordRegisterRequest(Intent intent) {
        try {
            if (intent == null) {
                return;
            }
            if (!PushConstants.MIPUSH_ACTION_REGISTER_APP.equals(intent.getAction())) {
                return;
            }

            String pkg = intent.getStringExtra(Constants.EXTRA_MI_PUSH_PACKAGE);
            if (pkg == null) {
                logger.e("Package name is NULL!");
                return;
            }

            RegisteredApplication application = RegisteredApplicationDb
                    .registerApplication(pkg, true);
            if (application == null) {
                return;
            }

            logger.d("onHandleIntent -> A application want to register push");
            showRegisterToastIfExistsConfiguration(application);
            EventDb.insertEvent(Event.ResultType.OK,
                    new RegistrationType(null, pkg, null)
            );
        } catch (RuntimeException e) {
            logger.e("XMPushService::onHandleIntent: ", e);
            Utils.makeText(xmPushService, xmPushService.getString(R.string.common_err, e.getMessage()), Toast.LENGTH_LONG);
        }
    }

    private void showRegisterToastIfExistsConfiguration(RegisteredApplication application) {
        String pkg = application.getPackageName();
        boolean notificationOnRegister = ConfigCenter.getInstance().isNotificationOnRegister(xmPushService);
        notificationOnRegister = notificationOnRegister && application.isNotificationOnRegister();
        if (notificationOnRegister) {
            CharSequence appName = ApplicationNameCache.getInstance().getAppName(xmPushService, pkg);
            CharSequence usedString = xmPushService.getString(R.string.notification_registerAllowed, appName);
            Utils.makeText(xmPushService, usedString, Toast.LENGTH_SHORT);
        } else {
            Log.e("XMPushService Bridge", "Notification disabled");
        }
    }

    class PullAllApplicationDataJob extends XMPushService.Job {
        public PullAllApplicationDataJob() {
            super(XMPushService.Job.TYPE_SEND_MSG);
        }

        @Override
        public String getDesc() {
            return "pull all application data";
        }

        @Override
        public void process() {
            SharedPreferences sp = Utils.getApplication().getSharedPreferences("pref_registered_pkg_names", 0);
            for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
                String packageName = entry.getKey();
                String appId = entry.getValue() == null ? null : entry.getValue().toString();
                if (TextUtils.isEmpty(appId)) {
                    continue;
                }

                XmPushActionNotification notification2 = new XmPushActionNotification();
                notification2.setAppId(appId);
                notification2.setType("pull");
                notification2.setId("fake_pull_" + appId + "_" + System.currentTimeMillis());
                notification2.setRequireAck(false);

                XmPushActionContainer sendMsgContainer = JavaCalls.callStaticMethod(
                        PushContainerHelper.class.getName(), "generateRequestContainer",
                        Utils.getApplication(), notification2, ActionType.Notification,
                        false, packageName, appId);
                byte[] msgBytes = XmPushThriftSerializeUtils.convertThriftObjectToBytes(sendMsgContainer);
                xmPushService.sendMessage(packageName, msgBytes, false);
            }
        }
    }
}

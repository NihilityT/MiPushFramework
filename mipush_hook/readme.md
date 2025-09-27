# 不能在此模块中使用写 kotlin 代码，会出现编译错误

# server -> xmsf -> application

``` mermaid

---
config:
  layout: elk
---
flowchart TB
 subgraph cloud["cloud"]
        cloud.mipushServer["mipush server"]
  end
 subgraph PushMessageProcessor["PushMessageProcessor"]
        SyncInfoHelper.doSyncInfoAsync["doSyncInfoAsync"]
        PushMessageProcessor.processMessage["processMessage"]
        PushMessageProcessor.processIntent["processIntent"]
        PushMessageProcessor.tryToReinitialize["tryToReinitialize"]
        PushMessageProcessor.ackMessage["ackMessage"]
  end
 subgraph MessageHandleService["MessageHandleService"]
        MessageHandleService.processJob["processJob"]
  end
 subgraph PushMessageReceiver["PushMessageReceiver (exported)"]
        PushMessageReceiver.onReceive["onReceive"]
  end
 subgraph PushMessageHandler["PushMessageHandler (exported)"]
        PushMessageHandler.handleNewMessage["handleNewMessage"]
        PushMessageHandler.onHandleIntent["onHandleIntent"]
        PushMessageHandler.onStart["onStart"]
        PushMessageHandler.processMessageForCallback["processMessageForCallback"]
  end
 subgraph PushServiceReceiver["PushServiceReceiver"]
        PushServiceReceiver.onReceive["onReceive"]
  end
 subgraph PushMessageHelper["PushMessageHelper"]
        PushMessageHelper.sendCommandMessageBroadcast["sendCommandMessageBroadcast"]
        PushMessageHelper.sendQuitMessageBroadcast["sendQuitMessageBroadcast"]
  end
 subgraph MiPushClient["MiPushClient"]
        MiPushClient.reInitialize["reInitialize"]
        MiPushClient.initialize["initialize"]
  end
 subgraph MiPushSdk["com.xiaomi.mipush.sdk"]
        PushMessageProcessor
        MessageHandleService
        PushMessageReceiver
        PushMessageHandler
        PushServiceReceiver
        PushMessageHelper
        MiPushClient
  end
 subgraph APP["app.that.support.mipush"]
        Application["Application"]
        MiPushSdk
  end
 subgraph MIPushEventProcessor["MIPushEventProcessor"]
        NotificationManagerHelper.notify["notify"]
        MIPushNotificationHelper.notifyPushMessage["notifyPushMessage"]
        MIPushEventProcessor.postProcessMIPushMessage["postProcessMIPushMessage"]
        MIPushEventProcessor.processMIPushMessage["processMIPushMessage"]
        MIPushEventProcessor.processNewPacket["processNewPacket"]
  end
 subgraph ClientEventDispatcher["ClientEventDispatcher"]
        ClientEventDispatcher.notifyPacketArrival["notifyPacketArrival"]
  end
 subgraph PacketSync["PacketSync"]
        PacketSync.onPacketReceive["onPacketReceive"]
        PacketSync.handleBlob["handleBlob"]
        PacketSync.onBlobReceive["onBlobReceive"]
  end
 subgraph XMPushService["XMPushService"]
        XMPushService.BlobReceiveJob["BlobReceiveJob"]
        XMPushService.PacketListener["PacketListener"]
        XMPushService.PacketReceiveJob["PacketReceiveJob"]
  end
 subgraph MiPushService["com.xiaomi.push.service"]
        MIPushEventProcessor
        ClientEventDispatcher
        PacketSync
        XMPushService
  end
 subgraph XMSF["com.xiaomi.xmsf"]
        MiPushService
  end
    cloud.mipushServer --> XMPushService.PacketListener
    XMPushService.PacketListener --> XMPushService.BlobReceiveJob & XMPushService.PacketReceiveJob
    XMPushService.BlobReceiveJob --> PacketSync.onBlobReceive
    XMPushService.PacketReceiveJob --> PacketSync.onPacketReceive
    PacketSync.onPacketReceive --> ClientEventDispatcher.notifyPacketArrival
    PacketSync.onBlobReceive --> PacketSync.handleBlob
    PacketSync.handleBlob --> ClientEventDispatcher.notifyPacketArrival
    ClientEventDispatcher.notifyPacketArrival -- chid 5 --> MIPushEventProcessor.processNewPacket
    MIPushEventProcessor.processNewPacket --> MIPushEventProcessor.processMIPushMessage
    MIPushEventProcessor.processMIPushMessage -- Intent(MIPUSH_ACTION_NEW_MESSAGE) --> MIPushEventProcessor.postProcessMIPushMessage
    MIPushEventProcessor.postProcessMIPushMessage --> MIPushNotificationHelper.notifyPushMessage
    MIPushNotificationHelper.notifyPushMessage --> NotificationManagerHelper.notify
    MIPushEventProcessor.postProcessMIPushMessage == broadcast<br>MIPUSH_ACTION_MESSAGE_ARRIVED for notification<br>MIPUSH_ACTION_NEW_MESSAGE for passthrough ==> PushMessageReceiver.onReceive
    NotificationManagerHelper.notify == notification ==> PushMessageHandler.onStart
    PushMessageHelper.sendCommandMessageBroadcast --> PushServiceReceiver.onReceive
    PushMessageHelper.sendQuitMessageBroadcast --> PushServiceReceiver.onReceive
    PushServiceReceiver.onReceive --> PushMessageHandler.onHandleIntent
    PushMessageHandler.onStart ==> PushMessageHandler.onHandleIntent
    PushMessageHandler.onHandleIntent ==> PushMessageHandler.handleNewMessage
    PushMessageHandler.handleNewMessage ==> MessageHandleService.processJob
    PushMessageReceiver.onReceive ==> MessageHandleService.processJob
    MessageHandleService.processJob ==> PushMessageProcessor.processIntent
    MessageHandleService.processJob == callback to Application by PushMessageReceiver ==> Application
    PushMessageProcessor.processIntent == "!appRegistered ||<br>!AppInfoHolder.invalidated() ||<br>!appRegistered &amp;&amp;<br>!ActionType.Registration &amp;&amp;<br>isIgnoreRegInfo &amp;&amp;<br>id.length == 22 &amp;&amp;<br>'satuigmo'.contains(id[0])" ==> PushMessageProcessor.processMessage
    PushMessageProcessor.processIntent -- MIPUSH_ACTION_NEW_MESSAGE --> PushMessageProcessor.ackMessage
    PushMessageProcessor.processMessage -- !MIPUSH_ACTION_NEW_MESSAGE &&!fromNotification --> PushMessageProcessor.ackMessage
    PushMessageHandler.processMessageForCallback -. "MiPushClient.MiPushClientCallback" .-> Application
    PushMessageProcessor.processMessage -- "NotificationType.ForceSync" --> SyncInfoHelper.doSyncInfoAsync
    PushMessageHandler.onHandleIntent -. CallbackPushMode(1) .-> PushMessageProcessor.processIntent
    PushMessageProcessor.processIntent -. return value .-> PushMessageHandler.processMessageForCallback
    PushMessageProcessor.processIntent -- "!appRegistered && !ActionType.Registration" --> PushMessageProcessor.tryToReinitialize
    PushMessageProcessor.tryToReinitialize --> MiPushClient.reInitialize
    PushMessageProcessor.processMessage -- "Notification && NotificationType.RegIdExpired" --> MiPushClient.reInitialize
    Application --> MiPushClient.initialize


```


# application -> xmsf -> server

``` mermaid

graph TB;
subgraph cloud
    cloud.mipushServer["mipush server"]
end

Application --Context.startService()--> XMPushService.onStartCommand
Application --Context.bindService()-->  XMPushService.Messenger

subgraph app.that.support.mipush

    Application
end

subgraph com.xiaomi.xmsf
subgraph com.xiaomi.push.service
    subgraph MIPushAccountUtils
        MIPushAccountUtils.register
    end
    subgraph MIPushHelper
        MIPushHelper.contructAppAbsentMessage
    end
    subgraph MIPushClientManager
        MIPushClientManager.registerApp
    end

    subgraph XMPushService
        XMPushService.Messenger --send(what:17)--> XMPushService.onStart
        XMPushService.onStartCommand --> XMPushService.onStart
        XMPushService.onStart --IntentJob--> XMPushService.handleIntent

        XMPushService.handleIntent ==ACTION_SEND_MESSAGE==> XMPushService.sendMessage
        XMPushService.handleIntent --MIPUSH_ACTION_UNREGISTER_APP/ACTION_SEND_IQ/ACTION_SEND_PRESENCE--> XMPushService.sendMessage
        XMPushService.sendMessage --> XMPushService.sendPacket
        XMPushService.sendPacket --> cloud.mipushServer

        XMPushService.handleIntent --ACTION_BATCH_SEND_MESSAGE--> XMPushService.sendMessages
        XMPushService.sendMessages --> XMPushService.batchSendPacket
        XMPushService.batchSendPacket --> cloud.mipushServer

        XMPushService.handleIntent --ACTION_OPEN_CHANNEL--> XMPushService.BindJob/ReBindJob --> cloud.mipushServer
        XMPushService.handleIntent --ACTION_CLOSE_CHANNEL--> XMPushService.UnbindJob --> cloud.mipushServer

        MIPushClientManager.registerApp --> XMPushService.MIPushAppRegisterJob
        --> MIPushAccountUtils.register --register mipush account on server by HTTP.POST--> cloud.mipushServer
        MIPushAccountUtils.register --reconnect XMPP--> cloud.mipushServer

        XMPushService.handleIntent --ACTION_UNINSTALL--> MIPushHelper.contructAppAbsentMessage --> cloud.mipushServer
        XMPushService.handleIntent --MIPUSH_ACTION_ENABLE_PUSH_MESSAGE--> XMPushService.MIPushAppRegisterJob

        XMPushService.handleIntent --MIPUSH_ACTION_REGISTER_APP--> XMPushService.registerForMiPushApp
        --> MIPushClientManager.registerApp --XMPP--> cloud.mipushServer
    end
end
end

```


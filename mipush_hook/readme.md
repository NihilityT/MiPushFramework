# 不能在此模块中使用写 kotlin 代码，会出现编译错误

``` mermaid
graph TB;
subgraph cloud
    cloud.mipushServer["mipush server"]
end

Application --Context.startService()--> XMPushService.onStartCommand
Application --Context.bindService()-->  XMPushService.Messenger
cloud.mipushServer --> XMPushService.PacketListener

subgraph app.that.support.mipush

    Application
    PushMessageProcessor.processMessage --PushMessageReceiver--> Application
    PushMessageHandler.processMessageForCallback --MiPushClient.MiPushClientCallback--> Application

    subgraph com.xiaomi.mipush.sdk
        subgraph PushMessageProcessor
            PushMessageProcessor.processIntent --> PushMessageProcessor.processMessage
            --NotificationType.ForceSync--> SyncInfoHelper.doSyncInfoAsync
        end
        subgraph MessageHandleService
            MessageHandleService.processJob
        end
        subgraph PushMessageReceiver
            PushMessageReceiver.onReceive --> MessageHandleService.processJob
        end
        subgraph PushMessageHandler
            PushMessageHandler.onStart --> PushMessageHandler.onHandleIntent
            --> PushMessageHandler.handleNewMessage --> MessageHandleService.processJob
            --> PushMessageProcessor.processIntent
            PushMessageHandler.onHandleIntent --CallbackPushMode--> PushMessageProcessor.processIntent
            PushMessageProcessor.processMessage --> PushMessageHandler.processMessageForCallback
        end
        subgraph PushServiceReceiver
            PushServiceReceiver.onReceive --> PushMessageHandler.onHandleIntent
        end
        subgraph PushMessageHelper
            PushMessageHelper.sendCommandMessageBroadcast --> PushServiceReceiver.onReceive
            PushMessageHelper.sendQuitMessageBroadcast --> PushServiceReceiver.onReceive
        end
    end
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
    subgraph ClientEventDispatcher
        ClientEventDispatcher.notifyPacketArrival
    end
    subgraph MIPushNotificationHelper
        MIPushNotificationHelper.notifyPushMessage
    end
    subgraph NotificationManagerHelper
        NotificationManagerHelper.notify
    end

    subgraph MIPushEventProcessor
        MIPushEventProcessor.processNewPacket
        --> MIPushEventProcessor.processMIPushMessage
        --Intent(MIPUSH_ACTION_NEW_MESSAGE)--> MIPushEventProcessor.postProcessMIPushMessage
        --> MIPushNotificationHelper.notifyPushMessage --> NotificationManagerHelper.notify --notification--> PushMessageHandler.onStart
        MIPushEventProcessor.postProcessMIPushMessage --broadcast MIPUSH_ACTION_MESSAGE_ARRIVED--> PushMessageReceiver.onReceive
    end
    subgraph PacketSync
        PacketSync.onPacketReceive --> ClientEventDispatcher.notifyPacketArrival
        PacketSync.onBlobReceive --> PacketSync.handleBlob --> ClientEventDispatcher.notifyPacketArrival
        --chid 5--> MIPushEventProcessor.processNewPacket
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

        XMPushService.handleIntent --MIPUSH_ACTION_REGISTER_APP--> XMPushService.registerForMiPushApp
        --> MIPushClientManager.registerApp --XMPP--> cloud.mipushServer

        MIPushClientManager.registerApp --> XMPushService.MIPushAppRegisterJob
        --> MIPushAccountUtils.register --register mipush account on server by HTTP.POST--> cloud.mipushServer
        MIPushAccountUtils.register --reconnect XMPP--> cloud.mipushServer

        XMPushService.handleIntent --ACTION_UNINSTALL--> MIPushHelper.contructAppAbsentMessage --> cloud.mipushServer
        XMPushService.handleIntent --MIPUSH_ACTION_ENABLE_PUSH_MESSAGE--> XMPushService.MIPushAppRegisterJob

        XMPushService.PacketListener --> XMPushService.BlobReceiveJob --> PacketSync.onBlobReceive
        XMPushService.PacketListener --> XMPushService.PacketReceiveJob --> PacketSync.onPacketReceive
    end
end
end



```

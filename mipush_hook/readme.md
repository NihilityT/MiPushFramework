# 不能在此模块中使用写 kotlin 代码，会出现编译错误

``` mermaid
graph TB;
subgraph cloud
    mipushServer["mipush server"]
end

Application --Context.startService()--> onStartCommand
Application --Context.bindService()-->  Messenger
mipushServer --> PacketListener

subgraph app.that.support.mipush

    Application
    processMessage --PushMessageReceiver--> Application
    processMessageForCallback --MiPushClient.MiPushClientCallback--> Application
    
    subgraph com.xiaomi.mipush.sdk
        subgraph PushMessageProcessor
            processIntent --> processMessage
        end
        subgraph PushMessageHandler
            PushMessageHandler.onStart --> PushMessageHandler.onHandleIntent
            --> handleNewMessage --> processJob
            --> processIntent
            PushMessageHandler.onHandleIntent --CallbackPushMode--> processIntent
            processMessage --> processMessageForCallback
        end
    end
end

subgraph com.xiaomi.xmsf
subgraph com.xiaomi.push.service
    subgraph MIPushAccountUtils
        register
    end
    subgraph MIPushHelper
        contructAppAbsentMessage
    end
    subgraph MIPushClientManager
        registerApp
    end
    subgraph ClientEventDispatcher
        notifyPacketArrival
    end
    subgraph MIPushNotificationHelper
        notifyPushMessage
    end
    subgraph NotificationManagerHelper
        notify
    end
    
    subgraph MIPushEventProcessor
        processNewPacket
        --> processMIPushMessage
        --Intent(MIPUSH_ACTION_NEW_MESSAGE)--> postProcessMIPushMessage
        --> notifyPushMessage --> notify --notification--> PushMessageHandler.onStart
    end
    subgraph PacketSync
        onPacketReceive --> notifyPacketArrival
        onBlobReceive --> handleBlob --> notifyPacketArrival
        --chid 5--> processNewPacket
    end
    
    subgraph XMPushService
        Messenger --send(what:17)--> onStart
        onStartCommand --> onStart
        onStart --IntentJob--> handleIntent
        
        handleIntent ==ACTION_SEND_MESSAGE==> sendMessage
        handleIntent --MIPUSH_ACTION_UNREGISTER_APP/ACTION_SEND_IQ/ACTION_SEND_PRESENCE--> sendMessage
        sendMessage --> sendPacket
        sendPacket --> mipushServer
        
        handleIntent --ACTION_BATCH_SEND_MESSAGE--> sendMessages
        sendMessages --> batchSendPacket
        batchSendPacket --> mipushServer
        
        handleIntent --ACTION_OPEN_CHANNEL--> BindJob/ReBindJob --> mipushServer
        handleIntent --ACTION_CLOSE_CHANNEL--> UnbindJob --> mipushServer
        
        handleIntent --MIPUSH_ACTION_REGISTER_APP--> registerForMiPushApp
        --> registerApp --XMPP--> mipushServer
        
        registerApp --> MIPushAppRegisterJob
        --> register --register mipush account on server by HTTP.POST--> mipushServer
        register --reconnect XMPP--> mipushServer
        
        handleIntent --ACTION_UNINSTALL--> contructAppAbsentMessage --> mipushServer
        handleIntent --MIPUSH_ACTION_ENABLE_PUSH_MESSAGE--> MIPushAppRegisterJob
        
        PacketListener --> BlobReceiveJob --> onBlobReceive
        PacketListener --> PacketReceiveJob --> onPacketReceive
    end
end
end



```
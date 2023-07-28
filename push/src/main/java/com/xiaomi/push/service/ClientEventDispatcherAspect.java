package com.xiaomi.push.service;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.slim.Blob;
import com.xiaomi.smack.packet.Packet;
import com.xiaomi.xmpush.thrift.ActionType;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import top.trumeet.common.BuildConfig;

/**
 * Created by Trumeet on 2018/1/22.
 * 修改过的 ClientEventDispatcher，用于修改包接收处理逻辑
 * <p>
 * 消息的处理：
 * 发送方（framework）：
 * <p>
 * 广播 1： {@link PushConstants#MIPUSH_ACTION_MESSAGE_ARRIVED}
 * {@link MIPushEventProcessor} 负责将序列化后的消息广播/发送通知。
 * 具体可以看到 {@link MIPushEventProcessor#postProcessMIPushMessage(XMPushService, String, byte[], Intent, boolean)}
 * 里面的 170 行。它发送了 {@link PushConstants#MIPUSH_ACTION_MESSAGE_ARRIVED} 广播给客户端。
 * <p>
 * 广播 2： {@link PushConstants#MIPUSH_ACTION_NEW_MESSAGE}；
 * 同样由 {@link MIPushEventProcessor} 发送。最初是在 {@link MIPushEventProcessor#buildIntent(byte[], long)} 中生成，由
 * {@link MIPushEventProcessor#postProcessMIPushMessage(XMPushService, String, byte[], Intent, boolean)} 中 192 行发送。
 * <p>
 * 广播 3： {@link PushConstants#MIPUSH_ACTION_ERROR}
 * 由 {@link MIPushClientManager#notifyError} 发送。
 * <p>
 * 客户端（接收方）：
 * 消息 intent 统一由 {@link com.xiaomi.mipush.sdk.PushMessageProcessor#processIntent} 处理。
 * <p>
 * Warning:
 * 理论上这里是服务器发送给 Framework，然后再由 Framework 发给对方 app 的中转。所以一些请求类的 request（如 {@link ActionType#Subscription}
 * 这里拦截没有任何作用，所以没有在这里处理，仅记录。
 */

@Aspect
public class ClientEventDispatcherAspect {
    private static final String TAG = ClientEventDispatcherAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @Before("execution(* com.xiaomi.push.service.ClientEventDispatcher.notifyPacketArrival(..)) && args(pushService, chid, data)")
    public void notifyPacketArrival(final JoinPoint joinPoint,
                                    XMPushService pushService, String chid, Object data) {
        logger.d(joinPoint.getSignature());
        if (data instanceof Blob) {
            Blob blob = (Blob) data;
            if (BuildConfig.DEBUG) {
                logger.d("blob arrival: " + chid + "; " + blob);
            }
        } else {
            Packet packet = (Packet) data;
            if (BuildConfig.DEBUG) {
                logger.d("packet arrival: " + chid + "; " + packet.toXML());
            }
        }
    }

}

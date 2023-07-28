package com.xiaomi.xmsf.utils;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.xiaomi.channel.commonutils.android.DataCryptUtils;
import com.xiaomi.channel.commonutils.string.Base64Coder;
import com.xiaomi.mipush.sdk.DecryptException;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.xmpush.thrift.*;
import com.xiaomi.xmsf.push.utils.Utils;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public class ConvertUtils {
    private static final String TAG = ConvertUtils.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public static JsonElement toJson(XmPushActionContainer container) {
        return toJson(container, Utils.getRegSec(container));
    }

    public static JsonElement toJson(XmPushActionContainer container, String regSec) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        String[] exclude = {"hb", "__isset_bit_vector"};
                        for (String field : exclude) {
                            if (f.getName().equals(field)) {
                                return true;
                            }
                        }
                        if (f.getDeclaredClass() == Map.class && f.getName().equals("internal")) {
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
        JsonElement jsonElement = gson.toJsonTree(container);
        if (jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            String pushAction = "pushAction";
            try {
                TBase message = getResponseMessageBodyFromContainer(container, regSec);
                json.add(pushAction, gson.toJsonTree(message));
            } catch (TException e) {
                logger.e(e.getLocalizedMessage(), e);
            } catch (Throwable e) {
                json.add(pushAction, gson.toJsonTree(e));
            }
            jsonElement = json;
        }
        return jsonElement;
    }

    public static JsonElement toJson(Intent intent) {
        if (intent == null) {
            return JsonNull.INSTANCE;
        }
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new BundleTypeAdapterFactory())
                .create();
        JsonObject json = new JsonObject();
        json.add("action", gson.toJsonTree(intent.getAction()));
        if (intent.getExtras() != null) {
            JsonObject extras = (JsonObject) gson.toJsonTree(intent.getExtras());
            byte[] payload = intent.getByteArrayExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD);
            if (payload != null) {
                extras.add(PushConstants.MIPUSH_EXTRA_PAYLOAD, toJson(
                        MIPushEventProcessor.buildContainer(payload)));
            }
            json.add("extras", extras);
        }
        return json;
    }


    public static TBase getResponseMessageBodyFromContainer(XmPushActionContainer container, String regSec)
            throws TException, DecryptException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        byte[] oriMsgBytes;
        boolean encrypted = container.isEncryptAction();
        if (encrypted) {
            Objects.requireNonNull(regSec, "register secret is null");
            byte[] keyBytes = Base64Coder.decode(regSec);
            try {
                oriMsgBytes = DataCryptUtils.mipushDecrypt(keyBytes, container.getPushAction());
            } catch (Exception e) {
                throw new DecryptException("the aes decrypt failed.", e);
            }
        } else {
            oriMsgBytes = container.getPushAction();
        }
        try {
            Method createRespMessageFromAction = PushContainerHelper.class.getDeclaredMethod("createRespMessageFromAction", ActionType.class, boolean.class);
            createRespMessageFromAction.setAccessible(true);
            TBase packet = createMessageFromAction(container.getAction(), container.isRequest);
            if (packet != null) {
                XmPushThriftSerializeUtils.convertByteArrayToThriftObject(packet, oriMsgBytes);
            }
            return packet;
        } catch (Exception e) {
            throw e;
        }
    }

    private static TBase createMessageFromAction(ActionType act, boolean isRequest) {
        if (isRequest) {
            return createRequestMessageFromAction(act);
        }
        return createResponseMessageFromAction(act);
    }

    private static TBase createRequestMessageFromAction(ActionType act) {
        switch (act) {
            case Registration:
                return new XmPushActionRegistration();
            case UnRegistration:
                return new XmPushActionUnRegistration();
            case Subscription:
                return new XmPushActionSubscription();
            case UnSubscription:
                return new XmPushActionUnSubscription();
            case SendMessage:
                return new XmPushActionSendMessage();
            case AckMessage:
                return new XmPushActionAckMessage();
            case SetConfig:
                return new XmPushActionCommand();
            case ReportFeedback:
                return new XmPushActionSendFeedback();
            case Notification:
                return new XmPushActionNotification();
            case Command:
                return new XmPushActionCommand();
            default:
                return null;
        }
    }

    private static TBase createResponseMessageFromAction(ActionType act) {
        switch (act) {
            case Registration:
                return new XmPushActionRegistrationResult();
            case UnRegistration:
                return new XmPushActionUnRegistrationResult();
            case Subscription:
                return new XmPushActionSubscriptionResult();
            case UnSubscription:
                return new XmPushActionUnSubscriptionResult();
            case SendMessage:
                return new XmPushActionSendMessage();
            case AckMessage:
                return new XmPushActionAckMessage();
            case SetConfig:
                return new XmPushActionCommandResult();
            case ReportFeedback:
                return new XmPushActionSendFeedbackResult();
            case Notification:
                XmPushActionAckNotification ackNotification = new XmPushActionAckNotification();
                ackNotification.setErrorCodeIsSet(true);
                return ackNotification;
            case Command:
                return new XmPushActionCommandResult();
            default:
                return null;
        }
    }
}

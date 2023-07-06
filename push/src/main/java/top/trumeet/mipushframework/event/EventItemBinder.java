package top.trumeet.mipushframework.event;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiaomi.channel.commonutils.string.Base64Coder;
import com.xiaomi.mipush.sdk.DecryptException;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.MIPushEventProcessorAspect;
import com.xiaomi.push.service.XMPushServiceAspect;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.notification.NotificationChannelManager;
import com.xiaomi.xmsf.push.utils.Configurations;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.EventType;
import top.trumeet.mipush.provider.event.type.TypeFactory;
import top.trumeet.mipushframework.permissions.ManagePermissionsActivity;
import top.trumeet.mipushframework.utils.BaseAppsBinder;

/**
 * Created by Trumeet on 2017/8/26.
 *
 * @author Trumeet
 * @see Event
 * @see EventFragment
 */

public class EventItemBinder extends BaseAppsBinder<Event> {
    private static Logger logger = XLog.tag(EventItemBinder.class.getSimpleName()).build();

    EventItemBinder() {
        super();
    }

    @Override
    protected void onBindViewHolder(final @NonNull ViewHolder holder, final @NonNull Event item) {
        fillData(item.getPkg(), false, holder);
        final EventType type = TypeFactory.create(item, item.getPkg());
        holder.title.setText(type.getTitle(holder.itemView.getContext()));
        holder.summary.setText(type.getSummary(holder.itemView.getContext()));

        String status = "";
        switch (item.getResult()) {
            case Event.ResultType.OK:
                status = fillEventData(holder, item);
                break;
            case Event.ResultType.DENY_DISABLED:
                status = holder.itemView.getContext()
                        .getString(R.string.status_deny_disable);
                break;
            case Event.ResultType.DENY_USER:
                status = holder.itemView.getContext()
                        .getString(R.string.status_deny_user);
                break;
            default:
                break;
        }

        Calendar calendarServer = Calendar.getInstance();
        calendarServer.setTime(new Date(item.getDate()));
        int zoneOffset = calendarServer.get(java.util.Calendar.ZONE_OFFSET);
        int dstOffset = calendarServer.get(java.util.Calendar.DST_OFFSET);
        calendarServer.add(java.util.Calendar.MILLISECOND, (zoneOffset + dstOffset));
        DateFormat formatter = SimpleDateFormat.getDateTimeInstance();

        holder.text2.setText(holder.itemView.getContext().getString(R.string.date_format_long,
                formatter.format(calendarServer.getTime())));
        holder.status.setText(status);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = createInfoDialog(item,
                        holder.itemView.getContext()); // "Developer info" dialog for event messages
                if (dialog != null) {
                    dialog.show();
                } else {
                    startManagePermissions(type.getPkg(), holder.itemView.getContext());
                }
            }
        });
    }

    @Nullable
    private String fillEventData(@NonNull ViewHolder holder, @NonNull Event item) {
        do {
            byte[] payload = item.getPayload();
            if (payload == null) {
                break;
            }
            XmPushActionContainer container = MIPushEventProcessor.buildContainer(payload);
            if (!container.metaInfo.isSetPassThrough()) {
                break;
            }
            if (container.metaInfo.passThrough == 1) {
                return holder.itemView.getContext()
                        .getString(R.string.message_type_pass_through);
            }
            if (container.metaInfo.passThrough == 0) {
                new ConfigurationWorkerTask(holder, container).execute();
                return holder.itemView.getContext()
                        .getString(R.string.message_type_notification);
            }
        } while (false);
        return "";
    }

    @Nullable
    private Dialog createInfoDialog(final Event event, final Context context) {
        XmPushActionContainer container = buildContainer(event.getPayload());
        final CharSequence info = containerToJson(container, event.getRegSec());
        if (info == null)
            return null;

        TextView showText = new TextView(context);
        showText.setText(info);
        showText.setTextSize(14);
        showText.setTextIsSelectable(true);
        showText.setTypeface(Typeface.MONOSPACE);

        final ScrollView scrollView = new ScrollView(context);
        scrollView.addView(showText);

        AlertDialog.Builder build = new AlertDialog.Builder(context)
                .setView(scrollView)
                .setTitle("Developer Info")
                .setNeutralButton(android.R.string.copy, (dialogInterface, i) -> {
                    ClipboardManager clipboardManager = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(info);
                })
                .setNegativeButton(R.string.action_edit_permission, (dialogInterface, i) ->
                        startManagePermissions(event.getPkg(), context));

        AlertDialog dialog;
        if (event.getPayload() != null) {
            build.setPositiveButton(R.string.action_notify, (dialogInterface, i) ->
                    MIPushEventProcessorAspect.mockProcessMIPushMessage(XMPushServiceAspect.xmPushService, event.getPayload()));
            build.setNeutralButton(R.string.action_configurate, null);

            dialog = build.create();
            dialog.setOnShowListener(dialogInterface -> {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                button.setOnClickListener(view -> {
                    try {
                        XmPushActionContainer newContainer = buildContainer(event.getPayload());
                        Configurations.getInstance().handle(container.packageName, newContainer);
                        showText.setText(containerToJson(newContainer, event.getRegSec()));
                    } catch (Throwable e) {
                        e.printStackTrace();
                        showText.setText(e.toString());
                    }
                });
            });
        } else {
            dialog = build.create();
        }
        return dialog;
    }

    private CharSequence containerToJson(XmPushActionContainer container, String regSec) {
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
        final CharSequence info = gson.toJson(jsonElement);
        return info;
    }

    public static TBase getResponseMessageBodyFromContainer(XmPushActionContainer container, String regSec)
            throws TException, DecryptException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        byte[] oriMsgBytes;
        boolean encrypted = container.isEncryptAction();
        if (encrypted) {
            byte[] keyBytes = Base64Coder.decode(regSec);
            try {
                oriMsgBytes = PushContainerHelper.MIPushDecrypt(keyBytes, container.getPushAction());
            } catch (Exception e) {
                throw new DecryptException("the aes decrypt failed.", e);
            }
        } else {
            oriMsgBytes = container.getPushAction();
        }
        try {
            Method createRespMessageFromAction = PushContainerHelper.class.getDeclaredMethod("createRespMessageFromAction", ActionType.class , boolean.class);
            createRespMessageFromAction.setAccessible(true);
            TBase packet = (TBase) createRespMessageFromAction.invoke(null, container.getAction(), container.isRequest);
            if (packet != null) {
                XmPushThriftSerializeUtils.convertByteArrayToThriftObject(packet, oriMsgBytes);
            }
            return packet;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void startManagePermissions(String packageName, Context context) {
        // Issue: This currently allows overlapping opens.
        context.startActivity(new Intent(context, ManagePermissionsActivity.class)
                .putExtra(ManagePermissionsActivity.EXTRA_PACKAGE_NAME, packageName));
    }

    private static class ConfigurationWorkerTask extends AsyncTask<String, Void, String> {
        private final ViewHolder viewHolder;
        private final XmPushActionContainer container;

        ConfigurationWorkerTask(ViewHolder viewHolder, XmPushActionContainer container) {
            this.viewHolder = viewHolder;
            this.container = container;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Set<String> ops = Configurations.getInstance().handle(container.getPackageName(), container);
                String status = container.getMetaInfo().getExtra().get("channel_name");
                if (!NotificationChannelManager.isNotificationChannelEnabled(
                        container.getPackageName(),
                        NotificationChannelManager.getChannelId(
                                container.metaInfo, container.getPackageName()))) {
                    ops.add("disable");
                }
                if (!ops.isEmpty()) {
                    status = ops + " " + status;
                }
                return status;
            } catch (Throwable ignored) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String status) {
            if (status != null) {
                viewHolder.status.setText(status);
            }
        }
    }
}

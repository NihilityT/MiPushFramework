package top.trumeet.mipushframework;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.nihility.InternalMessenger;
import com.nihility.service.XMPushServiceListener;
import com.xiaomi.channel.commonutils.android.DeviceInfo;
import com.xiaomi.channel.commonutils.android.MIUIUtils;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.xmsf.utils.ConfigCenter;

public class MainPageUtils {
    private static final String TAG = MainPageUtils.class.getSimpleName();
    InternalMessenger messenger;

    public interface ConnectionStatusChanged {
        void onChange(XMPushServiceListener.ConnectionStatus status);
    }

    public MainPageUtils() {
    }

    void initOnCreate(Context context, ConnectionStatusChanged connectionStatusChanged) {
        context = context.getApplicationContext();
        messenger = new InternalMessenger(context) {{
            register(new IntentFilter(XMPushServiceMessenger.IntentSetConnectionStatus));
            addListener(intent -> {
                String status = intent.getStringExtra("status");
                connectionStatusChanged.onChange(XMPushServiceListener.ConnectionStatus.valueOf(status));
            });
        }};

        printHookResultForCheck();

        ConfigCenter.getInstance().loadConfigurations(context);

        messenger.send(new Intent(XMPushServiceMessenger.IntentGetConnectionStatus));
    }

    void printHookResultForCheck() {
        Log.i(TAG, String.format("[hook_res] MIUIUtils.getIsMIUI() -> [%s]", MIUIUtils.getIsMIUI()));
        Log.i(TAG, String.format("[hook_res] DeviceInfo.quicklyGetIMEI() -> [%s]", DeviceInfo.quicklyGetIMEI(null)));
        Log.i(TAG, String.format("[hook_res] DeviceInfo.getMacAddress() -> [%s]", DeviceInfo.getMacAddress(null)));
        Log.i(TAG, String.format("[hook_res] ConnectionConfiguration.getXmppServerHost() -> [%s]", ConnectionConfiguration.getXmppServerHost()));
    }
}
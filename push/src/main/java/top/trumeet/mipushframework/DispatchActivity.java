package top.trumeet.mipushframework;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.push.sdk.MyPushMessageHandler;
import com.xiaomi.push.service.PushConstants;

public class DispatchActivity extends AppCompatActivity {
    private static final Logger logger = XLog.tag("DispatchActivity").build();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        logger.d("onCreate");
        handle(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        logger.d("onNewIntent");
        handle(intent);
    }


    private void handle(Intent intent) {
        byte[] payload = intent.getByteArrayExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD);
        if (payload != null &&
                MyPushMessageHandler.forwardToTargetApplication(getApplication(), payload) != null) {
            MyPushMessageHandler.cancelNotification(getApplication(), intent.getExtras());
        }
        finish();
    }

}

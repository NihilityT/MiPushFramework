package top.trumeet.mipushframework.wizard;

import android.content.Context;

import androidx.annotation.NonNull;

import com.xiaomi.xmsf.R;

public class WelcomePhonyPermissionInfo extends DisplayOnlyPhonyPermissionInfo {
    public WelcomePhonyPermissionInfo(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public String getPermissionTitle() {
        return context.getString(R.string.app_name);
    }

    @NonNull
    @Override
    public String getPermissionDescription() {
        return context.getString(R.string.wizard_descr);
    }

}

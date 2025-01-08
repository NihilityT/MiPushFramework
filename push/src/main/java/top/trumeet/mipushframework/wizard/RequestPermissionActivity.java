package top.trumeet.mipushframework.wizard;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.setupwizardlib.view.NavigationBar;
import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.utils.PermissionUtils;
import top.trumeet.mipushframework.wizard.permission.PermissionInfo;
import top.trumeet.mipushframework.wizard.permission.PermissionInfoFactory;
import top.trumeet.mipushframework.wizard.permission.PermissionOperator;

public class RequestPermissionActivity extends PushControllerWizardActivity implements NavigationBar.NavigationBarListener {
    private PermissionOperator permissionOperator;
    private boolean nextClicked = false;
    private PermissionInfo permissionInfo;

    public static @NonNull Intent intentFor(Context context, Class<? extends PermissionInfo> permissionInfoClass) {
        return PermissionInfoFactory.bindPermissionInfo(
                new Intent(context, RequestPermissionActivity.class),
                permissionInfoClass);
    }

    @NonNull
    protected PermissionOperator getPermissionOperator() {
        return permissionInfo.getPermissionOperator();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            nextPage();
            finish();
            return;
        }
        connect();
        permissionInfo = PermissionInfoFactory.createFrom(getIntent(), this);
        permissionOperator = getPermissionOperator();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (permissionOperator.isPermissionGranted()) {
            nextPage();
            finish();
        } else if (nextClicked) {
            layout.getNavigationBar()
                    .getNextButton()
                    .setText(R.string.retry);
        }
    }

    @Override
    public void onConnected(Bundle savedInstanceState) {
        super.onConnected(savedInstanceState);

        if (permissionOperator.isPermissionGranted()) {
            nextPage();
            finish();
            return;
        }
        layout.getNavigationBar()
                .setNavigationBarListener(this);
        mText.setText(Html.fromHtml(getPermissionDescription()));
        layout.setHeaderText(Html.fromHtml(getPermissionTitle()));
        setContentView(layout);
    }

    @NonNull
    protected String getPermissionTitle() {
        return permissionInfo.getPermissionTitle();
    }

    @NonNull
    public String getPermissionDescription() {
        return permissionInfo.getPermissionDescription();
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        nextClicked = true;
        if (!permissionOperator.isPermissionGranted()) {
            if (PermissionUtils.canAssignPermissionViaAppOps()) {
                permissionOperator.requestPermissionSilently();
            } else {
                permissionOperator.requestPermission();
            }
        } else {
            nextPage();
        }
    }

    private void nextPage() {
        startActivity(nextPageIntent());
    }

    @NonNull
    protected Intent nextPageIntent() {
        return permissionInfo.nextPageIntent();
    }
}

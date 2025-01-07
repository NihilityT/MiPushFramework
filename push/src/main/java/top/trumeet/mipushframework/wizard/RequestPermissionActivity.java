package top.trumeet.mipushframework.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.setupwizardlib.view.NavigationBar;
import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.utils.PermissionUtils;

public abstract class RequestPermissionActivity extends PushControllerWizardActivity implements NavigationBar.NavigationBarListener {
    private final PermissionOperator permissionOperator = getPermissionOperator();
    private boolean nextClicked = false;

    @NonNull
    protected abstract PermissionOperator getPermissionOperator();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            nextPage();
            finish();
            return;
        }
        connect();
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
    protected abstract String getPermissionTitle();

    @NonNull
    public abstract String getPermissionDescription();

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
        startActivity(new Intent(this,
                nextPageClass()));
    }

    @NonNull
    protected abstract Class<? extends Activity> nextPageClass();
}

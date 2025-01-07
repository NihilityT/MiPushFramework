package top.trumeet.mipushframework.wizard;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.android.setupwizardlib.view.NavigationBar;
import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.utils.PermissionUtils;

/**
 * Created by Trumeet on 2017/8/25.
 *
 * @author Trumeet
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class AlertWindowPermissionActivity extends PushControllerWizardActivity implements NavigationBar.NavigationBarListener {
    private final AlertWindowPermissionOperator permissionOperator = new AlertWindowPermissionOperator(this);
    private boolean nextClicked = false;

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
        mText.setText(Html.fromHtml(getString(R.string.wizard_title_alert_window_text)));
        layout.setHeaderText(Html.fromHtml(getString(R.string.wizard_title_alert_window_permission)));
        setContentView(layout);
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
        startActivity(new Intent(this,
                FinishWizardActivity.class));
    }

}

package top.trumeet.mipushframework.permissions;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.xiaomi.xmsf.R;

import io.reactivex.disposables.CompositeDisposable;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;
import top.trumeet.mipush.provider.register.RegisteredApplication.RegisteredType;
import top.trumeet.mipushframework.control.CheckPermissionsUtils;

/**
 * Created by Trumeet on 2017/8/27.
 *
 * @author Trumeet
 */

public class ManagePermissionsActivity extends AppCompatActivity {
    private static final String TAG = ManagePermissionsActivity.class.getSimpleName();

    public static final String EXTRA_PACKAGE_NAME = ManagePermissionsActivity.class.getName() + ".EXTRA_PACKAGE_NAME";

    public static final String EXTRA_IGNORE_NOT_REGISTERED = ManagePermissionsActivity.class.getName() + ".EXTRA_IGNORE_NOT_REGISTERED";

    private CompositeDisposable composite = new CompositeDisposable();

    private LoadApplicationInfoTask mTask;
    private String mPkg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null && getIntent().hasExtra(EXTRA_PACKAGE_NAME)) {
            mPkg = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            checkAndStart();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void checkAndStart() {
        composite.add(CheckPermissionsUtils.checkAndRun(result -> {
            switch (result) {
                case OK:
                    mTask = new LoadApplicationInfoTask(mPkg);
                    mTask.execute();
                    break;
                case PERMISSION_NEEDED:
                    Toast.makeText(this, getString(top.trumeet.common.R.string.request_permission), Toast.LENGTH_LONG).show();
                    // Restart to request permissions again.
                    checkAndStart();
                    break;
                case PERMISSION_NEEDED_SHOW_SETTINGS:
                    Toast.makeText(this, getString(top.trumeet.common.R.string.request_permission), Toast.LENGTH_LONG).show();
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                case REMOVE_DOZE_NEEDED:
                    Toast.makeText(this, getString(R.string.request_battery_whitelist), Toast.LENGTH_LONG).show();
                    checkAndStart();
                    break;
            }
        }, throwable -> {
            Log.e(TAG, "check permissions", throwable);
        }, this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mTask != null && !mTask.isCancelled()) {
            mTask.cancel(true);
            mTask = null;
        }
        // Activity request should cancel in onPause?
        if (composite != null && !composite.isDisposed()) {
            composite.dispose();
        }
        super.onDestroy();
    }

    private class LoadApplicationInfoTask extends AsyncTask<Void, Void, RegisteredApplication> {
        private String pkg;
        private CancellationSignal mSignal;

        LoadApplicationInfoTask(String pkg) {
            this.pkg = pkg;
        }

        @Override
        protected RegisteredApplication doInBackground(Void... voids) {
            mSignal = new CancellationSignal();
            RegisteredApplication application = RegisteredApplicationDb.getRegisteredApplication(pkg);

            if (application == null && getIntent().getBooleanExtra(EXTRA_IGNORE_NOT_REGISTERED, false)) {
                application = new RegisteredApplication();
                application.setPackageName(pkg);
                application.setRegisteredType(RegisteredType.NotRegistered);
            }
            return application;
        }

        @Override
        protected void onPostExecute(RegisteredApplication application) {
            if (application != null) {
                ManagePermissionsFragment fragment = new ManagePermissionsFragment(application);
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitAllowingStateLoss();
            }
        }

        @Override
        protected void onCancelled() {
            if (mSignal != null) {
                if (!mSignal.isCanceled()) mSignal.cancel();
                mSignal = null;
            }
        }
    }

}

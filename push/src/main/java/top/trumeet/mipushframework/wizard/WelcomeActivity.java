package top.trumeet.mipushframework.wizard;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.settings.MainActivity;

/**
 * Created by Trumeet on 2017/8/24.
 * Wizard welcome page
 */

public class WelcomeActivity extends AppCompatActivity implements NavigationBar.NavigationBarListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isPermissionRequested()) {
            jumpToMainActivity();
            finish();
            return;
        }
        setContentView(createWelcomeContent());
    }

    private @NonNull SetupWizardLayout createWelcomeContent() {
        SetupWizardLayout layout = new SetupWizardLayout(this);
        layout.getNavigationBar()
                .setNavigationBarListener(this);
        layout.addView(createDescriptionTextView());
        layout.setHeaderText(R.string.app_name);
        return layout;
    }

    private void jumpToMainActivity() {
        startActivity (new Intent(this,
                MainActivity.class));
    }

    private boolean isPermissionRequested() {
        return !WizardSPUtils.shouldShowWizard(this);
    }

    private @NonNull TextView createDescriptionTextView() {
        TextView descriptionTextView = new TextView(this);
        descriptionTextView.setText(Html.fromHtml(getString(R.string.wizard_descr)));
        int padding = (int) getResources().getDimension(R.dimen.suw_glif_margin_sides);
        descriptionTextView.setPadding(padding, padding, padding, padding);
        descriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());
        return descriptionTextView;
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        startActivity(new Intent(this, RequestPermissionPage.class));
    }
}

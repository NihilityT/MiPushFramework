package top.trumeet.mipushframework.wizard;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import top.trumeet.mipushframework.main.MainPage;

/**
 * Created by Trumeet on 2017/8/24.
 * Wizard welcome page
 */

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (WizardSPUtils.shouldShowWizard(this)) {
            jumpToRequestPermissionPage();
        } else {
            jumpToMainActivity();
        }
        finish();
    }

    private void jumpToRequestPermissionPage() {
        startActivity(new Intent(this, RequestPermissionPage.class));
    }

    private void jumpToMainActivity() {
        startActivity (new Intent(this,
                MainPage.class));
    }
}

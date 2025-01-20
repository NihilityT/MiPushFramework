package top.trumeet.mipushframework;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.main.HelpActivity;

public class MainPageOperation {
    private final Context context;

    public MainPageOperation(Context context) {
        this.context = context;
    }

    void gotoHelpActivity() {
        Intent intent = new Intent();
        intent.setClass(context, HelpActivity.class);
        context.startActivity(intent);
    }

    void showAboutDialog() {
        String versionInfo = String.format("name: %s\ncode: %d\nflavor: %s\ntype: %s",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.FLAVOR,
                BuildConfig.BUILD_TYPE);
        AlertDialog.Builder build = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_about)
                .setPositiveButton("Copy", (dialogInterface, i) -> {
                    ClipboardManager clipboardManager = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(versionInfo);
                });
        TextView content = build.show().findViewById(R.id.text_version);
        content.setText(versionInfo);
    }

    void gotoGitHubReleasePage() {
        context.startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://github.com/NihilityT/MiPushFramework/releases")));
    }
}
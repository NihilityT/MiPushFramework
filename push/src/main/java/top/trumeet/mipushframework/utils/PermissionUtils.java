/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.trumeet.mipushframework.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.xiaomi.xmsf.R;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.Utils;

public class PermissionUtils {
    public static boolean canAssignPermissionViaAppOps() {
        return Utils.isAppOpsInstalled() || ShellUtils.isSuAvailable();
    }

    public static boolean lunchAppOps(Context context, String permission, CharSequence tips) {
        // root first
        if (ShellUtils.isSuAvailable()) {
            if (allowPermission(permission)) {
                return true;
            } else {
                Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show();
            }
        }

        if (Utils.isAppOpsInstalled()) {
            Intent intent = new Intent(Intent.ACTION_SHOW_APP_INFO)
                    .setClassName("rikka.appops", "rikka.appops.appdetail.AppDetailActivity")
                    .putExtra("rikka.appops.intent.extra.USER_HANDLE", Utils.myUid())
                    .putExtra("rikka.appops.intent.extra.PACKAGE_NAME", Constants.SERVICE_APP_NAME)
                    .setData(Uri.parse("package:" + Constants.SERVICE_APP_NAME))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Toast.makeText(context, tips, Toast.LENGTH_LONG).show();
            return true;
        }

        return false;
    }

    public static boolean allowPermission(String permission) {
        return ShellUtils.exec("appops set --user " + Utils.myUid() +
                " " + Constants.SERVICE_APP_NAME + " " + permission +
                " " + AppOpsManager.MODE_ALLOWED);
    }
}

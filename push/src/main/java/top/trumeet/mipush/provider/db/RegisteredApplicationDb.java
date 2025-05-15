package top.trumeet.mipush.provider.db;

import static top.trumeet.common.BuildConfig.DEBUG;
import static top.trumeet.mipush.provider.DatabaseUtils.daoSession;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.gen.db.RegisteredApplicationDao;
import top.trumeet.mipush.provider.entities.RegisteredApplication;

/**
 * Created by Trumeet on 2017/12/23.
 */

public class RegisteredApplicationDb {

    @NonNull
    public static RegisteredApplication registerApplication(String pkg) {
        RegisteredApplication registeredApplication = getRegisteredApplication(pkg);
        if (registeredApplication == null) {
            return create(pkg);
        }
        return registeredApplication;
    }

    @Nullable
    public static RegisteredApplication getRegisteredApplication(String pkg) {
        List<RegisteredApplication> list = getList(pkg);
        if (DEBUG) {
            Log.d("RegisteredApplicationDb", "register -> existing list = " + list.toString());
        }
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    private static @NonNull RegisteredApplication create(String pkg) {
        // TODO: Configurable defaults; use null for optional and global options?
        RegisteredApplication registeredApplication =
                new RegisteredApplication(null
                        , pkg
                        , RegisteredApplication.Type.ASK
                        , true
                        , false
                        , false
                        , false
                        , RegisteredApplication.RegisteredType.NotRegistered
                        , ApplicationNameCache.getInstance()
                        .getAppName(Utils.getApplication(), pkg).toString()

                );
        registeredApplication.setId(insert(registeredApplication));
        return registeredApplication;
    }

    public static List<RegisteredApplication> getList(@Nullable String pkg) {
        QueryBuilder<RegisteredApplication> query = daoSession.queryBuilder(RegisteredApplication.class);
        if (!TextUtils.isEmpty(pkg)) {
            query.where(RegisteredApplicationDao.Properties.PackageName.eq(pkg));
        }
        return query.list();
    }

    public static long update(RegisteredApplication application) {
        daoSession.insertOrReplace(application);
        return application.getId();
    }

    private static long insert(RegisteredApplication application) {
        return daoSession.insert(application);
    }
}

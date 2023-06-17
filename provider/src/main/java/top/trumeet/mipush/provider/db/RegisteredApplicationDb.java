package top.trumeet.mipush.provider.db;

import static top.trumeet.common.BuildConfig.DEBUG;
import static top.trumeet.mipush.provider.DatabaseUtils.daoSession;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.gen.db.RegisteredApplicationDao;
import top.trumeet.mipush.provider.register.RegisteredApplication;

/**
 * Created by Trumeet on 2017/12/23.
 */

public class RegisteredApplicationDb {

    public static RegisteredApplication registerApplication(String pkg, boolean autoCreate) {
        List<RegisteredApplication> list = getList(pkg);
        if (DEBUG) {
            Log.d("RegisteredApplicationDb", "register -> existing list = " + list.toString());
        }
        if (!list.isEmpty()) {
            return list.get(0);
        }
        if (autoCreate) {
            return create(pkg);
        }
        return null;
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


    private static RegisteredApplication create(String pkg) {
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
        insert(registeredApplication);

        // Very bad
        return registerApplication(pkg, false);
    }


    private static long insert(RegisteredApplication application) {
        return daoSession.insert(application);
    }
}

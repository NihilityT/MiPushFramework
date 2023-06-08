package top.trumeet.mipush.provider.db;

import static top.trumeet.common.BuildConfig.DEBUG;
import static top.trumeet.mipush.provider.DatabaseUtils.daoSession;

import android.content.Context;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import top.trumeet.mipush.provider.gen.db.RegisteredApplicationDao;
import top.trumeet.mipush.provider.register.RegisteredApplication;

/**
 * Created by Trumeet on 2017/12/23.
 */

public class RegisteredApplicationDb {

    public static RegisteredApplication
    registerApplication (String pkg, boolean autoCreate, Context context,
                         CancellationSignal signal) {
        List<RegisteredApplication> list = getList(context, pkg, signal);
        if (DEBUG) {
            Log.d("RegisteredApplicationDb", "register -> existing list = " + list.toString());
        }
        return list.isEmpty() ?
                (autoCreate ? create(pkg, context) : null) : list.get(0);
    }


    public static List<RegisteredApplication>
    getList (Context context,
             @Nullable String pkg,
             CancellationSignal signal) {
        QueryBuilder<RegisteredApplication> query = daoSession.queryBuilder(RegisteredApplication.class);
        if (!TextUtils.isEmpty(pkg)) {
            query.where(RegisteredApplicationDao.Properties.PackageName.eq(pkg));
        }
        return query.list();
    }


    public static long update (RegisteredApplication application,
                               Context context) {
        daoSession.update(application);
        return application.getId();
    }


    private static RegisteredApplication create (String pkg,
                                                 Context context) {
        // TODO: Configurable defaults; use null for optional and global options?
        RegisteredApplication registeredApplication =
                new RegisteredApplication(null
                        , pkg
                        , RegisteredApplication.Type.ASK
                        , true
                        , false
                        , false
                        , false
                );
        insert(registeredApplication, context);

        // Very bad
        return registerApplication(pkg, false, context,
                null);
    }


    private static long insert (RegisteredApplication application,
                                Context context) {
        return daoSession.insert(application);
    }
}

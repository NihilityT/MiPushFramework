package top.trumeet.mipushframework.register;

import static top.trumeet.common.Constants.TAG;

import android.app.SearchManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.promeg.pinyinhelper.Pinyin;
import com.xiaomi.xmsf.R;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.drakeet.multitype.Items;
import me.drakeet.multitype.MultiTypeAdapter;
import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;
import top.trumeet.mipushframework.utils.MiPushManifestChecker;
import top.trumeet.mipushframework.widgets.Footer;
import top.trumeet.mipushframework.widgets.FooterItemBinder;

/**
 * Created by Trumeet on 2017/8/26.
 *
 * @author Trumeet
 */

public class RegisteredApplicationFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private MultiTypeAdapter mAdapter;
    private LoadTask mLoadTask;
    private String mQuery = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new MultiTypeAdapter();
        mAdapter.register(RegisteredApplication.class, new RegisteredApplicationBinder());
        mAdapter.register(Footer.class, new FooterItemBinder());
        setHasOptionsMenu(true);
    }

    SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView view = new RecyclerView(getActivity());
        view.setLayoutManager(new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false));
        view.setAdapter(mAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(view.getContext(),
                LinearLayoutManager.VERTICAL);
        view.addItemDecoration(dividerItemDecoration);


        swipeRefreshLayout = new SwipeRefreshLayout(getActivity());
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.addView(view);

        loadPage();
        return swipeRefreshLayout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.action_enable).setVisible(false);
        menu.findItem(R.id.action_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);

        initSearchBar(searchItem);
    }

    private void initSearchBar(MenuItem searchItem) {
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.equals(mQuery)) {
                    return true;
                }
                mQuery = newText.toLowerCase();
                onRefresh();
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String newText) {
                return true;
            }
        });
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPage();
    }

    private void loadPage() {
        Log.d(TAG, "loadPage");
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        mLoadTask = new LoadTask(getActivity());
        mLoadTask.execute();
    }

    @Override
    public void onDetach() {
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            mLoadTask.cancel(true);
            mLoadTask = null;
        }
        super.onDetach();
    }

    @Override
    public void onRefresh() {
        loadPage();

    }

    private class LoadTask extends AsyncTask<Integer, Void, LoadTask.Result> {
        private CancellationSignal mSignal;

        public LoadTask(Context context) {
            this.context = context;
        }

        private Context context;

        class Result {
            private final int notUseMiPushCount;
            private final List<RegisteredApplication> list;

            public Result(int notUseMiPushCount, List<RegisteredApplication> list) {
                this.notUseMiPushCount = notUseMiPushCount;
                this.list = list;
            }
        }

        @Override
        protected Result doInBackground(Integer... integers) {
            // TODO: Sharing/Modular actuallyRegisteredPkgs to doInBackground of ManagePermissionsActivity.java
            mSignal = new CancellationSignal();

            Map<String /* pkg */, RegisteredApplication> registeredPkgs = new HashMap<>();
            for (RegisteredApplication application : RegisteredApplicationDb.getList(context, null, mSignal)) {
                registeredPkgs.put(application.getPackageName(), application);
            }
            Set<String> actuallyRegisteredPkgs = EventDb.queryRegistered(context, mSignal);

            MiPushManifestChecker checker = null;
            try {
                checker = MiPushManifestChecker.create(context);
            } catch (PackageManager.NameNotFoundException | ClassNotFoundException | NoSuchMethodException e) {
                Log.e(RegisteredApplicationFragment.class.getSimpleName(), "Create mi push checker", e);
            }

            List<RegisteredApplication> res = new Vector<>();

            int threadCount = Runtime.getRuntime().availableProcessors();
            ExecutorService pool = new ThreadPoolExecutor(
                    threadCount,
                    threadCount * 2,
                    1,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.CallerRunsPolicy());

            final List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES
                    | PackageManager.GET_DISABLED_COMPONENTS | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS);
            for (final Iterator<PackageInfo> iterator = packageInfos.iterator(); iterator.hasNext(); )
                if ((iterator.next().applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0) iterator.remove();   // Exclude apps not installed in current user.

            int totalPkg = packageInfos.size();
            for (PackageInfo packageInfo : packageInfos) {
                final PackageInfo info = packageInfo;
                MiPushManifestChecker finalChecker = checker;

                pool.submit(() -> {
                    final String appName = ApplicationNameCache.getInstance()
                            .getAppName(context, info.packageName).toString();
                    if (!(info.packageName.toLowerCase().contains(mQuery) ||
                            appName.toLowerCase().contains(mQuery) ||
                            Pinyin.toPinyin(appName, "").contains(mQuery)
                    )) {
                        return;
                    }
                    String currentAppPkgName = info.packageName;
                    if (info.services == null) info.services = new ServiceInfo[]{};

                    RegisteredApplication application = null;
                    boolean existServices = finalChecker != null && finalChecker.checkServices(info);
                    if (registeredPkgs.containsKey(currentAppPkgName)) {
                        application = registeredPkgs.get(currentAppPkgName);
                        application.setRegisteredType(actuallyRegisteredPkgs.contains(currentAppPkgName) ? 1 : 2);
                        res.add(application);
                    } else if (existServices) {
                        // checkReceivers will use Class#forName, but we can't change our classloader to target app's.
                        application = new RegisteredApplication();
                        application.setPackageName(currentAppPkgName);
                        application.setRegisteredType(0);
                        res.add(application);
                    } else {
                        Log.d(TAG, "not use mipush : " + currentAppPkgName);
                    }
                    if (application != null) {
                        application.existServices = existServices;
                        application.appName = appName;
                        application.lastReceiveTime = new Date(EventDb.getLastReceiveTime(application.getPackageName()));
                    }
                });
            }

            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            Collections.sort(res, (o1, o2) -> {
                final String o1Name = Pinyin.toPinyin(o1.appName,"");
                final String o2Name = Pinyin.toPinyin(o2.appName,"");
                if (o1.getId() == null && o2.getId() == null) {
                    return o1Name.compareTo(o2Name);
                }

                if (o1.getId() == null) {
                    return 1;
                }

                if (o2.getId() == null) {
                    return -1;
                }

                if (o1.getRegisteredType() == o2.getRegisteredType()) {
                    return o1Name.compareTo(o2Name);
                } else {
                    return o1.getRegisteredType() - o2.getRegisteredType();

                }

            });
            int notUseMiPushCount = totalPkg - registeredPkgs.size();

            return new Result(notUseMiPushCount, res);
        }

        @Override
        protected void onPostExecute(Result result) {
            mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount());
            mAdapter.getItems().clear();

            int start = mAdapter.getItemCount();
            Items items = new Items(mAdapter.getItems());
            items.addAll(result.list);
            if (result.notUseMiPushCount > 0) {
                items.add(new Footer(getString(R.string.footer_app_ignored_not_registered, Integer.toString(result.notUseMiPushCount))));
            }
            mAdapter.setItems(items);
            mAdapter.notifyItemRangeInserted(start, result.notUseMiPushCount > 0 ? result.list.size() + 1 : result.list.size());

            swipeRefreshLayout.setRefreshing(false);
            mLoadTask = null;
        }

        @Override
        protected void onCancelled() {
            if (mSignal != null) {
                if (!mSignal.isCanceled()) {
                    mSignal.cancel();
                }
                mSignal = null;
            }

            swipeRefreshLayout.setRefreshing(false);
            mLoadTask = null;
        }
    }
}

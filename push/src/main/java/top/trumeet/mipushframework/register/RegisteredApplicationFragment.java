package top.trumeet.mipushframework.register;

import static top.trumeet.common.Constants.TAG;

import android.app.SearchManager;
import android.content.Context;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.xiaomi.xmsf.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.drakeet.multitype.Items;
import me.drakeet.multitype.MultiTypeAdapter;
import top.trumeet.mipush.provider.register.RegisteredApplication;
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
    private ExecutorService updateThread = Executors.newSingleThreadExecutor();

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
            ApplicationPageOperation.MiPushApplications miPushApplications =
                    ApplicationPageOperation.getMiPushApplicationsThatQueryMatched(RegisteredApplicationFragment.this.mQuery);

            int notUseMiPushCount = miPushApplications.totalPkg - miPushApplications.res.size();
            return new Result(notUseMiPushCount, miPushApplications.res);
        }

        @Override
        protected void onPostExecute(Result result) {
            mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount());
            mAdapter.getItems().clear();

            int start = mAdapter.getItemCount();
            Items items = new Items(mAdapter.getItems());
            items.addAll(result.list);
            if (result.notUseMiPushCount > 0) {
                items.add(new Footer(ApplicationPageOperation.getNotSupportHint(context, result.notUseMiPushCount)));
            }
            mAdapter.setItems(items);
            mAdapter.notifyItemRangeInserted(start, result.notUseMiPushCount > 0 ? result.list.size() + 1 : result.list.size());

            swipeRefreshLayout.setRefreshing(false);
            mLoadTask = null;

            updateApplicationInfo(result.list);
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

        private void updateApplicationInfo(List<RegisteredApplication> list) {
            updateThread.execute(() -> {
                ApplicationPageOperation.updateRegisteredApplicationDb(context, list);
            });
        }

    }

}

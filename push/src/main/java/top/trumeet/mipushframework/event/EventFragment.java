package top.trumeet.mipushframework.event;

import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import com.xiaomi.xmsf.R;

import me.drakeet.multitype.Items;
import me.drakeet.multitype.MultiTypeAdapter;
import top.trumeet.common.db.EventDb;
import top.trumeet.common.event.Event;
import top.trumeet.mipushframework.utils.OnLoadMoreListener;

/**
 * Created by Trumeet on 2017/8/26.
 * 
 * @author Trumeet
 */

public class EventFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String EXTRA_TARGET_PACKAGE = EventFragment.class.getName() + ".EXTRA_TARGET_PACKAGE";
    private static final String EXTRA_QUERY = EventFragment.class.getName() + ".EXTRA_QUERY";

    private MultiTypeAdapter mAdapter;
    private static final String TAG = EventFragment.class.getSimpleName();

    /**
     * Already load page
     */
    private int mLoadPage;
    private String mPacketName = null;
    private String mQuery = null;
    private LoadTask mLoadTask;

    public static EventFragment newInstance(String targetPackage) {
        EventFragment fragment = new EventFragment();
        fragment.mPacketName = targetPackage;
        return fragment;
    }

    void setQuery(String query) {
        mQuery = query;
        if (mQuery != null && mQuery.length() == 0) mQuery = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPacketName = savedInstanceState.getString(EXTRA_TARGET_PACKAGE);
            mQuery = savedInstanceState.getString(EXTRA_QUERY);
        }
        setHasOptionsMenu(true);
        mAdapter = new MultiTypeAdapter();
        mAdapter.register(Event.class, new EventItemBinder());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_TARGET_PACKAGE, mPacketName);
        outState.putString(EXTRA_QUERY, mQuery);
    }

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        RecyclerView view = new RecyclerView(getActivity());
        view.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        view.setAdapter(mAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(view.getContext(),
                LinearLayoutManager.VERTICAL);
        view.addItemDecoration(dividerItemDecoration);
        view.addOnScrollListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                loadPage();
            }
        });

        swipeRefreshLayout = new SwipeRefreshLayout(getActivity());
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.addView(view);
        return swipeRefreshLayout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mPacketName != null) {
            inflater.inflate(R.menu.menu_main, menu);
            menu.findItem(R.id.action_enable).setActionView(R.layout.switch_layout);
        }
        menu.findItem(R.id.action_enable).setVisible(false);
        menu.findItem(R.id.action_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);

        SearchView searchView = (SearchView) searchItem.getActionView();
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                setQuery(newText);
                cancelPage();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            ((SearchView) item.getActionView()).setIconified(false);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPage();
    }

    private void loadPage() {
        Log.d(TAG, "loadPage");
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            if (mLoadTask.getStatus() != AsyncTask.Status.FINISHED) {
                return;
            }
        }
        mLoadTask = new LoadTask(mLoadPage + 1);
        mLoadTask.execute();
    }

    private void cancelPage() {
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            mLoadTask.cancel(true);
            mLoadTask = null;
        }
    }

    @Override
    public void onDetach() {
        cancelPage();
        super.onDetach();
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "refreshPage");
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            if (mLoadTask.getStatus() != AsyncTask.Status.FINISHED) {
                return;
            }
        }
        mLoadTask = new LoadTask(1);
        mLoadTask.execute();
    }

    private class LoadTask extends AsyncTask<Integer, Void, List<Event>> {
        private int mTargetPage;
        private CancellationSignal mSignal;

        LoadTask(int page) {
            mTargetPage = page;
        }

        @Override
        protected List<Event> doInBackground(Integer... integers) {
            mSignal = new CancellationSignal();
            return EventDb.query(mPacketName, mQuery, mTargetPage, getActivity(), mSignal);
        }

        @Override
        protected void onPostExecute(List<Event> list) {
            if (mTargetPage == 1) {
                mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount());
                mAdapter.getItems().clear();
            }

            appendItemToAdapter(list);
            mLoadPage = mTargetPage;

            swipeRefreshLayout.setRefreshing(false);
            mLoadTask = null;
        }

        private void appendItemToAdapter(List<Event> list) {
            int start = mAdapter.getItemCount();
            Items items = new Items(mAdapter.getItems());
            items.addAll(list);
            mAdapter.setItems(items);
            mAdapter.notifyItemRangeInserted(start, list.size());
        }

        @Override
        protected void onCancelled() {
            if (mSignal != null) {
                if (!mSignal.isCanceled()) {
                    mSignal.cancel();
                }
                mSignal = null;
            }
        }
    }
}

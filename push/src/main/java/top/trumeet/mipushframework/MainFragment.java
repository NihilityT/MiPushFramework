package top.trumeet.mipushframework;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.control.FragmentBroadcast;
import top.trumeet.mipushframework.control.OnConnectStatusChangedListener;
import top.trumeet.mipushframework.event.EventFragment;
import top.trumeet.mipushframework.help.HelpActivity;
import top.trumeet.mipushframework.register.RegisteredApplicationFragment;
import top.trumeet.mipushframework.settings.SettingsFragment;

/**
 * @author Trumeet
 * @date 2017/12/30
 */

public class MainFragment extends Fragment implements OnConnectStatusChangedListener {

    private FragmentBroadcast mBroadcaster;

    private static final String FRAGMENT_EVENT = "event";
    private static final String FRAGMENT_APPLICATIONS = "applications";
    private static final String FRAGMENT_SETTINGS = "settings";
    private MenuItem mSearchItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBroadcaster = new FragmentBroadcast();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            String versionInfo = String.format("name: %s\ncode: %d\nflavor: %s\ntype: %s",
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.FLAVOR,
                    BuildConfig.BUILD_TYPE);
            AlertDialog.Builder build = new AlertDialog.Builder(getContext())
                    .setView(R.layout.dialog_about)
                    .setPositiveButton("Copy", (dialogInterface, i) -> {
                        ClipboardManager clipboardManager = (ClipboardManager)
                                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboardManager.setText(versionInfo);
                    });
            TextView content = build.show().findViewById(R.id.text_version);
            content.setText(versionInfo);
            return true;
        } else if (item.getItemId() == R.id.action_update) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://github.com/NihilityT/MiPushFramework/releases")));
            Toast.makeText(getActivity(), R.string.update_toast, Toast.LENGTH_LONG).show();
        } else if (item.getItemId() == R.id.action_help) {
            Intent intent = new Intent();
            intent.setClass(this.getContext(), HelpActivity.class);
            startActivity(intent);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_enable);
        item.setActionView(R.layout.switch_layout);
        mSearchItem = menu.findItem(R.id.action_search);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, parent, false);

        final BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_nav);
        {
            MenuItem menu = bottomNavigationView.getMenu().getItem(0);
            menu.setTitle(menu.getTitle() + "(" + BuildConfig.VERSION_CODE + ")");
        }
        {
            MenuItem menu = bottomNavigationView.getMenu().getItem(1);
            menu.setTitle(menu.getTitle() + "(" + BuildConfig.VERSION_NAME + ")");
        }
        {
            MenuItem menu = bottomNavigationView.getMenu().getItem(2);
            menu.setTitle(menu.getTitle() + "(" + BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE + ")");
        }


        final ViewPager viewPager = view.findViewById(R.id.viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(0).setChecked(false);
                bottomNavigationView.getMenu().getItem(1).setChecked(false);
                bottomNavigationView.getMenu().getItem(2).setChecked(false);
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        if (mBroadcaster.hasFragment(FRAGMENT_EVENT)) {
                            return mBroadcaster.getFragment(FRAGMENT_EVENT);
                        }
                        EventFragment eventFragment = new EventFragment();
                        mBroadcaster.registerFragment(FRAGMENT_EVENT, eventFragment);
                        return eventFragment;
                    case 1:
                        if (mBroadcaster.hasFragment(FRAGMENT_APPLICATIONS)) {
                            return mBroadcaster.getFragment(FRAGMENT_APPLICATIONS);
                        }
                        RegisteredApplicationFragment registeredApplicationFragment = new RegisteredApplicationFragment();
                        mBroadcaster.registerFragment(FRAGMENT_APPLICATIONS, registeredApplicationFragment);
                        return registeredApplicationFragment;
                    case 2:
                        if (mBroadcaster.hasFragment(FRAGMENT_SETTINGS)) {
                            return mBroadcaster.getFragment(FRAGMENT_SETTINGS);
                        }
                        SettingsFragment settingsFragment = new SettingsFragment();
                        mBroadcaster.registerFragment(FRAGMENT_SETTINGS, settingsFragment);
                        return settingsFragment;
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        SearchView searchView = (SearchView) mSearchItem.getActionView();
                        searchView.setIconified(true);
                        searchView.setIconified(true);
                        viewPager.setCurrentItem(item.getOrder());
                        return true;
                    }
                });
        ViewCompat.setElevation(bottomNavigationView, 8f);
        viewPager.setCurrentItem(1);
        return view;
    }

    @Override
    public void onChange(@Status int newStatus) {
        // Broadcast
        mBroadcaster.broadcast(newStatus);
    }

    @Override
    public void onDetach() {
        if (mBroadcaster != null) {
            mBroadcaster.unregisterAll();
        }
        super.onDetach();
    }
}

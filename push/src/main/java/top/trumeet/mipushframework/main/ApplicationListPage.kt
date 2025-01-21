package top.trumeet.mipushframework.main

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.elvishew.xlog.XLog
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.utils.Utils
import top.trumeet.mipush.provider.register.RegisteredApplication
import top.trumeet.mipushframework.event.EventItemBinder
import top.trumeet.mipushframework.utils.ParseUtils
import top.trumeet.ui.theme.Theme
import java.util.Locale

class ApplicationListPage : Fragment() {
    private var query by mutableStateOf("")
    private lateinit var iconCache: ApplicationIconCache

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        return ComposeView(requireContext()).apply {
            setContent {
                ApplicationList {
                    val miPushApplications =
                        ApplicationPageOperation.getMiPushApplicationsThatQueryMatched(query)
                    ApplicationPageOperation.updateRegisteredApplicationDb(
                        context,
                        miPushApplications.res
                    )
                    miPushApplications
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_enable).setVisible(false)
        menu.findItem(R.id.action_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        val searchItem = menu.findItem(R.id.action_search)
        searchItem.setVisible(true)

        initSearchBar(searchItem)
    }


    private fun initSearchBar(searchItem: MenuItem) {
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = searchItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText != query) {
                    query = newText.lowercase(Locale.getDefault())
                }
                return true
            }

            override fun onQueryTextSubmit(newText: String): Boolean {
                return true
            }
        })
    }


    @Composable
    fun ApplicationList(getMiPushApplications: () -> ApplicationPageOperation.MiPushApplications) {
        val context = LocalContext.current
        if (!::iconCache.isInitialized) {
            iconCache = ApplicationIconCache(context)
        }
        val isPreview = LocalInspectionMode.current
        var items by remember {
            mutableStateOf(
                if (isPreview) getMiPushApplications()
                else ApplicationPageOperation.MiPushApplications()
            )
        }
        val notUseMiPushCount by remember { derivedStateOf { items.totalPkg - items.res.size } }
        var isRefreshing by remember { mutableStateOf(false) }

        val refreshScope = rememberCoroutineScope { Dispatchers.IO }
        val onRefresh: () -> Unit = {
            isRefreshing = true
            refreshScope.launch {
                items = getMiPushApplications()
                isRefreshing = false
                items.res.forEach { iconCache.cache(it) }
            }
        }
        LaunchedEffect(query) { onRefresh() }

        Theme {
            Surface {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = onRefresh
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(items.res, { it.packageName }) {
                            ApplicationItem(it)
                        }
                        item {
                            Footer(notUseMiPushCount)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Footer(notUseMiPushCount: Int) {
        val context = LocalContext.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_info_outline_black_24dp),
                null,
                tint = Color(0xFF757575),
                modifier = Modifier.padding(10.dp)
            )
            Text(
                ApplicationPageOperation.getNotSupportHint(
                    context,
                    notUseMiPushCount
                )
            )
        }
    }

    @Composable
    private fun ApplicationItem(item: RegisteredApplication) {
        val context = LocalContext.current
        val isPreview = LocalInspectionMode.current
        var icon by remember {
            mutableStateOf(
                if (isPreview) iconCache.defaultAppIcon
                else iconCache.get(item) ?: iconCache.defaultAppIcon
            )
        }
        val registrationState = RegistrationStateStyle.contentOf(item)

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                iconCache.cache(item)?.let { icon = it }
            }
        }

        Row(
            Modifier
                .clickable {
                    EventItemBinder.startManagePermissions(
                        context,
                        item.packageName,
                        true
                    )
                }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(icon, item.appName, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(20.dp))
            Column {
                AppInfo(item, registrationState)
                LastReceive(item)
            }
        }
    }


    @Composable
    private fun LastReceive(item: RegisteredApplication) {
        val context = LocalContext.current
        Text(
            if (item.lastReceiveTime.time == 0L) ""
            else stringResource(R.string.last_receive) + ParseUtils.getFriendlyDateString(
                item.lastReceiveTime,
                Utils.getUTC(),
                context
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    @Composable
    private fun AppInfo(
        item: RegisteredApplication,
        registrationState: Pair<String, Color>
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                item.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = registrationState.second
            )
            Text(
                registrationState.first,
                style = MaterialTheme.typography.bodyMedium,
                color = registrationState.second
            )
        }
    }


    @Preview(
        showBackground = true,
        device = Devices.PIXEL_3,
        showSystemUi = true,
    )
    @Composable
    fun ApplicationListPreview() {
        XLog.init()

        ApplicationList {
            val miPushApplications = ApplicationPageOperation.MiPushApplications()
            miPushApplications.res = listOf(
                registeredApplication(
                    RegisteredApplication.RegisteredType.NotRegistered,
                    "123"
                ),
                registeredApplication(
                    RegisteredApplication.RegisteredType.Registered,
                    "qwe"
                ),
                registeredApplication(
                    RegisteredApplication.RegisteredType.Registered,
                    "asd"
                ),
                registeredApplication(
                    RegisteredApplication.RegisteredType.Unregistered,
                    "zxc"
                ),
                registeredApplication(
                    RegisteredApplication.RegisteredType.Unregistered,
                    "456",
                    false
                ),
            ) + ('a'..'z').map {
                registeredApplication(
                    RegisteredApplication.RegisteredType.NotRegistered,
                    it.toString()
                )
            }

            miPushApplications
        }
    }

    @Preview(
        showBackground = true,
        device = Devices.PIXEL_3,
        showSystemUi = true,
    )
    @Composable
    fun OneApplicationWithNonMiPushAppPreview() {
        XLog.init()

        ApplicationList {
            val miPushApplications = ApplicationPageOperation.MiPushApplications()
            miPushApplications.res = listOf(
                registeredApplication(
                    RegisteredApplication.RegisteredType.NotRegistered,
                    "123"
                )
            )
            miPushApplications.totalPkg = 100
            miPushApplications
        }
    }

    private fun registeredApplication(
        registeredType: Int,
        appName: String,
        existServices: Boolean = true
    ): RegisteredApplication {
        val registeredApplication = RegisteredApplication(
            null,
            appName,
            RegisteredApplication.Type.ASK,
            true,
            false,
            false,
            false,
            registeredType,
            appName
        )
        registeredApplication.existServices = existServices
        return registeredApplication
    }

}
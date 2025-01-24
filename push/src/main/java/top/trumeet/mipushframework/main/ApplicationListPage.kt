package top.trumeet.mipushframework.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.utils.Utils
import top.trumeet.mipush.provider.register.RegisteredApplication
import top.trumeet.mipushframework.component.AppIcon
import top.trumeet.mipushframework.component.RefreshableLazyColumn
import top.trumeet.mipushframework.component.iconCache
import top.trumeet.mipushframework.component.mutableStateSaver
import top.trumeet.mipushframework.utils.ParseUtils

class ApplicationListPage : BaseListPage() {

    @Composable
    override fun ViewContent() {
        ApplicationList(query)
    }

}

@Composable
fun ApplicationList(query: String) {
    val context = LocalContext.current
    ApplicationList(query) {
        val miPushApplications =
            ApplicationPageOperation.getMiPushApplicationsThatQueryMatched(query)
        ApplicationPageOperation.updateRegisteredApplicationDb(
            context,
            miPushApplications.res
        )
        miPushApplications
    }
}

@Composable
fun ApplicationList(query: String = "", getMiPushApplications: () -> ApplicationPageOperation.MiPushApplications) {
    val isPreview = LocalInspectionMode.current
    var items by rememberSaveable(saver = mutableStateSaver(ApplicationPageOperation.MiPushApplications::class)) {
        mutableStateOf(
            if (isPreview) getMiPushApplications()
            else ApplicationPageOperation.MiPushApplications()
        )
    }
    var isNeedRefresh by rememberSaveable(query) { mutableStateOf(true) }

    val refreshScope = rememberCoroutineScope { Dispatchers.IO }
    val onRefresh: (onRefreshed: () -> Unit) -> Unit = { onRefreshed ->
        refreshScope.launch {
            val applications = getMiPushApplications()
            withContext(Dispatchers.Main) {
                items = applications
                isNeedRefresh = false
                onRefreshed()
            }
            applications.res.forEach { iconCache.cache(it.packageName) }
        }
    }

    Page {
        RefreshableLazyColumn(onRefresh, { items.res.isEmpty() }, onRefresh, isNeedRefresh) {
            items(items.res, { it.packageName }) {
                ApplicationItem(it)
            }
            item {
                val notUseMiPushCount by remember { derivedStateOf { items.totalPkg - items.res.size } }
                Footer(notUseMiPushCount)
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

    Row(
        Modifier
            .clickable {
                EventListPageUtils.startManagePermissions(
                    context,
                    item.packageName,
                    true
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(item.packageName, item.appName, Modifier.size(48.dp))
        Spacer(Modifier.width(20.dp))
        Column {
            AppInfo(item)
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
private fun AppInfo(item: RegisteredApplication) {
    val registrationState = RegistrationStateStyle.contentOf(item)
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
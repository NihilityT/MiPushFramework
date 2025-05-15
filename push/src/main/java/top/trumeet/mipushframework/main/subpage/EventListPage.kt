package top.trumeet.mipushframework.main.subpage

import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import com.xiaomi.xmsf.R
import com.xiaomi.xmsf.push.utils.RegSecUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.Constants
import top.trumeet.common.utils.Utils
import top.trumeet.mipush.provider.entities.Event
import top.trumeet.mipush.provider.event.type.TypeFactory
import top.trumeet.mipushframework.component.AppIcon
import top.trumeet.mipushframework.component.RefreshableLazyColumn
import top.trumeet.mipushframework.component.TextView
import java.text.SimpleDateFormat
import java.util.Date

private val receiveDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

@Composable
fun EventList(query: String = "", packageName: String = "") {
    Page {
        val context = LocalContext.current
        var clickedEvent by remember { mutableStateOf<EventInfoForDisplay?>(null) }

        clickedEvent?.let {
            EventDetailsDialog(clickedEvent!!) { clickedEvent = null }
        }

        var lastId by rememberSaveable { mutableStateOf<Long?>(null) }
        EventList(onClick = { clickedEvent = it }, { isRefresh ->
            if (isRefresh) lastId = null
            val events = EventListPageUtils.getEventsById(
                lastId, Constants.PAGE_SIZE, packageName, query
            )
            events.lastOrNull()?.let { lastId = it.id }
            events.map { toEventInfoForDisplay(it, context,
                EventListPageUtils(
                    context
                )
            ) }
        }, query, packageName)
    }
}

private fun toEventInfoForDisplay(
    it: Event,
    context: Context,
    utils: EventListPageUtils
): EventInfoForDisplay {
    val type = TypeFactory.createForDisplay(it)

    val container = RegSecUtils.getContainerWithRegSec(it)
    val summary = type.getSummary(context).toString()
    val content = if (container != null)
        EventListPageUtils.getDecoratedSummary(
            summary,
            container
        )
    else summary
    return EventInfoForDisplay(
        id = it.id,
        packageName = it.pkg,
        configOptions = utils.getStatus(container) ?: setOf(),
        channel = utils.getStatusDescription(it),
        receiveDate = Date(it.date),
        title = type.getTitle(context).toString(),
        content = content,
        appName = "",
        event = it,
    )
}

@Composable
private fun EventDetailsDialog(
    clickedEvent: EventInfoForDisplay,
    content: String? = null,
    onDismiss: () -> Unit
) {
    var json by remember {
        mutableStateOf(
            content ?: EventListPageUtils.getJson(
                clickedEvent.event
            ).toString()
        )
    }
    AlertDialog(
        onDismiss,
        {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val context = LocalContext.current
                TextButton({
                    json =
                        EventListPageUtils.getContent(
                            clickedEvent.event,
                            RegSecUtils.getContainerWithRegSec(clickedEvent.event)
                        )
                }) { Text(stringResource(R.string.action_configurate)) }

                TextButton({
                    EventListPageUtils.mockMessage(
                        RegSecUtils.getContainerWithRegSec(
                            clickedEvent.event
                        )
                    )
                }) { Text(stringResource(R.string.action_notify)) }

                TextButton({
                    EventListPageUtils.startManagePermissions(
                        context,
                        clickedEvent.packageName
                    )
                }) { Text(stringResource(R.string.action_app_info)) }
            }
        },
        title = { Text("Developer Info") },
        text = {
            TextView(json)
        }
    )
}

private val g_items = mutableStateListOf<EventInfoForDisplay>()

@Composable
fun EventList(
    onClick: (EventInfoForDisplay) -> Unit,
    getEvents: (isRefresh: Boolean) -> List<EventInfoForDisplay>,
    query: String,
    packageName: String
) {
    val isPreview = LocalInspectionMode.current
    val items = remember {
        if (isPreview) getEvents(true).toMutableList()
        else if (packageName.isEmpty()) g_items
        else mutableStateListOf()
    }


    val refreshScope = rememberCoroutineScope { Dispatchers.IO }
    val doLoadMore: (onRefreshed: () -> Unit) -> Unit = { onRefreshed ->
        refreshScope.launch {
            items.addAll(getEvents(items.isEmpty()))
            onRefreshed()
        }
    }
    var isNeedRefresh by rememberSaveable(query) { mutableStateOf(true) }
    val doRefresh: (onRefreshed: () -> Unit) -> Unit = { onRefreshed ->
        refreshScope.launch {
            val elements = getEvents(true)
            withContext(Dispatchers.Main) {
                items.clear()
                items.addAll(elements)
                isNeedRefresh = false
                onRefreshed()
            }
        }
    }

    val isNeedMore: (Int) -> Boolean = { it >= items.size - 10 }

    RefreshableLazyColumn(doRefresh, isNeedMore, doLoadMore, isNeedRefresh) {
        items(items, { it.id }) {
            EventItem(it, onClick)
        }
    }
}

@Composable
private fun EventItem(item: EventInfoForDisplay, onClick: (EventInfoForDisplay) -> Unit) {
    Row(
        Modifier
            .clickable { onClick(item) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(item.packageName, item.appName, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(20.dp))
        Column {
            Row {
                ConfigOptions(item)
                ChannelInfo(item)
                Spacer(Modifier.weight(1f))
                EventReceiveDate(item)
            }
            EventTitle(item)
            EventContent(item)
        }
    }
}

@Composable
private fun ConfigOptions(item: EventInfoForDisplay) {
    if (item.configOptions.isNotEmpty()) {
        Text(item.configOptions.toString(), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(5.dp))
    }
}

@Composable
private fun ChannelInfo(item: EventInfoForDisplay) {
    Text(item.channel, style = MaterialTheme.typography.bodySmall)
}


@Composable
private fun EventReceiveDate(item: EventInfoForDisplay) {
    val format = receiveDateFormat
    Text(format.format(item.receiveDate), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun EventTitle(item: EventInfoForDisplay) {
    Text(
        item.title,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun EventContent(item: EventInfoForDisplay) {
    Text(
        item.content,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
fun EventDetailsDialogPreview() {
    EventDetailsDialog(
        EventInfoForDisplay(0, "", setOf(), "", Date(), "", ""),
        "sdfasdfsdfasdf"
    ) { }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun EventListPreview() {
    XLog.init()
    Utils.context = LocalContext.current

    val eventListSequence = sequence {
        yield(
            listOf(
                EventInfoForDisplay(
                    1212, "123",
                    setOf("123", "456"), "c1", date(2025, 1, 1),
                    "title", "content"
                )
            )
        )
        yield((0..10).map {
            val str = it.toString().repeat(3)
            EventInfoForDisplay(
                it.toLong(), str,
                setOf(str), str, date(2025, 1, 1),
                str, str
            )
        })
        yield((11..20).map {
            val str = it.toString().repeat(3)
            EventInfoForDisplay(
                it.toLong(), str,
                setOf(str), str, date(2025, 1, 1),
                str, str
            )
        })
        yield((21..30).map {
            val str = it.toString().repeat(3)
            EventInfoForDisplay(
                it.toLong(), str,
                setOf(str), str, date(2025, 1, 1),
                str, str
            )
        })
    }.iterator()

    val getEvents = { isRefresh: Boolean ->
        if (eventListSequence.hasNext()) eventListSequence.next() else emptyList()
    }

    Page {
        EventList({ }, getEvents, "", "")
    }
}

private fun date(year: Int, month: Int, date: Int) = Date(year - 1900, month - 1, date)


data class EventInfoForDisplay(
    val id: Long,
    val packageName: String,
    val configOptions: Set<String>,
    val channel: String,
    val receiveDate: Date,
    val title: String,
    val content: String,
    val appName: String? = null,
    val event: Event = Event(),
)


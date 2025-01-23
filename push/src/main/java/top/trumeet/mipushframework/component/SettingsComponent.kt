package top.trumeet.mipushframework.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaomi.xmsf.R
import com.xiaomi.xmsf.utils.ConfigCenter

@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clickable(onClick = onClick, enabled = enabled)
            .fillMaxWidth()
            .padding(5.dp)
            .heightIn(min = 40.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemInfo(title, summary, modifier = Modifier.weight(9f))
        content?.let { it() }
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String,
    key: String,
    values: Array<String>,
    defaultValue: String
) {
    var shouldShowDialog by remember { mutableStateOf(false) }
    SettingsItem(
        title = title,
        summary = summary,
        confirmButton = {},
        content = {
            ItemLists(key, defaultValue, values) {
                shouldShowDialog = false
            }
        }
    )
}

@Composable
fun SettingsItem(
    title: String,
    summary: String,
    confirmButton: @Composable (dismiss: () -> Unit) -> Unit,
    onDismiss: (() -> Unit)? = null,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    var shouldShowDialog by remember { mutableStateOf(false) }
    SettingsItem(
        title = title,
        summary = summary,
        content = {
            val hideDialog = {
                shouldShowDialog = false
                onDismiss?.invoke()
                Unit
            }
            SettingsDialog(title, shouldShowDialog, hideDialog, {
                confirmButton(hideDialog)
            }) {
                content(hideDialog)
            }
        }
    ) {
        shouldShowDialog = true
    }
}

@Composable
fun SettingsDialog(
    title: String,
    shouldShowDialog: Boolean,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (!shouldShowDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirmButton,
        title = { Text(title) },
        text = content
    )
}

@Composable
private fun ItemLists(
    key: String,
    defaultValue: String,
    values: Array<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = ConfigCenter.getSharedPreferences(context)
    val selected = preferences.getString(key, defaultValue)!!.toInt()

    LazyColumn {
        itemsIndexed(values) { index, item ->
            Row(
                Modifier
                    .clickable {
                        preferences
                            .edit()
                            .putString(key, index.toString())
                            .apply()
                        onDismiss()
                    }
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(index == selected, onClick = null)
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = item)
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    enabled: Boolean = true,
    onClick: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val preferences = ConfigCenter.getSharedPreferences(context)
    var checked by remember { mutableStateOf(preferences.getBoolean(key, defaultValue)) }
    SettingsItem(title = title, summary = summary, checked = checked, enabled = enabled) {
        preferences.edit().putBoolean(key, !checked).apply()
        checked = !checked
        onClick?.invoke(checked)
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onClick: () -> Unit
) {
    SettingsItem(
        title, summary, content = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.scale(0.7f)
            )
        }, enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun ItemInfo(title: String, summary: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 10.dp, end = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        summary?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfoDialogPreview() {
    SettingsDialog(
        title = stringResource(R.string.pref_title_access_mode),
        shouldShowDialog = true,
        {}, {}
    ) {
        ItemLists(
            "AccessMode",
            "0",
            stringArrayResource(R.array.pref_title_access_mode_list_titles)
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsItemPreview() {
    SettingsItem(
        title = stringResource(R.string.settings_start_foreground_service),
        summary = stringResource(R.string.settings_start_foreground_service_summary),
        key = "StartForegroundService",
        defaultValue = false,
        enabled = false
    )
}

@Preview(showBackground = true)
@Composable
fun SingleLineSettingsItemPreview() {
    SettingsItem(
        title = stringResource(R.string.settings_start_foreground_service)
    ) {}
}
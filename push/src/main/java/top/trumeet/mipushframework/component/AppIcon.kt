package top.trumeet.mipushframework.component

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.trumeet.mipushframework.main.ApplicationIconCache

@SuppressLint("StaticFieldLeak")
lateinit var iconCache: ApplicationIconCache

fun initIconCache(context: Context) {
    if (!::iconCache.isInitialized) {
        iconCache = ApplicationIconCache(context)
    }
}

@Composable
fun AppIcon(packageName: String, appName: String?, modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current
    var icon by remember {
        mutableStateOf(
            if (isPreview) iconCache.defaultAppIcon
            else iconCache.get(packageName) ?: iconCache.defaultAppIcon
        )
    }
    if (icon == iconCache.defaultAppIcon) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                icon = iconCache.cache(packageName)
            }
        }
    }
    Image(icon, appName, modifier = modifier)
}
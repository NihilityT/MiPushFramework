package top.trumeet.mipushframework.main.subpage

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import top.trumeet.mipushframework.component.initIconCache
import top.trumeet.ui.theme.Theme

@Composable
fun Page(content: @Composable () -> Unit) {
    val context = LocalContext.current
    initIconCache(context)

    Theme {
        Surface(content = content)
    }
}
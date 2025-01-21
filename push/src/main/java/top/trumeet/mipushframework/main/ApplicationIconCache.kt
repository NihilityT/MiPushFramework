package top.trumeet.mipushframework.main

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import androidx.core.graphics.drawable.toBitmap
import top.trumeet.mipush.provider.register.RegisteredApplication
import java.util.concurrent.ConcurrentHashMap

class ApplicationIconCache(val context: Context) {
    val defaultAppIcon by lazy {
        BitmapPainter(
            ImageBitmap.imageResource(
                context.resources,
                android.R.mipmap.sym_def_app_icon
            )
        )
    }
    private val iconCache = ConcurrentHashMap<String, Painter>()

    fun get(item: RegisteredApplication): Painter? {
        return iconCache[item.packageName]
    }

    fun cache(item: RegisteredApplication): Painter {
        val icon = getAppIcon(item)
        iconCache[item.packageName] = icon
        return icon
    }

    private fun getAppIcon(item: RegisteredApplication) =
        BitmapPainter(item.getIcon(context).toBitmap().asImageBitmap())

}
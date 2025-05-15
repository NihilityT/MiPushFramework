package top.trumeet.mipushframework.main

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.xiaomi.xmsf.R
import top.trumeet.mipush.provider.entities.RegisteredApplication

object RegistrationStateStyle {

    val ErrorColor = Color(0xFFF41804)
    val GreenColor = Color(0xff4caf50)
    val YellowColor = Color(0xffff9800)

    fun contentOf(app: RegisteredApplication, context: Context): Pair<String, Color> {
        val prefix =
            if (!app.existServices) context.getString(R.string.mipush_services_not_found) + " - "
            else ""
        val color = colorOf(app)
        return when (app.registeredType) {
            RegisteredApplication.RegisteredType.Registered -> {
                Pair(prefix + context.getString(R.string.app_registered), color)
            }

            RegisteredApplication.RegisteredType.Unregistered -> {
                Pair(prefix + context.getString(R.string.app_registered_error), color)
            }

//      RegisteredApplication.RegisteredType.NotRegistered
            else -> {
                Pair(prefix + context.getString(R.string.status_app_not_registered), color)
            }
        }
    }

    fun colorOf(app: RegisteredApplication): Color {
        return if (!app.existServices) ErrorColor
        else when (app.registeredType) {
            RegisteredApplication.RegisteredType.Registered -> {
                GreenColor
            }

            RegisteredApplication.RegisteredType.Unregistered -> {
                YellowColor
            }

//      RegisteredApplication.RegisteredType.NotRegistered
            else -> {
                Color.Unspecified
            }
        }
    }
}
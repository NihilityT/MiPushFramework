package top.trumeet.mipushframework.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.xiaomi.xmsf.R
import top.trumeet.mipush.provider.register.RegisteredApplication

object RegistrationStateStyle {

    val ErrorColor = Color(0xFFF41804)
    val GreenColor = Color(0xff4caf50)
    val YellowColor = Color(0xffff9800)

    @Composable
    fun contentOf(app: RegisteredApplication): Pair<String, Color> {
        val prefix =
            if (!app.existServices) stringResource(R.string.mipush_services_not_found) + " - "
            else ""
        val color = colorOf(app)
        return when (app.registeredType) {
            RegisteredApplication.RegisteredType.Registered -> {
                Pair(prefix + stringResource(R.string.app_registered), color)
            }

            RegisteredApplication.RegisteredType.Unregistered -> {
                Pair(prefix + stringResource(R.string.app_registered_error), color)
            }

//      RegisteredApplication.RegisteredType.NotRegistered
            else -> {
                Pair(prefix + stringResource(R.string.status_app_not_registered), color)
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
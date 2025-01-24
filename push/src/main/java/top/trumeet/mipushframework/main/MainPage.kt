package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xiaomi.xmsf.R
import top.trumeet.ui.theme.Theme

class MainPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Theme {
                window.navigationBarColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    NavigationBarDefaults.Elevation
                ).toArgb()
            }
            Main(Screen.Apps.route.toString()) {
                {
                    composable(Screen.Events.route.toString()) {
                        Column {
                            var query by rememberSaveable { mutableStateOf("") }
                            SearchBar { query = it }
                            EventList(query)
                        }
                    }
                    composable(Screen.Apps.route.toString()) {
                        Column {
                            var query by rememberSaveable { mutableStateOf("") }
                            SearchBar { query = it }
                            ApplicationList(query)
                        }
                    }
                    composable(Screen.Settings.route.toString()) { Settings() }
                }
            }
        }
    }
}

private sealed class Screen(val route: Int, val icon: Int) {
    object Events : Screen(R.string.main_event, R.drawable.ic_event_note_black_24dp)
    object Apps : Screen(R.string.main_apps, R.drawable.ic_apps_black_24dp)
    object Settings : Screen(R.string.main_settings, R.drawable.ic_settings_black_24dp)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Events, Screen.Apps, Screen.Settings
    )

    NavigationBar(Modifier.height(56.dp)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val name = stringResource(screen.route)
            NavigationBarItem(
                icon = { Icon(painterResource(id = screen.icon), contentDescription = name) },
                selected = currentRoute == screen.route.toString(),
                onClick = {
                    navController.navigate(screen.route.toString()) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
        }
    }
}

@Composable
private fun Main(
    startDestination: String,
    navContent: () -> NavGraphBuilder.() -> Unit
) {
    val navController = rememberNavController()

    Theme {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    builder = navContent()
                )
            }
            BottomNavigationBar(navController)
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainEventsPreview() {
    Main(Screen.Events.route.toString()) {
        {
            composable(Screen.Events.route.toString()) {
                Column {
                    val onValueChange: (String) -> Unit = {}
                    SearchBar(onValueChange)
                    EventListPreview()
                }
            }
            composable(Screen.Apps.route.toString()) { }
            composable(Screen.Settings.route.toString()) { }
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainAppsPreview() {
    Main(Screen.Apps.route.toString()) {
        {
            composable(Screen.Events.route.toString()) { }
            composable(Screen.Apps.route.toString()) {
                Column {
                    val onValueChange: (String) -> Unit = {}
                    SearchBar(onValueChange)
                    ApplicationListPreview()
                }
            }
            composable(Screen.Settings.route.toString()) { }
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainSettingsPreview() {
    Main(Screen.Settings.route.toString()) {
        {
            composable(Screen.Events.route.toString()) { }
            composable(Screen.Apps.route.toString()) { }
            composable(Screen.Settings.route.toString()) { SettingsPagePreview() }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchBar(onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val change: (String) -> Unit = { query = it; onValueChange(it) }
    TopAppBar(title = {
        TextField(
            value = query,
            onValueChange = change,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search...") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton({ change("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent)
        )
    })
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
)
@Composable
private fun MainDialogPreview() {
    Main(Screen.Events.route.toString()) {
        {
            composable(Screen.Events.route.toString()) {
                EventDetailsDialogPreview()
            }
            composable(Screen.Apps.route.toString()) { }
            composable(Screen.Settings.route.toString()) { }
        }
    }
}
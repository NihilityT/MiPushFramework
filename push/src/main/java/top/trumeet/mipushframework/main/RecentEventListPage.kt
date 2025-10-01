package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import top.trumeet.mipushframework.component.SearchBar
import top.trumeet.mipushframework.main.subpage.EventList
import top.trumeet.ui.theme.Theme

class RecentEventListPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val packageName = intent.dataString!!
        setContent {
            Theme {
                window.navigationBarColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    NavigationBarDefaults.Elevation
                ).toArgb()
                Column(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    var query by rememberSaveable { mutableStateOf("") }
                    SearchBar("Search...") { query = it }
                    EventList(query, packageName)
                }
            }
        }
    }
}

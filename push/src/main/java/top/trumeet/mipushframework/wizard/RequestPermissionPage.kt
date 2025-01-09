package top.trumeet.mipushframework.wizard

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import top.trumeet.mipushframework.help.MarkdownView
import top.trumeet.mipushframework.settings.MainActivity
import top.trumeet.mipushframework.wizard.permission.AlertWindowPermissionInfo
import top.trumeet.mipushframework.wizard.permission.PermissionInfo
import top.trumeet.mipushframework.wizard.permission.RequestIgnoreBatteryOptimizationsPermissionInfo
import top.trumeet.mipushframework.wizard.permission.UsageStatsPermissionInfo
import top.trumeet.ui.theme.Theme

class RequestPermissionPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        setContent {
            Theme {
                windowInsetsController.isAppearanceLightStatusBars = isSystemInDarkTheme()
                val backgroundColor = MaterialTheme.colorScheme.primaryContainer
                window.navigationBarColor = backgroundColor.toArgb()
                MainPage(navigatorColor = backgroundColor)
            }
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun MainPage(
    modifier: Modifier = Modifier,
    navigatorColor: Color = MaterialTheme.colorScheme.secondaryContainer
) {
    val context = LocalContext.current
    val permissionInfos = getPermissionInfos(context)

    val currentItem = remember { mutableStateOf(0) }
    if (allPermissionsGranted(currentItem, permissionInfos)) {
        JumpToMainActivity()
        WizardSPUtils.finishWizard(context as AppCompatActivity)
        return
    }

    NavigateToNextPageIfPermissionGranted(permissionInfos, currentItem)

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        RequestPermissionContent(permissionInfos[currentItem.value])
        BottomBar(currentItem, permissionInfos, navigatorColor)
    }
}

@Composable
private fun JumpToMainActivity() {
    val context = LocalContext.current
    context.startActivity(Intent(context, MainActivity::class.java))
}

private fun allPermissionsGranted(
    currentItem: MutableState<Int>, permissionInfos: MutableList<PermissionInfo>
) = currentItem.value >= permissionInfos.size

private fun getPermissionInfos(context: Context): MutableList<PermissionInfo> {
    val pages = mutableListOf<PermissionInfo>().apply {
        add(WelcomePhonyPermissionInfo(context))
        add(UsageStatsPermissionInfo(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            add(RequestIgnoreBatteryOptimizationsPermissionInfo(context))
            AlertWindowPermissionInfo(context)
        }
        add(FinishedPhonyPermissionInfo(context))
    }
    return pages
}

@Composable
private fun NavigateToNextPageIfPermissionGranted(
    pages: List<PermissionInfo>, currentItem: MutableState<Int>
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isNotFirstResume = false
        val observer = LifecycleEventObserver { _, event ->
            val page = pages[currentItem.value]
            println("Lifecycle event: $event ${currentItem.value} ${page.permissionOperator.isPermissionGranted()}")
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isNotFirstResume && page.permissionOperator.isPermissionGranted()) {
                    currentItem.value++
                }
                isNotFirstResume = true
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun RequestPermissionContent(permissionInfo: PermissionInfo) {
    Column {
        Title(permissionInfo.permissionTitle)
        Description(permissionInfo.permissionDescription)
    }
}

@Composable
private fun Description(description: String) {
    Row {
        MarkdownView(
            description,
            textSize = MaterialTheme.typography.bodyLarge.fontSize.value,
            modifier = Modifier
                .align(Alignment.Bottom)
                .padding(16.dp)
        )
    }
}

@Composable
private fun Title(title: String) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.Bottom)
                .padding(16.dp)
        )
    }
}

@Composable
private fun BottomBar(
    currentItem: MutableState<Int>, permissions: List<PermissionInfo>, backgroundColor: Color
) {
    BottomAppBar(
        modifier = Modifier.height(56.dp),
        containerColor = backgroundColor,
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { currentItem.value-- }, enabled = currentItem.value > 0
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, contentDescription = "上一项"
                )
            }

            val operator = permissions[currentItem.value].permissionOperator
            IconButton(onClick = {
                if (operator.isPermissionGranted()) {
                    currentItem.value++
                } else {
                    operator.requestPermission()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowForward, contentDescription = "下一项"
                )
            }
        }
    }
}
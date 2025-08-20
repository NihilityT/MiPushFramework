package top.trumeet.mipushframework.main


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.elevation.SurfaceColors
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.MarkdownView
import top.trumeet.ui.theme.Theme
import java.io.InputStreamReader

class HelpPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val color = SurfaceColors.SURFACE_2.getColor(this)
            window.statusBarColor = color
            Theme {
                HelpList()
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
)
@Composable
fun HelpPage(modifier: Modifier = Modifier) {
    HelpList()
}

@Composable
fun HelpList(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "list", modifier = modifier) {
        composable("list") { HelpList(navController) }
        composable("markdown/{markdownResId}") { backStackEntry ->
            val markdownResId = backStackEntry.arguments?.getString("markdownResId")?.toInt()
            Markdown(markdownResId)
        }
    }
}

@Composable
fun HelpList(navController: NavHostController) {
    Column {
        FAQ(navController)
        Divider()
        ContactUs()
    }
}

@Composable
private fun Markdown(markdownResId: Int?) {
    MarkdownView(
        readRawFile(LocalContext.current, markdownResId!!), modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun FAQ(
    navController: NavHostController
) {
    Group(stringResource(R.string.helplib_title_faq))
    for (article in getArticles(LocalContext.current)) {
        ClickableListItem(article.titleRes) {
            navController.navigate("markdown/${article.markdownRes}") // 跳转并传递数据
        }
    }
}

@Composable
private fun ContactUs() {
    val context = LocalContext.current
    Group(stringResource(R.string.helplib_title_contact))
    ClickableListItem(R.string.helplib_action_qq_group) {
        openUrl(context, "https://pd.qq.com/s/4tsiu8hlu")
    }
    ClickableListItem(R.string.helplib_action_telegram_group) {
        openUrl(context, "http://t.me/mipushframework")
    }
    ClickableListItem(R.string.helplib_action_issue) {
        openUrl(context, "https://github.com/NihilityT/MiPushFramework/issues")
    }
}

@Composable
private fun Group(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun ClickableListItem(textResourceId: Int, onClick: () -> Unit) {
    ClickableListItem(stringResource(textResourceId), onClick)
}

@Composable
private fun ClickableListItem(item: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp)
            .padding(start = 24.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = item,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

class Article(val titleRes: Int, val markdownRes: Int)

private fun getArticles(context: Context): List<Article> {
    val articlesArray = context.resources.getStringArray(R.array.help_articles)

    val articles = mutableListOf<Article>()
    for (str in articlesArray) {
        val info = str.split("|")
        if (info.size != 2) continue

        val titleRes = R.string::class.java.getField(info[0]).getInt(null)
        val markdownRes = R.raw::class.java.getField(info[1]).getInt(null)
        articles.add(Article(titleRes, markdownRes))
    }
    return articles
}

private fun readRawFile(context: Context, fileName: Int): String {
    val inputStream = context.resources.openRawResource(fileName)
    val reader = InputStreamReader(inputStream)
    return reader.readText()
}
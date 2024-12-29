package top.trumeet.mipushframework.help

import android.os.Bundle
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun MarkdownView(markdownText: String, modifier: Modifier = Modifier) {
    val html = toHtml(markdownText)
    val context = LocalContext.current
    AndroidView(modifier = modifier, factory = {
        TextView(context).apply {
            text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    })
}

@Composable
private fun toHtml(markdownText: String): String {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownText)
    val htmlText = HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
    return htmlText
}

@Preview(showBackground = true)
@Composable
fun PreviewMarkdown() {
    MarkdownView(markdownText = "# Hello, Compose!\n\nThis is **Markdown** rendering in Compose!")
}
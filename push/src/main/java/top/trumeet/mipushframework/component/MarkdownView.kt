package top.trumeet.mipushframework.component

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun MarkdownView(markdownText: String, modifier: Modifier = Modifier, textSize: Float? = null) {
    val html = toHtml(markdownText)
    AndroidView(modifier = modifier, factory = { context -> TextView(context).apply {
        minHeight = 0
        setTextIsSelectable(true) // must before movementMethod
        isFocusable = true
        movementMethod = LinkMovementMethod.getInstance()
    } }, update = { it ->
        it.apply {
            text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).trimEnd()
            textSize?.let { setTextSize(it) }
        }
    })
}

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
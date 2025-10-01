package top.trumeet.mipushframework.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.HtmlCompat
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun MarkdownView(markdownText: String, modifier: Modifier = Modifier, textSize: Float? = null) {
    val html = toHtml(markdownText)
    val textToShow = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).trimEnd()
    TextView(textToShow, modifier, textSize)
}

private fun toHtml(markdownText: String): String {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownText)
    val htmlText = HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
    return htmlText
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    MarkdownView(markdownText = "# Hello, Compose!\n\nThis is **Markdown** rendering in Compose!")
}
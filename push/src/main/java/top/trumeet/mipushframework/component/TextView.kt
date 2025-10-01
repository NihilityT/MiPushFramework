package top.trumeet.mipushframework.component

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TextView(text: CharSequence, modifier: Modifier = Modifier, textSize: Float? = null) {
    AndroidView(modifier = modifier, factory = { context ->
        TextView(context).apply {
            minHeight = 0
            setTextIsSelectable(true) // must before movementMethod
            isFocusable = true
            movementMethod = LinkMovementMethod.getInstance()
        }
    }, update = { it ->
        it.apply {
            this.text = text
            textSize?.let { setTextSize(it) }
        }
    })
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TextView("# Hello, Compose!\n\nThis is **Markdown** rendering in Compose!")
}
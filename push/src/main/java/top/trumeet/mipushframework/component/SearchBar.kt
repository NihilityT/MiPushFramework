package top.trumeet.mipushframework.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchBar(placeholder: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val ph by rememberUpdatedState(placeholder)
    val debounceOnValueChange: (String) -> Unit = debounce(onValueChange)
    val change: (String) -> Unit = { query = it; debounceOnValueChange(it) }
    TopAppBar(title = {
        TextField(
            value = query,
            onValueChange = change,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(ph) },
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

@Composable
private fun debounce(onValueChange: (String) -> Unit): (String) -> Unit {
    val scope = rememberCoroutineScope()
    var job: Job? = null
    val change: (String) -> Unit = {
        scope.launch {
            job?.cancel()
            job = launch {
                delay(300)
                onValueChange(it)
            }
        }
    }
    return change
}
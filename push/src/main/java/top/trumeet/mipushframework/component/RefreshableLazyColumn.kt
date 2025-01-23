package top.trumeet.mipushframework.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun RefreshableLazyColumn(
    doRefresh: (onRefreshed: () -> Unit) -> Unit,
    isNeedMore: (lastVisibleIndex: Int) -> Boolean,
    doLoadMore: (onRefreshed: () -> Unit) -> Unit,
    content: LazyListScope.() -> Unit
) {
    val currentIsNeedMore by rememberUpdatedState(isNeedMore)
    val currentDoLoadMore by rememberUpdatedState(doLoadMore)

    var isRefreshing by remember { mutableStateOf(false) }
    val onRefreshed by remember { mutableStateOf({ isRefreshing = false }) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {
            isRefreshing = true
            doRefresh(onRefreshed)
        }
    ) {
        val lazyListState = rememberLazyListState()
        LaunchedEffect(lazyListState) {
            snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
                .collect { visibleItems ->
                    if (isRefreshing) return@collect
                    val lastIndex = if (visibleItems.isNotEmpty())
                        visibleItems.last().index else 0
                    if (currentIsNeedMore(lastIndex)) {
                        isRefreshing = true
                        currentDoLoadMore(onRefreshed)
                    }
                }
        }
        LazyColumn(Modifier.fillMaxSize(), state = lazyListState, content = content)
    }
}
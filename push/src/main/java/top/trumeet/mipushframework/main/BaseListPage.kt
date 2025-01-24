package top.trumeet.mipushframework.main

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.initIconCache
import top.trumeet.ui.theme.Theme
import java.util.Locale

abstract class BaseListPage : Fragment() {
    protected var query by mutableStateOf("")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        return ComposeView(requireContext()).apply {
            setContent {
                ViewContent()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_enable).setVisible(false)
        menu.findItem(R.id.action_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        val searchItem = menu.findItem(R.id.action_search)
        searchItem.setVisible(true)

        initSearchBar(searchItem)
    }

    private fun initSearchBar(searchItem: MenuItem) {
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = searchItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText != query) {
                    query = newText.lowercase(Locale.getDefault())
                }
                return true
            }

            override fun onQueryTextSubmit(newText: String): Boolean {
                return true
            }
        })
    }

    @Composable
    protected abstract fun ViewContent()
}


@Composable
fun Page(content: @Composable () -> Unit) {
    val context = LocalContext.current
    initIconCache(context)

    Theme {
        Surface(content = content)
    }
}
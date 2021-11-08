package il.co.superclick.order.address_lookup

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.ui.showKeyboard
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.LayoutManager
import il.co.superclick.remote.Place
import il.co.superclick.utilities.mainColor
import il.co.superclick.widgets.SearchWidget
import kotlinx.android.synthetic.main.fragment_address_lookup.*

class AddressLookupFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_CALLBACK = "KEY_CALLBACK"
        private const val KEY_QUERY = "KEY_QUERY"

        fun open(query: String? = null, callback: ((Place) -> Unit)? = null) {
            open(KEY_CALLBACK to callback, KEY_QUERY to query)
        }
    }

    override val layout = R.layout.fragment_address_lookup
    override val themeBackground: Drawable? get() = getDrawable(android.R.drawable.screen_background_light)
    private val backButton: ImageView? get() = address_lookup_back_button
    private val searchWidget: SearchWidget? get() = address_lookup_search
    private val lineView: View? get() = address_lookup_line
    private val recycler: RecyclerView? get() = address_lookup_recycler

    private val adapter: AddressLookupAdapter? get() = recycler?.adapter as? AddressLookupAdapter
    private val viewModel: AddressLookupViewModel? get() = ViewModelProvider(this)[AddressLookupViewModel::class.java]
    private var query: String? = null
    private var callback: ((Place) -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        callback = arguments[KEY_CALLBACK] as? ((Place) -> Unit)
        query = arguments[KEY_QUERY] as? String
    }

    override fun onStart() {
        super.onStart()
        initBackButton()
        initRecycler()
        initSearchEdit()
    }

    private fun initBackButton() {
        backButton?.onClick { navigateBack() }
        backButton?.imageTintList = mainColor
    }

    private fun initRecycler() {
        recycler?.apply {
            layoutManager = (layoutManager ?: LayoutManager(context,1))
            adapter = AddressLookupAdapter(onItemClick = { place ->
                callback?.invoke(place)
            })
        }
    }

    private fun initSearchEdit() {
        searchWidget?.text = viewModel?.lastSearch
        searchWidget?.onTextChanged { text ->
            if (text.isNotBlank()) {
                viewModel?.search(text.trim().toString()) {
                    adapter?.submitList(it) {
                        lineView?.isVisible = adapter?.currentList?.isNotEmpty() == true
                    }
                }
            } else {
                adapter?.clearList()
                lineView?.isInvisible = true
            }
        }
        if (query != null) {
            searchWidget?.text = query
            query = null
        }
    }

    override fun onResume() {
        super.onResume()
        searchWidget?.requestFocus()
        showKeyboard()
    }

}
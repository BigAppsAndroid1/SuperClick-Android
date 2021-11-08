package il.co.superclick.search

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.ui.viewModel
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.R
import il.co.superclick.infrastructure.LayoutManager
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.product_list.ProductListAdapter
import il.co.superclick.product_list.ProductsAdapter
import kotlinx.android.synthetic.main.fragment_search.*

class SearchFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_TEXT = "KEY_TEXT"

        fun open(text: String) {
            if (foregroundFragment?.javaClass == clazz) {
                (foregroundFragment as? SearchFragment)?.run {
                    search(text)
                }
            } else {
                open(KEY_TEXT to text)
            }
        }
    }

    override val isSearch = true
    override val isCheckoutBar = false
    override val layout = R.layout.fragment_search
    override val themeBackground: Drawable? = getDrawable(R.drawable.bg_search)
    private val recycler: RecyclerView? get() = search_recycler
    private val adapter: ProductsAdapter? get() = recycler?.adapter as? ProductsAdapter
    private val viewModel: SearchViewModel? get() = viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuBar?.toggleCartButton()
        menuBar?.toggleButtonsColor()
        initRecycler()
        subscribe()
        menuBar?.toggleSearchEdit(true)
        menuBar?.searchEdit?.requestFocus()
    }

    override fun onForeground() {
        super.onForeground()
        (adapter as? ProductListAdapter)?.notifyDataSetChanged()
        menuBar?.toggleCartButton()
    }

    private fun search(text: String) {
        viewModel?.fetch(text)
    }

    private fun initRecycler() {
        recycler?.adapter = ProductListAdapter()
        (recycler?.layoutManager as? LayoutManager)?.orientation = VERTICAL
    }

    private fun subscribe() {
        viewModel?.results?.removeObservers(viewLifecycleOwner)
        viewModel?.results?.observe(viewLifecycleOwner) { products ->
            if (products != null) adapter?.submitList(products)
        }
    }

}
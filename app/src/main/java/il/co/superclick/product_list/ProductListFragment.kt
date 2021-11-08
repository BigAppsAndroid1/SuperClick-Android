package il.co.superclick.product_list

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.updatePadding
import com.dm6801.framework.ui.viewModel
import com.dm6801.framework.utilities.openWebBrowser
import il.co.superclick.R
import il.co.superclick.data.Cart.updateLiveData
import il.co.superclick.data.ListType
import il.co.superclick.data.Shop
import il.co.superclick.infrastructure.App
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator.database
import il.co.superclick.infrastructure.Locator.notifications
import il.co.superclick.infrastructure.Locator.repository
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.notifications.NotificationSubscribeDialog
import il.co.superclick.utilities.OnSwipeTouchListener
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.fragment_product_list.*
import kotlinx.android.synthetic.main.item_sub_category.*


class ProductListFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_CATEGORY = "KEY_CATEGORY"

        fun open(category: String) {
            val fragment = foregroundFragment
            if (fragment is ProductListFragment) {
                if (fragment.category != category) {
                    fragment.subscribe(category)
                }
            } else {
                open(KEY_CATEGORY to category)
            }
        }
    }

    override val isSearch = true
    override val isCheckoutBar = false
    override val layout = R.layout.fragment_product_list
    private val progressBar: ContentLoadingProgressBar? get() = product_list_progress_bar
    private val recycler: RecyclerView? get() = product_list_recycler
    private val accessibilityButton: View? get() = main_accessibilities_button
    val adapter get() = recycler?.adapter
    val viewModel: ProductListViewModel get() = viewModel()
    private val cart = database.cart
    private val products = cart.liveProducts
    var category: String? = null; private set

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_CATEGORY] as? String)?.let { category = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuBar?.toggleCartButtonColor(false)
        initRecycler()
        category?.run(::subscribe)
        observeCartProducts()
        initNotifications()
        fetch(category ?: return)
        viewModel.subCategory.observe(viewLifecycleOwner) { subCategory ->
            val products = if (subCategory != null)
                repository.products[category]?.values?.filter { it.product.subCategory == subCategory }
                    ?: return@observe
            else
                repository.products[category]?.values?.toList() ?: return@observe
            ((adapter as? ProductListAdapter) ?: (adapter as? ProductGridAdapter)
            ?: (adapter as? ProductHorizontalAdapter))?.submitList(products)
        }

        (foregroundApplication as? App)?.accessibilityLink?.takeIf { it.isNotBlank() }
            ?.let { link ->
                accessibilityButton?.setOnTouchListener(object : OnSwipeTouchListener() {
                    override fun onSwipeRight(): Boolean {
                        accessibilityButton?.isVisible = false
                        return super.onSwipeRight()
                    }

                    override fun onTouch(v: View, event: MotionEvent): Boolean {

                        return super.onTouch(v, event)
                    }

                    override fun onSwipeLeft(): Boolean {
                        accessibilityButton?.isVisible = false
                        return super.onSwipeLeft()
                    }
                }
                )
                accessibilityButton?.isVisible = true
                accessibilityButton?.onClick {
                    if (accessibilityButton?.isVisible == false) return@onClick
                    openWebBrowser(link)
                }
            }
    }

    override fun onForeground() {
        super.onForeground()
        database.cart.updateLiveData()
        menuBar?.toggleCartButton(false)
    }

    private fun initNotifications() {
        if (database.isNotifications) notifications.init()
        else NotificationSubscribeDialog.open()
    }

    private fun observeCartProducts() {
        products.observe(viewLifecycleOwner) { map ->
            if (foregroundFragment !is ProductListFragment)
                adapter?.notifyDataSetChanged()
        }
    }

    private fun initRecycler() {
        recycler?.adapter = when (Shop.listType) {
            ListType.Linear, ListType.LinearBig -> {
                ProductListAdapter { end ->
                    viewModel?.preload(category ?: return@ProductListAdapter, force = end)
                }
            }
            ListType.Grid -> {
                ProductGridAdapter { end ->
                    viewModel.preload(category ?: return@ProductGridAdapter, force = end)
                }
            }
            ListType.Horizontal -> {
                recycler?.updatePadding(left = 50.dpToPx, right = 50.dpToPx)
                recycler?.clipToPadding = false
                ProductHorizontalAdapter { end ->
                    viewModel.preload(category ?: return@ProductHorizontalAdapter, force = end)
                }
            }
        }
    }

    fun subscribe(category: String) {
        this.category = category
        this.categoriesBar?.set(category)
        viewModel.getLiveData(category).apply {
            removeObservers(viewLifecycleOwner)
            observe(viewLifecycleOwner) { products ->
                if (products == null) return@observe
                when (Shop.listType) {
                    ListType.Linear, ListType.LinearBig -> (adapter as? ProductListAdapter)?.submitList(
                        products
                    ) { hideLoader() }
                        ?: hideLoader()
                    ListType.Grid -> (adapter as? ProductGridAdapter)?.submitList(products) { hideLoader() }
                        ?: hideLoader()
                    ListType.Horizontal -> (adapter as? ProductHorizontalAdapter)?.submitList(
                        products
                    ) { hideLoader() }
                        ?: hideLoader()
                }
            }
        }
    }

    private fun fetch(category: String) {
        showLoader()
        viewModel.fetch(category)
    }

    private fun showLoader() {
        progressBar?.isVisible = true
    }

    private fun hideLoader() {
        progressBar?.isGone = true
    }

}
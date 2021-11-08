package il.co.superclick.cart

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.dm6801.framework.ui.findChild
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.data.Cart
import il.co.superclick.data.Database.cart
import il.co.superclick.data.Shop
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.order.OrderTypeFragment
import il.co.superclick.utilities.*
import il.co.superclick.widgets.SpinnerWidget
import kotlinx.android.synthetic.main.fragment_cart.*
import kotlinx.android.synthetic.main.fragment_cart.cart_container
import kotlinx.android.synthetic.main.fragment_cart.cart_scroll_view
import kotlinx.android.synthetic.main.fragment_cart.checkout_coupon_button
import kotlinx.android.synthetic.main.fragment_cart.checkout_order_button
import kotlinx.android.synthetic.main.fragment_cart.checkout_widget_spinner_list
import kotlinx.android.synthetic.main.fragment_checkout.*

class CartFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_FOCUS_PRODUCT = "KEY_FOCUS_PRODUCT"
        private val database get() = Locator.database
        private val shop get() = database.shop
    }

    override val isSearch = true
    override val layout = R.layout.fragment_cart
    private val checkAllView: ViewGroup? get() = cart_check_all
    private val checkboxAll: CheckBox? get() = cart_checkbox_all
    private val checkboxBackground: View? get() = cart_checkbox_background
    private val nestedScrollView: NestedScrollView? get() = cart_scroll_view
    private val container: ConstraintLayout? get() = cart_container

    private val checkoutOrderButton: TextView? get() = checkout_order_button
    private val checkoutCouponButton: TextView? get() = checkout_coupon_button
    private val chooseBranch: SpinnerWidget? get() = checkout_widget_spinner_list

    private val cartDeliveryPrice: TextView? get() = cart_delivery_price
    private val recyclerContainer: LinearLayout? get() = cart_recycler_container
    private val emptyListView: ViewGroup? get() = cart_empty_container
    private val emptyListViewButton: ImageView? get() = cart_empty_container_button
    private val products get() = cart.liveProducts
    private val selected = mutableSetOf<Int>()
    private var size = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        initCheckAllCheckbox()
        chooseBranch?.onBranchChecked = { shopBranch ->
            main {
                if (shopBranch == null) {
                    chooseBranch?.animate()
                        ?.translationY(chooseBranch?.measuredHeight?.toFloat() ?: 0f)?.duration =
                        300
                    return@main
                }
                OrderTypeFragment.open(shopBranch)
                chooseBranch?.translationY = chooseBranch?.measuredHeight?.toFloat() ?: 0f
            }
        }
        checkoutCouponButton?.apply {
            if (Shop.isShopWithCoupons == false) {
                isVisible = false
                return@apply
            }
            isVisible = true
            onClick {
                CouponNumberDialog().show()
            }
            if (database.coupon != null) {
                this.disable()
            }
        }
        observeCartProducts()
    }

    override fun onForeground() {
        super.onForeground()
        initEmptyCartButton()
        menuBar?.toggleCartButton(false)
        products.value?.run(::refreshData)

    }

    private fun setToolbar() {
        menuBar?.toggleCartButtonColor(isFilled = true)
    }

    private fun initCheckAllCheckbox() {
        checkboxAll?.buttonTintList = mainColor
        checkboxAll?.onClick(1_000) {
            val isChecked = (it as? CheckBox)?.isChecked ?: false
            getListViews().forEach { categoryView ->
                if (isChecked) categoryView.checkAll()
                else categoryView.uncheckAll()
            }
        }
    }

    private fun initEmptyCartButton() {
        emptyListViewButton?.onClick {
            activity?.popBackStack()
        }
    }

    private fun observeCartProducts() {
        products.observe(viewLifecycleOwner) { map ->
            refreshData(map)
           if(map.values.flatten().firstOrNull { it.isChecked} != null){
               checkoutOrderButton?.alpha = 1f
               checkoutOrderButton?.onClick {
                   if (shop?.branches.isNullOrEmpty()) {
                       OrderTypeFragment.open()
                       return@onClick
                   }
                   chooseBranch?.animate()?.translationY(0f)?.duration = 300
               }
           } else{
               checkoutOrderButton?.alpha = 0.6f
               checkoutOrderButton?.onClick {}
           }

        }

    }

    private fun refreshData(map: Map<String, List<Cart.Item>>) {
        map.forEach { (category, products) ->
            updateList(category, products)
        }
        toggleLists(map)
        toggleBarButton()
        toggleDeliveryPrice()

    }

    private fun scrollToProduct(productId: Int) {
        val childView = (view as? ViewGroup)?.findChild<View?> { it?.tag == productId } ?: return
        val (childX, childY) = childView.getLocationInWindow()
        nestedScrollView?.smoothScrollTo(childX, childY - (childView.height * 0.9).toInt())
    }

    private fun updateList(
        category: String,
        products: List<Cart.Item>,
        onUpdated: (() -> Unit)? = null
    ) {
        val recyclerContainer = recyclerContainer ?: return
        var listView = recyclerContainer.findViewWithTag<CartList?>(category)
        if (listView == null) {
            listView = CartList(recyclerContainer.context)
            recyclerContainer.addView(listView)
            listView.init(
                category,
                products,
                isHistory = false,
                onToggle = { productId, isChecked ->
                    if (isChecked) selected.add(productId)
                    else selected.remove(productId)
                    toggleBarButton()
                })
        } else {
            listView.submitList(products, onUpdated)
            listView.show()
        }
    }

    private fun toggleLists(map: Map<String, List<Cart.Item>>) {
        getListViews().forEach { categoryView ->
            val category = categoryView.tag as? String ?: return@forEach
            if (!map.containsKey(category)) categoryView.submitList(emptyList())
        }
        val categoriesSize = map.values.filter { it.isNotEmpty() }.size
        emptyListView?.isVisible = categoriesSize == 0
        checkAllView?.isVisible = categoriesSize != 0
        container?.isVisible = categoriesSize != 0
        size = map.values.flatten().size
        if (categoriesSize == 0) selected.clear()
    }

    private fun toggleDeliveryPrice() {
        if (shop?.deliveryZones?.isEmpty() == true) {
            cartDeliveryPrice?.text = getString(
                R.string.delivery_price,
                (shop?.deliveryCost ?: 0.0).formatPrice()
            )
            cartDeliveryPrice?.isInvisible = shop?.deliveryCost == 0.0 || shop?.isAreaDelivery == true
        }
    }

    private fun toggleBarButton() {
        checkboxAll?.isChecked = selected.size == size
        checkboxBackground?.isVisible = selected.size == size
    }

    private fun getListViews(): Sequence<CartList> {
        return recyclerContainer?.children?.mapNotNull { it as? CartList } ?: emptySequence()
    }

    fun disableCouponButton() {
        checkoutCouponButton?.disable()
    }
}
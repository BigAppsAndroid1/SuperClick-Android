package il.co.superclick.cart

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import il.co.superclick.R
import il.co.superclick.data.Cart
import il.co.superclick.data.OrderProduct
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.RecyclerAdapter
import kotlinx.android.synthetic.main.view_cart_category_list.view.*

@Suppress("UsePropertyAccessSyntax")
open class CartList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val LAYOUT = R.layout.view_cart_category_list
        private val database get() = Locator.database
    }

    protected val text: TextView? get() = cart_category_list_name
    protected val recyclerView: RecyclerView? get() = cart_category_list
    protected val _adapter: RecyclerAdapter<*, *>? get() = recyclerView?.adapter as? RecyclerAdapter<*, *>
    private val adapter: CartAdapter? get() = _adapter as? CartAdapter
    private var isHistory: Boolean = false

    private var onToggle: ((productId: Int, isChecked: Boolean) -> Unit)? = null
    protected var category: String? = null
    open val products get() = adapter?.currentList?.map { it.cartItem } ?: emptyList()

    init {
        inflate(context, LAYOUT, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        orientation = VERTICAL
        setLayout()
    }

    private fun setLayout() {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun init(
        category: String,
        products: List<OrderProduct>,
        isHistory: Boolean,
        onToggle: ((productId: Int, isChecked: Boolean) -> Unit)? = null
    ) {
        this.category = category
        tag = category
        text?.text = database.findCategory(category)?.displayName
        this.onToggle = onToggle
        recyclerView?.adapter = adapter(products, isHistory)
        this.isHistory = isHistory
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun adapter(products: List<OrderProduct>, isHistory: Boolean): RecyclerAdapter<*, *> {
        return CartAdapter(
            (products as List<Cart.Item>).filter { it.product != null },
            onToggle
        )
    }

    open fun submitList(products: List<Cart.Item>, callback: (() -> Unit)? = null) {
        adapter?.submitList(products.filter { it.product != null }) {
            hideIfEmpty()
            callback?.invoke()
        }
    }

    fun hideIfEmpty() {
        if (_adapter?.getItemCount() == 0) hide()
    }

    fun show() = setVisibility(View.VISIBLE)
    fun hide() = setVisibility(View.GONE)

    fun checkAll() = adapter?.checkAll()
    fun uncheckAll() = adapter?.uncheckAll()

}
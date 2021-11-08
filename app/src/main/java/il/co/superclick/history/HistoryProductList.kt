package il.co.superclick.history

import android.content.Context
import android.util.AttributeSet
import il.co.superclick.data.HistoryProduct
import il.co.superclick.infrastructure.RecyclerAdapter

class HistoryProductList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : HistoryList(context, attrs, defStyleAttr, defStyleRes) {

    private val adapter: HistoryProductAdapter? get() = _adapter as? HistoryProductAdapter

    override fun adapter(products: List<HistoryProduct>): RecyclerAdapter<*, *> = HistoryProductAdapter(products.filter { it.product != null })

    override fun submitList(products: List<HistoryProduct>, callback: (() -> Unit)?) {
        adapter?.submitList(products.filter { it.product != null }) {
            hideIfEmpty()
        }
    }
}
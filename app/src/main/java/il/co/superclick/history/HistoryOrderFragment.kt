package il.co.superclick.history

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.dm6801.framework.infrastructure.foregroundActivity
import il.co.superclick.R
import il.co.superclick.data.Database
import il.co.superclick.data.HistoryOrder
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.fragment_history_order.*

class HistoryOrderFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_ORDER = "KEY_ORDER"
        fun open(order: HistoryOrder) = open(KEY_ORDER to order)
    }

    override val layout = R.layout.fragment_history_order
    private val container: LinearLayout? get() = history_order_container
    private val title: TextView? get() = history_order_title
    private val copyOrder: TextView? get() = history_order_copy_order
    private var order: HistoryOrder? = null

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ORDER] as? HistoryOrder)?.let { order = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuBar?.toggleCartButton()
        order?.let {
            populate(it)
        }
        title?.text = getString(R.string.order_with_number, order?.id.toString())
        copyOrder?.onClick {
            order?.let { it1 -> HistoryListAdapter.addHistoryOrderToCard(it1) }
            foregroundActivity?.popBackStack(FragmentManager.POP_BACK_STACK_INCLUSIVE, HistoryListFragment().TAG)
        }
    }

    private fun populate(order: HistoryOrder) {
        if (order.products.isNullOrEmpty()) return
        order.products?.groupBy { it.category }?.forEach { (category, items) ->
            items.forEach { it.link = order.link }
            val listView = HistoryProductList(container?.context ?: return)
            container?.addView(listView)
            listView.init(category, items, isHistory = true)
        }
    }

}
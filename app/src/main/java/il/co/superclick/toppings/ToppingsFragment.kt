package il.co.superclick.toppings

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.dm6801.framework.ui.getColor
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.mainColor
import kotlinx.android.synthetic.main.view_pager_toppings.*

class ToppingsFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_PIZZA = "KEY_PIZZA"
        private const val KEY_PRODUCT = "KEY_PRODUCT"
        private const val KEY_CART_ITEM = "KEY_CART_ITEM"
        private const val KEY_ON_FINISH = "KEY_ON_FINISH"
        private const val KEY_LEVEL = "KEY_LEVEL"

        fun open(
            pizza: Boolean,
            product: ShopProduct,
            cartItem: Cart.Item? = null,
            level: Level? = null,
            onFinish: (MutableList<MutableList<Pair<Int, List<Int>>>>?, MutableList<MutableList<Pair<Int, List<Int>>>>?) -> Unit,
        ) {
            open(
                KEY_PIZZA to pizza,
                KEY_PRODUCT to product,
                KEY_CART_ITEM to cartItem,
                KEY_ON_FINISH to onFinish,
                KEY_LEVEL to level
            )
        }
    }

    override val layout = R.layout.view_pager_toppings
    override val themeBackground: Drawable? =
        ColorDrawable(getColor(R.color.dim) ?: Color.TRANSPARENT)
    val viewPager: ViewPager2? get() = toppings_pager

    val pizzaViewPagerAdapter: ToppingsPizzaPagerAdapter? get() = viewPager?.adapter as? ToppingsPizzaPagerAdapter
    val tabLayout: TabLayout? get() = toppings_pager_indicator
    private var pizza: Boolean = false
    private var product: ShopProduct? = null
    private var cartItem: Cart.Item? = null
    private var level: Level? = null
    private val state by lazy {
        ToppingsState(
            cartItem?.toppings?.count()?.minus(1) ?: 0,
            cartItem?.shopToppings?.toMutableList() ?: mutableListOf(),
            cartItem?.shopOptions?.toMutableList() ?: mutableListOf(),
            cartItem?.shopToppings?.indices?.let { with(it) { associateWith { true } }.toMutableMap() }
                ?: mutableMapOf(),
            cartItem?.toppings ?: mutableListOf()
        )
    }
    var onToppingsSave: ((List<List<Pair<Int, List<Int>>>>?, List<List<Pair<Int, List<Int>>>>?) -> Unit)? = null;private set

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_PIZZA] as? Boolean)?.let { pizza = it }
        (arguments[KEY_PRODUCT] as? ShopProduct)?.let { product = it }
        (arguments[KEY_CART_ITEM] as? Cart.Item)?.let { cartItem = it }
        (arguments[KEY_ON_FINISH] as? (List<List<Pair<Int, List<Int>>>>?, List<List<Pair<Int, List<Int>>>>?) -> Unit)?.let {
            onToppingsSave = it
        }
        (arguments[KEY_LEVEL] as? Level)?.let { level = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPager()
    }

    fun initPager() {
        viewPager?.adapter = product?.let { ToppingsPizzaPagerAdapter(this, it, cartItem, if (level == null) state else ToppingsState(0, mutableListOf(), mutableListOf(), mutableMapOf(), mutableListOf()), level) }
        TabLayoutMediator(tabLayout ?: return, viewPager ?: return) { tab, position ->
            if (state.selectedIds?.isNotEmpty() == true)
                tab.overrideIndicatorColor()
            else
                tab.customView = null
        }.attach()
        viewPager?.apply {
            offscreenPageLimit = 3
            setPageTransformer(MarginPageTransformer(40))
        }
    }

    private fun TabLayout.Tab.overrideIndicatorColor() {
        if (customView == null) {
            setCustomView(R.layout.tab_indicator)
            customView?.backgroundTintList = mainColor
        }
    }

    override fun onDestroy() {
        if (level == null) {
            if (cartItem == null) { //new
                if (state.count > 0)
                    onToppingsSave?.invoke(
                        state.associatedToppings.filterIndexed { index, _ -> state.isSaved[index] == true },
                        state.associatedOptions.filterIndexed { index, _ -> state.isSaved[index] == true }
                    )
            } else { //edit
                if (state.count > 0 || Locator.database.cart.has(product?.id ?: -1)) {
                    onToppingsSave?.invoke(
                        state.associatedToppings.filterIndexed { index, _ -> state.isSaved[index] == true },
                        state.associatedOptions.filterIndexed { index, _ -> state.isSaved[index] == true }
                    )
                }
            }
        }
        super.onDestroy()
    }

}
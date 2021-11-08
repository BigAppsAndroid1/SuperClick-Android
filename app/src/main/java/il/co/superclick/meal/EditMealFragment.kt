package il.co.superclick.meal

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.utilities.Log
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.cart.CartAdapter
import il.co.superclick.cart.CartFragment
import il.co.superclick.data.Cart
import il.co.superclick.data.Cart.updateLiveData
import il.co.superclick.data.Meal
import il.co.superclick.data.MealProduct
import il.co.superclick.data.ProductType
import il.co.superclick.dialogs.ProductAlertDialog
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.foregroundFragment
import kotlinx.android.synthetic.main.fragment_cart.*
import kotlinx.android.synthetic.main.fragment_edit_meal.*
import kotlinx.coroutines.delay

class EditMealFragment: BaseFragment() {

    companion object: Comp(){
        private const val KEY_MEAL = "KEY_MEAL"
        private const val KEY_MEAL_INDEX = "KEY_MEAL_INDEX"
        fun open(item: CartAdapter.AdapterItem, mealIndex: Int){
            open(KEY_MEAL to item, KEY_MEAL_INDEX to mealIndex)
        }
    }

    override val layout: Int get() = R.layout.fragment_edit_meal
    private val recyclerContainer: MealEditList? get() = edit_meal_recycler_container
    private val mealName: TextView? get() = edit_meal_title
    private var meal: CartAdapter.AdapterItem? = null
    private var mealIndex: Int? = null


    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_MEAL] as? CartAdapter.AdapterItem)?.let { meal = it }
        (arguments[KEY_MEAL_INDEX] as? Int)?.let { mealIndex = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuBar?.toggleCartButton()
        mealName?.text = meal?.cartItem?.product?.product?.name
        meal?.cartItem?.product?.product?.levels?.forEachIndexed { levelIndex, level ->
            updateList(
                level.description,
                meal?.cartItem?.meals?.first()?.second?.filter { it.level == levelIndex }?.map { it } ?: mutableListOf()
            ){ itemPosInLevel ->
                MealFragment.open(level = levelIndex, Meal(meal?.cartItem?.product ?: return@updateList)){ shopProduct,amount,toppings,options ->
                    val levelPosition = meal?.cartItem?.product?.product?.levels?.subList(0, levelIndex)?.map { it.productsAmount }?.sum()
                    meal?.cartItem?.meals?.first()?.second?.apply {
                        removeAt(levelPosition?.plus(itemPosInLevel) ?: return@open)
                        add(levelPosition.plus(itemPosInLevel), MealProduct(shopProduct, amount, levelIndex, level.toppingsFree,  level.toppingsAddPaid == 1, level.optionsPaid == 0, toppings, options))
                        Cart.updateLiveData()
                        menuBar?.toggleCartButton()
                        Cart.save()

                        if (shopProduct.product.productType == ProductType.PIZZA || shopProduct.isToppings)
                            main {
                                delay(100)
                                foregroundActivity?.popBackStack()
                            }
                        else
                            foregroundActivity?.popBackStack()
                        ProductAlertDialog.productUpdated(meal?.cartItem?.product)
                        refreshRecycler(levelIndex)
                    }
                }
            }
        }
    }

    private fun refreshRecycler(pos:Int){
            val mealEditList = (recyclerContainer as? ViewGroup)?.children?.filter { it.tag != null }?.toList()?.get(pos) as? ViewGroup
            ((mealEditList?.get(1) as? RecyclerView)?.adapter as? EditMealAdapter)?.submitList(meal?.cartItem?.meals?.first()?.second?.filter { it.level == pos }?.map { it } ?: mutableListOf())
    }

    private fun updateList(
        level: String,
        products: List<MealProduct>,
        onItemShouldBeChanged: ((Int) -> Unit)? = null
    ) {
        val recyclerContainer = recyclerContainer ?: return
        var listView = recyclerContainer.findViewWithTag<MealEditList?>(level)
        if (listView == null) {
            listView = MealEditList(recyclerContainer.context)
            recyclerContainer.addView(listView)
            listView.init(
                level,
                products
            ){ pos ->
                onItemShouldBeChanged?.invoke(pos)
            }
        } else {
            listView.submitList(products)
            listView.show()
        }
    }
}
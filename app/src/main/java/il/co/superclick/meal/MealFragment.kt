package il.co.superclick.meal

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.updatePadding
import com.dm6801.framework.utilities.Log
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.data.*
import il.co.superclick.data.Database.cart
import il.co.superclick.infrastructure.App

import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.product_list.*
import il.co.superclick.utilities.delay
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.fragment_meal.*
import kotlin.time.seconds

class MealFragment : BaseFragment() {
    companion object : Comp() {

        private const val KEY_ON_COMPLETE = "ON_COMPLETE"
        private const val KEY_MEAL = "MEAL"
        private const val KEY_LEVEL = "LEVEL"

        fun open(meal: Meal) {
            open(KEY_MEAL to meal)
        }

        fun open(
            level: Int, meal: Meal, onComplete: (
                ShopProduct,
                Float?,
                MutableList<MutableList<Pair<Int, List<Int>>>>?,
                MutableList<MutableList<Pair<Int, List<Int>>>>?
            ) -> Unit
        ) {
            open(KEY_LEVEL to level, KEY_MEAL to meal, KEY_ON_COMPLETE to onComplete)
        }
    }

    override val layout: Int get() = R.layout.fragment_meal
    private val back: ImageView? get() = meal_back
    private val mealDescription: TextView? get() = meal_level_description
    private val recycler: RecyclerView? get() = meal_list_recycler
    private val adapter get() = recycler?.adapter as? ProductsAdapter
    private val levels: List<Level> get() = mealProduct?.product?.levels ?: listOf()
    private val mealProduct: ShopProduct? get() = meal?.shopProduct
    private var currentLevel = 0
        private set(value) {
            field = value
            mealDescription?.text = levels[value].description
            adapter?.submitList(levels[value].products)
            (adapter as? ProductGridAdapter)?.let { it.level = levels[value] } ?: (adapter as? ProductListAdapter)?.let { it.level = levels[value] } ?: (adapter as? ProductHorizontalAdapter)?.let { it.level = levels[value] }
            recycler?.scheduleLayoutAnimation()
        }
    var meal: Meal? = null
    var mealIndex = 0
    private val mealCallback: ((
        ShopProduct,
        Float?,
        MutableList<MutableList<Pair<Int, List<Int>>>>?,
        MutableList<MutableList<Pair<Int, List<Int>>>>?
    ) -> Unit) = { product, amount, toppings, options ->
        addItemToMeal(product, amount, toppings, options)

    }
    private var level: Int? = null

    private var onComplete: ((ShopProduct, Float?, MutableList<MutableList<Pair<Int, List<Int>>>>?, MutableList<MutableList<Pair<Int, List<Int>>>>?) -> Unit)? =
        null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        level?.let { currentLevel = it }
        back?.onClick {
            if (currentLevel > 0 && onComplete == null) {
                if ((meal?.meals?.size ?: 0) > mealIndex) meal?.meals?.get(mealIndex)?.second
                    ?.removeAll(meal?.meals?.get(mealIndex)?.second?.filter { it.level == currentLevel }
                        ?: listOf())
                currentLevel -= 1
                meal?.meals?.get(mealIndex)?.second
                    ?.removeAll(meal?.meals?.get(mealIndex)?.second?.filter { it.level == currentLevel }
                        ?: listOf())
            } else {
                if (meal?.meals?.getOrNull(mealIndex)?.second?.isEmpty() == true) meal?.meals?.removeAt(mealIndex)
                foregroundActivity?.popBackStack()
            }
        }
        mealDescription?.text = levels[currentLevel].description
        if (cart.has(meal?.shopProduct?.id ?: -1)) {
            mealIndex = cart[meal?.shopProduct?.id ?: return]?.meals?.size ?: mealIndex
        }
        initRecycler()
    }

    override fun onResume() {
        super.onResume()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_MEAL] as? Meal)?.let { meal = it }
        (arguments[KEY_LEVEL] as? Int)?.let { level = it }
        (arguments[KEY_ON_COMPLETE] as? ((ShopProduct, Float?, MutableList<MutableList<Pair<Int, List<Int>>>>?, MutableList<MutableList<Pair<Int, List<Int>>>>?) -> Unit))?.let {
            onComplete = it
        }
    }

    private fun initRecycler() {
        recycler?.adapter = when (Shop.listType) {
            ListType.Linear, ListType.LinearBig -> ProductListAdapter(
                mealCallback = onComplete ?: mealCallback,
                levels[currentLevel]
            ) {}
            ListType.Grid -> ProductGridAdapter(
                mealCallback = onComplete ?: mealCallback,
                levels[currentLevel]
            ) {}
            ListType.Horizontal -> {
                recycler?.updatePadding(left = 50.dpToPx, right = 50.dpToPx)
                recycler?.clipToPadding = false
                ProductHorizontalAdapter(
                    mealCallback = onComplete ?: mealCallback,
                    levels[currentLevel]
                ) {}
            }
        }
        currentLevel = currentLevel
    }

    private fun addItemToMeal(
        product: ShopProduct,
        amount: Float?,
        toppings: MutableList<MutableList<Pair<Int, List<Int>>>>?,
        options: MutableList<MutableList<Pair<Int, List<Int>>>>?
    ) {
        if (meal?.meals?.size == mealIndex)
            meal?.meals?.add(true to mutableListOf())

        val currentProduct = meal?.meals?.get(mealIndex)?.second?.firstOrNull{ it.shopProduct.id == product.id && it.level == currentLevel}
        if(currentProduct == null || amount == null)
            meal?.meals?.get(mealIndex)?.second?.add(
                MealProduct(
                    product,
                    amount,
                    currentLevel,
                    levels[currentLevel].toppingsFree,
                    levels[currentLevel].toppingsAddPaid == 1,
                    levels[currentLevel].optionsPaid == 0,
                    toppings,
                    options
                )
            )
        else
            currentProduct.amount = currentProduct.amount?.plus(amount)


        val levelProducts = meal?.meals?.get(mealIndex)?.second?.filter { it.level == currentLevel }
        val productsAmount = levelProducts?.sumBy { it.amount?.toInt() ?: 1 }

        if (productsAmount == levels[currentLevel].productsAmount) {
            if (currentLevel == levels.size - 1) {
                ItemMealDialog.open(0, 0, meal?.shopProduct?.product?.name){
                    meal?.shopProduct ?: kotlin.run { foregroundActivity?.popBackStack(); return@open }
                    open(meal ?: return@open)
                }
                cart.set(
                    mealProduct?.id ?: return,
                    mealProduct?.product?.category ?: return,
                    mealProduct?.unitType?.type ?: return,
                    1f,
                    null,
                    true,
                    meal?.meals,
                    null,
                    null
                )

                return
            }
            main {
                delay(1500){
                    currentLevel += 1
                }
            }

        }
        ItemMealDialog.open(
            productsAmount ?: 0,
            levels[currentLevel].productsAmount
        )
        main {
            delay(1500){
                adapter?.canMealProductButtonAction = true
            }
        }

    }
}



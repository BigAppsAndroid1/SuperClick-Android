package il.co.superclick.widgets

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.dm6801.framework.infrastructure.AbstractFragment
import il.co.superclick.R
import il.co.superclick.search.SearchFragment
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.ui.*
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.delay
import il.co.superclick.cart.CartFragment
import il.co.superclick.data.Shop
import il.co.superclick.fragments.MenuFragment
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.meal.EditMealFragment
import il.co.superclick.order.CreditCardFragment
import il.co.superclick.order.OrderDetailsFragment
import il.co.superclick.utilities.*
import kotlinx.android.synthetic.main.view_menu_bar.view.*
import kotlinx.coroutines.*

@Suppress("UsePropertyAccessSyntax")
class MenuBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val activity get() = foregroundActivity
        private val cart get() = Locator.database.cart
    }

    private val filledButton: Drawable?
        get() = getDrawable(R.drawable.cart_button_filled)?.apply { setTint(Shop.getShopColor()) }
    private val whiteStrokeButton: Drawable? get() = getDrawable(R.drawable.cart_button)

    private val back: ImageView? get() = menu_back
    private val button: ImageView? get() = menu_button
    private val searchButton: ImageView? get() = menu_search
    private val priceView: TextView? get() = menu_bar_sum
    private val cartButton: LinearLayout? get() = menu_bar_cart_button
    private val cartBadge: TextView? get() = menu_bar_cart_badge
    private val cartImage: ImageView? get() = menu_bar_cart_image
    private val cartText: TextView? get() = menu_bar_label
    private val fragmentTitle: TextView get() = fragment_title

    val searchEdit: EditText? get() = menu_search_edit
    val isSearchMode: Boolean get() = searchEdit?.isVisible == true

    private val sumObserver = Observer<Double> { sum ->
        priceView?.setText("₪${currencyFormatter.format(sum)}")
        //priceView?.setTextSize(sum)
    }

    private val livePendingItemsSizeObserver = Observer<Int> { size ->
        cartBadge?.isVisible = listOf(SearchFragment().TAG, CreditCardFragment().TAG, OrderDetailsFragment().TAG).firstOrNull {
            it == foregroundFragment?.TAG
        } == null
        if(cartBadge?.isVisible == true){
            cartBadge?.setText(size.toString())
        }
    }

    init {
        inflate(context, R.layout.view_menu_bar, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setBackgroundResource(R.color.menu_bar_background)
        setLayoutTransition(LayoutTransition())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cart.liveSum.observeForever(sumObserver)
        cart.livePendingItemsSize.observeForever(livePendingItemsSizeObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cart.liveSum.removeObserver(sumObserver)
        cart.livePendingItemsSize.removeObserver(livePendingItemsSizeObserver)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        button?.isVisible = foregroundFragment !is EditMealFragment
        button?.onClick { MenuFragment.open() }
        initCart()
        toggleCartButtonColor(false)
        toggleBack((activity?.supportFragmentManager?.fragments?.filterIsInstance<AbstractFragment>()?.size ?: 0) > 1 )
        toggleSearch(true)
    }

    private fun initCart() {
        cartButton?.onClick {
            if (foregroundFragment?.javaClass == CartFragment().javaClass) return@onClick
            CartFragment.open()
        }
    }

    fun toggleBack(isEnabled: Boolean) {
        if (isEnabled) {
            back?.isVisible = true
            back?.onClick { activity?.navigateBack() }
        } else {
            back?.isGone = true
            back?.onClick { }
        }
    }

    private var job: Job? = null

    private val textListener = EditableTextWatcher(searchEdit) { text ->
        if (text.length < 2) return@EditableTextWatcher
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            delay(300)
            if (foregroundFragment !is SearchFragment) {
                searchEdit?.isInvisible = true
                setText("")
            }
            job = null
        }
        search(searchEdit?.value?.trim())
    }

    fun toggleSearch(isEnabled: Boolean) {
        if (isEnabled) {
            searchButton?.isVisible = true
            if(foregroundFragment is SearchFragment) searchButton?.isVisible = false
            searchButton?.onClick {
                if(foregroundFragment !is SearchFragment) {
                    SearchFragment.open()
                    delay(200, Dispatchers.Main) {
                        searchEdit?.text?.clear()
                        toggleSearchEdit(false)
                        searchEdit?.requestFocus()
                        showKeyboard()
                    }
                }
            }
            searchEdit?.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && searchEdit?.text.isNullOrBlank()) exitSearch(clear = true)
            }
            searchEdit?.filterEmoticons()
            catch { searchEdit?.removeTextChangedListener(textListener) }
            searchEdit?.addTextChangedListener(textListener)
        } else {
            searchButton?.isGone = true
            searchButton?.onClick { }
            catch { searchEdit?.removeTextChangedListener(textListener) }
        }
    }

    fun toggleSearchEdit(isVisible: Boolean) {
        if (isVisible) searchEdit?.isVisible = true
        else searchEdit?.isInvisible = true
    }

    fun toggleCartButton(isInvisible: Boolean = true) {
        cartButton?.isInvisible = isInvisible
        cartBadge?.isInvisible = isInvisible
        priceView?.isInvisible = isInvisible
        cartImage?.isInvisible = isInvisible
        cartText?.isInvisible = isInvisible
    }

    fun toggleButtonsColor() {
        val colorStateList = mainColor
        back?.imageTintList = colorStateList
        searchButton?.imageTintList = colorStateList
        button?.imageTintList = colorStateList
    }

    fun setFragmentTitle(title: String) {
        fragmentTitle.isVisible = true
        fragmentTitle.text = title
    }

    private fun search(value: String?) {
        if (value?.isNotBlank() == true) {
            if (value.length >= 2) {
                SearchFragment.open(value)
                exitSearch(clear = false)
            }
        } else if (value.isNullOrBlank()) {
            exitSearch()
        }
    }

    fun exitSearch(clear: Boolean = true) {
        if (foregroundFragment is SearchFragment) return
        toggleSearchEdit(false)
        if (clear) searchEdit?.text?.clear()
        hideKeyboard(searchEdit)
    }

    fun toggleCartButtonColor(isFilled: Boolean) {
        cartButton?.background = if (isFilled) filledButton else whiteStrokeButton
        cartBadge?.setTextColor(
            if (isFilled) Shop.getShopColor() else getColor(R.color.white) ?: return
        )
        cartBadge?.backgroundTintList = ColorStateList.valueOf(
            if (isFilled) getColor(R.color.white) ?: return else Shop.getShopColor()
        )
    }

}
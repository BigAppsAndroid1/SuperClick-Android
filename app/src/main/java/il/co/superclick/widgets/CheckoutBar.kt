package il.co.superclick.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import il.co.superclick.infrastructure.Locator
import il.co.superclick.R
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.utilities.disable
import il.co.superclick.utilities.enable
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.ui.setText
import il.co.superclick.cart.CartFragment
import kotlinx.android.synthetic.main.view_checkout_bar.view.*

@Suppress("UsePropertyAccessSyntax")
class CheckoutBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val cart get() = Locator.database.cart
        private val fragment get() = foregroundFragment as? BaseFragment
    }

    private val priceView: PriceView? get() = checkout_bar_sum
    private val button: Button? get() = checkout_bar_button
//    private var action: () -> Unit =
//        { CartFragment.open(cart.products.filterValues { it.isChecked }.keys.toList()) }

    private val sumObserver = Observer<Double> { sum ->
        priceView?.set(sum)
        if (sum > 0) button?.enable()
        else button?.disable()
    }

    init {
        inflate(context, R.layout.view_checkout_bar, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setBackgroundResource(R.color.checkout_bar_background)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
//        button?.onClick { action() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cart.liveSum.observe(fragment ?: return, sumObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cart.liveSum.removeObserver(sumObserver)
    }

    fun set(text: Any? = null, action: (() -> Unit)? = null) {
        text?.let { button?.setText(it) }
//        action?.let { this.action = it }
    }

}

package il.co.superclick.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import il.co.superclick.infrastructure.Locator
import il.co.superclick.R
import il.co.superclick.utilities.dial
import com.dm6801.framework.ui.onClick
import il.co.superclick.utilities.getString
import kotlinx.android.synthetic.main.view_customer_support.view.*

class CustomerSupportWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val shop get() = Locator.database.shop
    }

    private val textView: TextView? get() = customer_support

    init {
        inflate(context, R.layout.view_customer_support, this)
        setBackgroundResource(R.drawable.rect_white_corners_8)
        elevation = 4f
        isVisible = setPhone()
        if (isInEditMode) layoutParams =
            (layoutParams ?: ViewGroup.LayoutParams(context, attrs)).apply { height = 48 }
    }

    private fun setPhone(): Boolean {
        return shop?.extraPhone?.let { phone ->
            textView?.text = getString(R.string.customer_support_phone, phone)
            onClick { dial(phone) }
            true
        } ?: false
    }

}
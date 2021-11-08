package il.co.superclick.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dm6801.framework.ui.getDrawable
import il.co.superclick.R
import com.dm6801.framework.ui.onClick
import il.co.superclick.data.ListType
import il.co.superclick.data.Shop
import il.co.superclick.data.ShopProduct
import il.co.superclick.data.UnitType
import kotlinx.android.synthetic.main.widget_amount.view.*
import kotlin.math.roundToInt

class AmountWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val text: TextView? get() = widget_amount_text
    private val add: ImageView? get() = widget_amount_add
    private val remove: ImageView? get() = widget_amount_remove


    var callback: (()->Unit)? = null

    var unitTypeName: String = ""
    var amount: Float = 0f; private set
    private var productId: Int? = null
    private var multiplier: Float = 1f

    init {
        inflate(context, R.layout.widget_amount, this)
        if (Shop.listType == ListType.Linear)
            background = getDrawable(R.drawable.rect_grey_border_corners)
        else
            background = getDrawable(R.drawable.rect_gray_border_corners_8)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initAdd()
        initRemove()
    }

    fun setProduct(product: ShopProduct?, unit: UnitType? = product?.unitType) {
        if (product == null || unit == null) return
        productId = product.id
        setUnitType(unit)

    }

    fun setUnitType(unit: UnitType) {
        unitTypeName = unit.type
        multiplier = unit.multiplier
        setAmount(unit.multiplier)
        initAdd()
        initRemove()
    }

    fun setAmount(amount: Float) {
        if (amount < multiplier) return
        this.amount = amount
        this.text?.text = ((amount * 100).roundToInt() / 100f).text
    }

    private fun initAdd() {
        add?.onClick(300) {
            setAmount(amount + multiplier)
            callback?.invoke()
        }
    }

    private fun initRemove() {
        remove?.onClick(300) {
            val oldAmount = amount
            setAmount((amount - multiplier).coerceAtLeast(multiplier))
            if(oldAmount != amount) callback?.invoke()
        }
    }

    private val Float.text: String
        get() {
            val int = toInt()
            return if (int != 0 && this / int == 1f) int.toString()
            else toString()
        }

}

package il.co.superclick.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import il.co.superclick.R
import il.co.superclick.language.Language
import il.co.superclick.language.LanguageManager
import il.co.superclick.utilities.currencyFormatter
import il.co.superclick.utilities.setThemeColor
import kotlinx.android.synthetic.main.view_price.view.*
import java.util.*

class PriceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val textView: TextView? get() = view_price
    private val currencySign: TextView? get() = view_price_currency
    var value: Double? = null; private set
    private var textSize: Float =  if (LanguageManager.locale == Language.French.locale) 16f else 20f
    private var textColor: Int = Color.BLACK

    init {
        inflate(context, R.layout.view_price, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        context.obtainStyledAttributes(attrs, defStyleAttr, defStyleRes)
    }

    @SuppressLint("Recycle")
    private fun Context.obtainStyledAttributes(
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        try {
            obtainStyledAttributes(attrs, R.styleable.PriceView, defStyleAttr, defStyleRes).use {
                textSize =
                    it.getDimensionPixelSize(R.styleable.PriceView_android_textSize, textSize.toInt()).toFloat()
                textColor =
                    it.getColor(
                        R.styleable.PriceView_android_textColor,
                        ContextCompat.getColor(context, R.color.text_color)
                    )
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setStyle()
    }

    @SuppressLint("SetTextI18n")
    fun set(value: Double, code: String = "ils") {
        try {
            this.value = value
            textView?.text = currencyFormatter.format(value)
            Currency.getInstance(code)?.apply { currencySign?.text = symbol }
            setStyle()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setThemeColor(){
        textView?.setThemeColor()
        currencySign?.setThemeColor()
    }

    fun setTextToBold(){
        textView?.setTypeface(null, Typeface.BOLD)
        currencySign?.setTypeface(null, Typeface.BOLD)
    }

    fun setTextSize(sum: Double) {
        if (sum % 100 >= 1) {
          var size =
                ((if (LanguageManager.locale == Language.French.locale) 0.78 else 0.82) * textSize).toFloat()
            if (sum / 1000 >= 1) {
                size = textSize
            }

                textView?.setSize(size)
                textView?.setColor(textColor)
                currencySign?.setStyle(
                    (size * if (LanguageManager.locale == Language.French.locale) 0.7 else 0.8).toFloat(),
                    textColor
                )

        }
    }

    private fun setStyle() {
        textView?.setColor(textColor)
        currencySign?.setStyle((textSize * if (LanguageManager.locale == Language.French.locale) 0.7 else 0.8).toFloat(), textColor)
    }

    private fun TextView.setStyle(textSize: Float, textColor: Int) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        setTextColor(textColor)
    }
    private fun TextView.setColor(textColor: Int) {
        setTextColor(textColor)
    }
    private fun TextView.setSize(textSize: Float) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

    }
}

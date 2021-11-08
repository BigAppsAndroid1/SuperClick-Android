package il.co.superclick.widgets

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.getColor
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.ui.updatePadding
import il.co.superclick.R
import il.co.superclick.data.UnitType
import il.co.superclick.utilities.mainColor
import kotlinx.android.synthetic.main.widget_unit_type.view.*

class UnitTypeWidget3 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val CHECKED_BACKGROUND = R.drawable.unit_type_widget_2
        private const val UNCHECKED_BACKGROUND = R.drawable.rect_gray_border_corners_8
        private const val CHECKED_TEXT = Color.WHITE
        private val UNCHECKED_TEXT by lazy { getColor(R.color.colorPrimaryDark) ?: Color.BLACK }
    }

    private val kg: TextView? get() = widget_unit_type_kg
    private val unit: TextView? get() = widget_unit_type_unit

    val type: UnitType? get() = types.find { it.type == selected }
    private var types: List<UnitType> = emptyList()
    private var selected: String? = null
    private var callback: ((UnitType) -> Unit)? = null

    init {
        inflate(context, R.layout.widget_unit_type3, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setLayout()
        updatePadding(if (!isInEditMode) 1.dpToPx else 2)
    }

    private fun setLayout() {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun set(types: List<UnitType>?, default: String) {
        this.types = types ?: return
        callback = null
        kg?.apply {
            if (types.find { it.type == UnitType.UNIT_KG } != null) {
                isVisible = true
                paintFlags = 0
                onClick {
                    set(UnitType.UNIT_KG)
                    setStyle(UnitType.UNIT_KG)
                }
            } else {
                isVisible = false
                onClick { }
            }
        }
        unit?.apply {
            if (types.find { it.type == UnitType.UNIT } != null) {
                isVisible = true
                paintFlags = 0
                onClick {
                    set(UnitType.UNIT)
                    setStyle(UnitType.UNIT)
                }
            } else {
                isVisible = false
                onClick { }
            }
        }
        set(default)
    }

    fun set(unitType: String) {
        val type = types.find { it.type == unitType }
        type?.type?.let {
            setStyle(it)
            selected = unitType
            callback?.invoke(type)}
    }

    private fun setStyle(type: String) {
        when (type) {
            UnitType.UNIT_KG -> {
                kg?.setTextColor(CHECKED_TEXT)
                kg?.setBackgroundResource(CHECKED_BACKGROUND)
                kg?.backgroundTintList = mainColor
                kg?.isVisible = true
                unit?.setTextColor(UNCHECKED_TEXT)
                unit?.setBackgroundResource(UNCHECKED_BACKGROUND)
                unit?.backgroundTintList = null
                unit?.isGone = types.size == 1
            }
            UnitType.UNIT -> {
                unit?.setTextColor(CHECKED_TEXT)
                unit?.setBackgroundResource(CHECKED_BACKGROUND)
                unit?.backgroundTintList = mainColor
                unit?.isVisible = true
                kg?.setTextColor(UNCHECKED_TEXT)
                kg?.backgroundTintList = null
                kg?.setBackgroundResource(UNCHECKED_BACKGROUND)
                kg?.isGone = types.size == 1
            }

        }
    }

    fun setCallback(action: (UnitType) -> Unit) {
        callback = action
    }

}
package il.co.superclick.widgets

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import il.co.superclick.R
import com.dm6801.framework.ui.*
import il.co.superclick.data.ListType
import il.co.superclick.data.Shop
import il.co.superclick.data.UnitType
import kotlinx.android.synthetic.main.widget_unit_type.view.*

class UnitTypeWidget2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val CHECKED_BACKGROUND = R.drawable.unit_type_widget_2
        private const val UNCHECKED_BACKGROUND = R.drawable.rect_gray_border_corners_8
        private const val UNCHECKED_BACKGROUND_BIG_ITEM = R.drawable.rect_grey_border_corners

        private const val CHECKED_TEXT = Color.BLACK
        private val UNCHECKED_TEXT by lazy { getColor(R.color.colorPrimaryDark) ?: Color.BLACK }
    }

    private val kg: TextView? get() = widget_unit_type_kg
    private val unit: TextView? get() = widget_unit_type_unit

    val type: UnitType? get() = types.find { it.type == selected }
    private var types: List<UnitType> = emptyList()
    private var selected: String? = null
    private var callback: ((UnitType) -> Unit)? = null

    init {
        inflate(context, R.layout.widget_unit_type_2, this)
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
                paintFlags = 0
                onClick {
                    set(UnitType.UNIT_KG)
                    setStyle(UnitType.UNIT_KG)
                }
            } else {
                onClick { }
            }
        }
        unit?.apply {
            if (types.find { it.type == UnitType.UNIT } != null) {
                paintFlags = 0
                onClick {
                    set(UnitType.UNIT)
                    setStyle(UnitType.UNIT)
                }
            } else {
                onClick { }
            }
        }
        set(default)
    }

    fun set(unitType: String) {
        val type = types.find { it.type == unitType } ?: return
        setStyle(type.type)
        selected = unitType
        callback?.invoke(type)
    }

    private fun setStyle(type: String) {
        when (type) {
            UnitType.UNIT_KG -> {
                kg?.setTextColor(CHECKED_TEXT)
                if (Shop.listType == ListType.Linear)
                    kg?.setBackgroundResource(UNCHECKED_BACKGROUND_BIG_ITEM)
                else
                    kg?.setBackgroundResource(UNCHECKED_BACKGROUND)
                kg?.isVisible = true
                kg?.updateMargins(left = 10.dpToPx, right = 10.dpToPx)
                unit?.setTextColor(UNCHECKED_TEXT)
                unit?.setBackgroundColor(Color.TRANSPARENT)
                if (types.size == 1) {
                    unit?.isVisible = false
                }
            }
            UnitType.UNIT -> {
                unit?.setTextColor(CHECKED_TEXT)
                if (Shop.listType == ListType.Linear)
                    unit?.setBackgroundResource(UNCHECKED_BACKGROUND_BIG_ITEM)
                else
                    unit?.setBackgroundResource(UNCHECKED_BACKGROUND)
                unit?.isVisible = true
                unit?.updateMargins(left = 10.dpToPx, right = 10.dpToPx)
                kg?.setTextColor(UNCHECKED_TEXT)
                if (Shop.listType == ListType.Linear)
                    kg?.setBackgroundColor(UNCHECKED_BACKGROUND)
                else
                    kg?.setBackgroundColor(UNCHECKED_BACKGROUND_BIG_ITEM)
                if (types.size == 1) {
                    kg?.isVisible = false
                }
            }
        }
    }

    fun setCallback(action: (UnitType) -> Unit) {
        callback = action
    }

}
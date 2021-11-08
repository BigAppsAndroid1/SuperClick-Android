package il.co.superclick.meal

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import il.co.superclick.R
import il.co.superclick.data.Cart
import il.co.superclick.data.MealProduct
import il.co.superclick.infrastructure.Locator
import kotlinx.android.synthetic.main.view_edit_meal_list.view.*

@Suppress("UsePropertyAccessSyntax")
open class MealEditList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val LAYOUT = R.layout.view_edit_meal_list
        private val database get() = Locator.database
    }

    protected val text: TextView? get() = list_level_name
    protected val recyclerView: RecyclerView? get() = list_level
    protected val _adapter: EditMealAdapter? get() = recyclerView?.adapter as? EditMealAdapter
    private val adapter: EditMealAdapter? get() = _adapter

    protected var level: String? = null

    init {
        inflate(context, LAYOUT, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        orientation = VERTICAL
        setLayout()
    }

    private fun setLayout() {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun init(
        level: String,
        products: List<MealProduct>,
        onItemEditTapped:(Int) -> Unit
    ) {
        this.level = level
        tag = level
        text?.paintFlags = text?.paintFlags?.plus(Paint.UNDERLINE_TEXT_FLAG) ?: Paint.UNDERLINE_TEXT_FLAG
        text?.isVisible = true
        text?.text = level
        recyclerView?.adapter = EditMealAdapter(onItemEditTapped)
        submitList(products)
    }

    fun submitList(products: List<MealProduct>, callback: (() -> Unit)? = null) {
        adapter?.submitList(products) {
            hideIfEmpty()
            callback?.invoke()
        }
    }

    fun hideIfEmpty() {
        if (_adapter?.getItemCount() == 0) hide()
    }

    fun show() = setVisibility(View.VISIBLE)
    fun hide() = setVisibility(View.GONE)


}
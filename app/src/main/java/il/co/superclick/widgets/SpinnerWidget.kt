package il.co.superclick.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintLayout
import il.co.superclick.R
import il.co.superclick.data.ShopBranch
import il.co.superclick.infrastructure.Locator.database
import kotlinx.android.synthetic.main.widget_spinner.view.*

class SpinnerWidget  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object{
        private const val LAYOUT = R.layout.widget_spinner
    }

    private val spinnerList: ListView? get() = widget_spinner_list
    private var branches = database.shop?.branches?.toMutableList() ?: mutableListOf()
    var onBranchChecked: ((ShopBranch?) -> Unit)? = null

    init {
        inflate(context, LAYOUT, this)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }
    
    override fun onFinishInflate() {
        super.onFinishInflate()
        spinnerList?.adapter = ArrayAdapter(context, R.layout.widget_soinner_simple_list_item, branches.map{ it.name }.toMutableList().apply { add(0, "מאיזה סניף תרצה להזמין") })
        spinnerList?.setOnItemClickListener { _, _, i, _ ->
            if (i == 0)  onBranchChecked?.invoke(null)
            else onBranchChecked?.invoke(branches[i-1])
        }
    }

}



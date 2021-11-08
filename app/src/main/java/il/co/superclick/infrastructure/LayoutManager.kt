package il.co.superclick.infrastructure

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import il.co.superclick.data.ListType
import il.co.superclick.data.Shop
import il.co.superclick.search.SearchFragment

class LayoutManager(context: Context, rows: Int) : GridLayoutManager(context, rows) {
    var isScrollable: Boolean = true

    val visibleItemPositions: IntRange
        get() = findFirstVisibleItemPosition()..findLastVisibleItemPosition()

    val completelyVisibleItemPositions: IntRange
        get() = findFirstCompletelyVisibleItemPosition()..findLastCompletelyVisibleItemPosition()

    override fun setOrientation(orientation: Int) {
        when {
            foregroundFragment is SearchFragment -> super.setOrientation(VERTICAL)
            Shop.listType == ListType.Horizontal -> super.setOrientation(HORIZONTAL)
            else -> super.setOrientation(VERTICAL)
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return super.canScrollHorizontally() && isScrollable
    }

    override fun canScrollVertically(): Boolean {
        return super.canScrollVertically() && isScrollable
    }

    fun isViewCompletelyVisible(view: View): Boolean {
        return getPosition(view) in completelyVisibleItemPositions
    }
}
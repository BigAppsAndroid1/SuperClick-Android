package il.co.superclick.infrastructure

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.dm6801.framework.infrastructure.AbstractFragment
import il.co.superclick.R
import il.co.superclick.search.SearchFragment
import il.co.superclick.widgets.MenuBar
import il.co.superclick.widgets.CategoriesBar
import com.dm6801.framework.ui.*
import com.google.android.material.snackbar.Snackbar
import il.co.superclick.MainActivity
import il.co.superclick.data.Shop
import il.co.superclick.infrastructure.Locator.database

abstract class BaseFragment : AbstractFragment() {

    override val activity: MainActivity?
        get() = super.activity as? MainActivity

    protected open val themeBackground: Drawable? = Shop.getBackground()?.let { getDrawable(it) } ?: getDrawable(R.drawable.bg_3)
    val menuBar: MenuBar? get() = view?.findViewById(R.id.menu_bar)
    val categoriesBar: CategoriesBar? get() = view?.findViewById(R.id.categories_bar)

    protected open val isMenuBar: Boolean = true
    protected open val isSwitchBar: Boolean = true
    protected open val isCheckoutBar: Boolean = true
    protected open val isSearch: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = themeBackground
        menuBar?.isVisible = isMenuBar
        categoriesBar?.isVisible = isSwitchBar && (database.shop?.categories?.size ?: 0) > 1
        if (isMenuBar) {
            menuBar?.toggleSearch(isSearch)
        }
    }

    override fun onBackPressed(): Boolean {
        if (menuBar?.isSearchMode == true && this !is SearchFragment) {
            menuBar?.exitSearch()
            return true
        }
        return super.onBackPressed()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean? {
        if (menuBar?.searchEdit?.hasFocus() == true &&
            ev?.let { !menuBar?.searchEdit.wasClicked(it) } == true &&
            menuBar?.searchEdit?.text.isNullOrBlank()
        ) menuBar?.exitSearch()
        return super.dispatchTouchEvent(ev)
    }

    fun showSnackBar(text: String){
        val snack: Snackbar = Snackbar.make(view ?: return, text, Snackbar.LENGTH_LONG)
        snack.setTextColor(getColor(R.color.grey) ?: Color.GRAY)
        snack.animationMode = Snackbar.ANIMATION_MODE_FADE
        snack.duration = 600
        val snackView = snack.view
        val mTextView = snackView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        mTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackView.setBackgroundColor(Color.WHITE)
        val params = snackView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        snackView.layoutParams = params
        snackView.updateMargins(top = 48.dpToPx)
        snack.show()
    }

}
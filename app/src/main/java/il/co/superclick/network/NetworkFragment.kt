package il.co.superclick.network

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.AbstractFragment
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.utilities.openWebBrowser
import il.co.superclick.R
import il.co.superclick.infrastructure.App
import il.co.superclick.infrastructure.Locator.database
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.fragment_network.*


class NetworkFragment : AbstractFragment() {

    companion object : Comp()

    override val layout: Int get() = R.layout.fragment_network
    private val networkList: RecyclerView? get() = network_list
    private val title: TextView? get() = network_title
    private val filter: ImageView? get() = network_search
    private val noResults: View? get() = no_results
    private val disableFilters: TextView? get() = disable_filters
    private val imageFilter: ImageView? get() = filter_image
    private val accessibilitiesButton: View? get() = network_accessibilities_button

    private val networkAdapter: NetworkAdapter = NetworkAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title?.text = database.network?.welcomeMessage
        (foregroundApplication as? App)?.accessibilityLink?.takeIf { it.isNotBlank() }?.let { link ->
            accessibilitiesButton?.isVisible = true
            accessibilitiesButton?.onClick {
                openWebBrowser(link)
            }
        }
        filter?.onClick {
            TagsFilterFragment.open {
                filterShops()
            }
        }
        disableFilters?.onClick {
            database.network?.tags?.forEach { it.isChecked = false }
            filterShops()
            toggleFilter()
        }
        networkList?.adapter = networkAdapter

        networkAdapter.submitList(database.network?.shops)
        noResults?.isVisible = database.network?.shops.isNullOrEmpty()
    }


    override fun onForeground() {
        super.onForeground()
        networkAdapter.isClickable = true
    }

    private fun filterShops() {
        val shops = database.network?.shops?.filter { shop ->
            database.network?.tags?.filter { it.isChecked }
                ?.firstOrNull { shop.tags.contains(it.id) } != null || database.network?.tags?.all { !it.isChecked } == true
        }
        networkAdapter.submitList(shops)
        networkAdapter.notifyDataSetChanged()
        noResults?.isVisible = shops.isNullOrEmpty()
        if (database.network?.tags?.firstOrNull { it.isChecked } != null)
            toggleFilter(0f)
        else
            toggleFilter()
    }

    private fun toggleFilter(translation: Float = 110.dpToPx.toFloat()) {
        disableFilters?.animate()?.translationX(translation)?.duration = 500
        imageFilter?.animate()?.translationX(translation)?.duration = 500
    }

}
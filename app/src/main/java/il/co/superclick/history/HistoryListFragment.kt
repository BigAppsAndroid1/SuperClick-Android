package il.co.superclick.history

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.remote.Remote
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.ui.safeLaunch
import com.dm6801.framework.ui.set
import com.dm6801.framework.ui.viewModel
import com.dm6801.framework.utilities.suspendCatch
import il.co.superclick.data.HistoryOrder
import il.co.superclick.product_list.ProductListFragment
import kotlinx.android.synthetic.main.fragment_history_list.*
import kotlinx.coroutines.Dispatchers

class HistoryListFragment : BaseFragment() {

    companion object : Comp() {
        private val shop get() = Locator.database.shop

        fun open() {
            if (foregroundFragment?.javaClass == clazz) return
            open(replace = false)
        }
    }

    override val layout = R.layout.fragment_history_list
    private val emptyListView: ViewGroup? get() = history_empty_container
    private val emptyListViewButton: ImageView? get() = history_empty_container_button
    private val recycler: RecyclerView? get() = history_recycler
    private val adapter: HistoryListAdapter? get() = recycler?.adapter as? HistoryListAdapter
    private val viewModel: ViewModel? get() = viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        initRecycler()
        initEmptyCartButton()
        subscribe()
    }

    override fun onForeground() {
        super.onForeground()
        setToolbar()
    }

    private fun setToolbar() {
        menuBar?.toggleCartButton()
        menuBar?.setFragmentTitle(getString(R.string.history_title))
    }

    override fun onResume() {
        super.onResume()
        viewModel?.fetch()
    }

    private fun initRecycler() {
        recycler?.adapter = HistoryListAdapter()
    }

    private fun subscribe() {
        viewModel?.results?.removeObservers(viewLifecycleOwner)
        viewModel?.results?.observe(viewLifecycleOwner) { orders ->
            emptyListView?.isVisible = orders == null || orders.isEmpty()
            if (orders != null) {
                adapter?.submitList(orders)
            }
        }
    }

    private fun initEmptyCartButton() {
        emptyListViewButton?.onClick {
           activity?.onBackPressed()
        }
    }

    class ViewModel : androidx.lifecycle.ViewModel() {
        val results: LiveData<List<HistoryOrder>> = MutableLiveData(emptyList())

        fun fetch() = safeLaunch(Dispatchers.IO) {
            suspendCatch {
                results.set(Remote.getHistory()?.sortedByDescending { it.created }?: emptyList())
            }
        }
    }

}
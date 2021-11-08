package il.co.superclick.product_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dm6801.framework.infrastructure.hideProgressBar
import il.co.superclick.infrastructure.Locator
import com.dm6801.framework.ui.safeLaunch
import com.dm6801.framework.utilities.background
import com.dm6801.framework.utilities.main
import com.dm6801.framework.utilities.suspendCatch
import il.co.superclick.data.ProductPage
import il.co.superclick.data.ShopProduct
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ProductListViewModel : ViewModel() {

    companion object {
        private val database get() = Locator.database
        private val repository get() = Locator.repository
    }

    private val isFetching = AtomicBoolean(false)
    private val fetched = ConcurrentHashMap<String, ProductPage>()
    val subCategory: MutableLiveData<String?> = MutableLiveData(null)

    fun getLiveData(category: String): LiveData<List<ShopProduct>> = repository.getLiveData(category)

    fun preload(category: String, force: Boolean = false) {
        if (isFetching.get()) return
        val lastPage = fetched[category] ?: run{ fetch(category); return}
        val nextPage = lastPage.page + 1
        if (nextPage > lastPage.pages && !force) return
        fetch(category, nextPage, lastPage.pageSize)
    }

    fun fetch(category: String, page: Int = 1, pageSize: Int = 20) = safeLaunch(Dispatchers.IO) {
        if (database.findCategory(category) == null) {
            isFetching.set(false)
            return@safeLaunch
        }
        isFetching.set(true)
        suspendCatch {
            repository.fetch(category, page, pageSize)?.let { response ->
                if (response.products.isEmpty()) return@let
                fetched[category] = response.copy(products = emptyList())
                background { fetched[category]?.products?.forEach {  } }
                main { hideProgressBar() }
            }
        }
        isFetching.set(false)
    }

}
package il.co.superclick.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import il.co.superclick.utilities.set
import com.dm6801.framework.ui.safeLaunch
import com.dm6801.framework.utilities.suspendCatch
import il.co.superclick.data.ProductsRepository
import il.co.superclick.data.ShopProduct
import kotlinx.coroutines.Dispatchers

class SearchViewModel : ViewModel() {

    val results: LiveData<List<ShopProduct>> = MutableLiveData(emptyList())

    fun fetch(text: String) = safeLaunch(Dispatchers.IO) {
        suspendCatch {
            results.set(ProductsRepository.search(text))
        }
    }

}
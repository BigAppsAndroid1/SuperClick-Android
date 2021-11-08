package il.co.superclick.order.address_lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dm6801.framework.utilities.Log
import il.co.superclick.remote.Place
import il.co.superclick.remote.PlacesApi
import kotlinx.coroutines.*

class AddressLookupViewModel : ViewModel() {

    var lastSearch: String? = null; private set
    private var job: Job? = null

    fun search(text: String, onSuccess: (List<Place>) -> Unit) {
        job?.cancel()
        job = null
        job = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            this@AddressLookupViewModel.Log("Places AutoComplete: text=$text")
            PlacesApi.autocomplete(
                text
            ) {
                onSuccess(PlacesApi.parseAutocompleteResult(it))
            }
        }
    }
}
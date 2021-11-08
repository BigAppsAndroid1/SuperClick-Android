package il.co.superclick.order.address_lookup

import android.view.View
import android.widget.TextView
import il.co.superclick.R
import il.co.superclick.infrastructure.RecyclerAdapter
import il.co.superclick.remote.Place
import il.co.superclick.remote.PlacesApi
import il.co.superclick.utilities.CoOrds
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.item_address_lookup.view.*

class AddressLookupAdapter(
    val onItemClick: (Place) -> Unit
) : RecyclerAdapter<AddressLookupAdapter.ViewHolder, AddressLookupAdapter.Address>() {

    override val layout = R.layout.item_address_lookup
    override val viewHolderClass = ViewHolder::class.java

    @Suppress("UNUSED_PARAMETER", "unused")
    fun submitList(places: List<Place>, vararg a: Unit) {
        submitList(places, callback = null)
    }

    @Suppress("UNUSED_PARAMETER")
    fun submitList(places: List<Place>, vararg a: Unit, callback: (() -> Unit)? = null) {
        val filteredList =
            places.filter { it.address?.isNotBlank() == true && it.name?.isNotBlank() == true }

        super.submitList(filteredList.map {
            Address(
                id = it.placeId.hashCode(),
                placeId = it.placeId,
                street = it.name ?: "",
                municipality = it.address?.removePrefix(it.name ?: "") ?: "",
                coordinates = it.coordinates
            )
        }, callback)
    }

    inner class ViewHolder(itemView: View) :
        RecyclerAdapter.ViewHolder<Address>(itemView) {
        private val streetTextView: TextView? get() = itemView.address_lookup_street
        private val municipalityTextView: TextView? get() = itemView.address_lookup_municipality

        override fun bind(item: Address, position: Int, payloads: MutableList<Any>?) {
            super.bind(item, position, payloads)
            streetTextView?.text = item.street
            municipalityTextView?.text = item.municipality
            itemView.onClick { onItem(item) }
        }
    }

    private fun onItem(address: Address) {
        PlacesApi.getDetails(address.placeId) { response ->
            PlacesApi.parseDetailsResult(response)?.let(onItemClick)
        }
    }

    @Suppress("ArrayInDataClass")
    data class Address(
        override val id: Int,
        val placeId: String,
        val street: String,
        val municipality: String,
        val coordinates: CoOrds?
    ) : Identity<Int> {
        override fun compareTo(other: Int) = id.compareTo(other)
    }

}
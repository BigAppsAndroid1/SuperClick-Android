package il.co.superclick.network

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.infrastructure.showProgressBar
import com.dm6801.framework.utilities.main
import com.dm6801.framework.utilities.suspendCatch
import il.co.superclick.R
import il.co.superclick.data.Cart
import il.co.superclick.data.Database
import il.co.superclick.data.ShopProduct
import il.co.superclick.data.ShortShop
import il.co.superclick.infrastructure.Locator.database
import il.co.superclick.infrastructure.Locator.repository
import il.co.superclick.notifications.OneSignalExtender
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.remote.Remote
import il.co.superclick.utilities.*
import kotlinx.android.synthetic.main.item_network.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class NetworkAdapter : ListAdapter<ShortShop, NetworkAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ShortShop>() {
        override fun areItemsTheSame(oldItem: ShortShop, newItem: ShortShop): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShortShop, newItem: ShortShop): Boolean {
            return oldItem == newItem
        }
    }
) {

    var isClickable = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_network, parent, false)
        )
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val networkImage: ImageView? get() = itemView.item_network_logo
        private val networkName: TextView? get() = itemView.item_network_name
        private val networkAddress: TextView? get() = itemView.item_network_address
        private val networkShopIndicator: ImageView? get() = itemView.item_network_shop_indicator
        private val networkDeliveryTime: TextView? get() = itemView.item_network_delivery_time


        @SuppressLint("ClickableViewAccessibility", "ObjectAnimatorBinding")
        fun bind(item: ShortShop) {
            networkImage?.glide(item.image)
            networkName?.text = item.name
            networkAddress?.text = item.address
            networkShopIndicator?.setImageResource(if (item.isMakingDelivery) R.drawable.indicator_green else R.drawable.indicator_red)
            item.deliveryTimeMinutesFrom?.let {
                item.deliveryTimeMinutesTo?.let { it1 ->
                    networkDeliveryTime?.text = getString(
                        R.string.from_to_mins,
                        it, it1
                    )
                } ?: run { networkDeliveryTime?.isGone = true }
            } ?: run { networkDeliveryTime?.isGone = true }
            itemView.onClick {
                if (isClickable) {
                    main {
                        itemView.animateClick()
                        WelcomeDialog.open(item) {
                            repository.clear()
                            isClickable = false
                            showProgressBar()
                            CoroutineScope(Dispatchers.IO).launch {
                                if (item.id != database.shopId) {
                                    database.cart.clear()
                                    database.coupon = null
                                }
                                Remote.setShop(shopId = item.id)?.also { Database.shop = it }
                                    ?.let { shop ->
                                        val productIds = database.cart.products.map { it.key }
                                        shop.categories.forEach {
                                            foregroundApplication.preloadImage(
                                                it.iconUrl
                                            )
                                        }
                                        val productsMap = getProducts(productIds)
                                        val cartProducts = productsMap?.get("cart")
                                        for (id in productIds) {
                                            if (cartProducts?.firstOrNull { it.id == id || !it.isOutOfStock } == null)
                                                database.cart.remove(id)
                                        }
                                        OneSignalExtender.sendOneSignalId()
                                        main {
                                            database.network?.tags?.forEach { it.isChecked = false }
                                            ProductListFragment.open(
                                                shop.categories.firstOrNull()?.name ?: ""
                                            )
                                            Cart.liveSum.set(Cart.products.filterValues { if (it.meals == null) it.isChecked else it.meals?.firstOrNull { meal -> meal.first } != null }.values.sumByDouble { it.sum })
                                        }

                                    } ?: kotlin.run {
                                    isClickable = true
                                    return@launch
                                }
                            }

                        }
                    }
                }
            }
        }

    }

    private suspend fun getProducts(cartProducts: List<Int>): Map<String, List<ShopProduct>>? =
        suspendCatch {
            repository.fetchAll(cartProducts)
        }


}
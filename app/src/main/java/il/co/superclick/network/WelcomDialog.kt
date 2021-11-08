package il.co.superclick.network

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.getString
import com.dm6801.framework.ui.onClick
import il.co.superclick.R
import il.co.superclick.data.ShortShop
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.utilities.glide
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.dialog_welcome.*

class WelcomeDialog : BaseDialog() {

    companion object : Comp<WelcomeDialog>() {

        private const val KEY_SHOP = "KEY_SHOP"
        private const val KEY_ADDRESS = "KEY_ADDRESS"
        private const val KEY_COMPLETE = "KEY_COMPLETE"

        fun open(shop: ShortShop, onComplete: () -> Unit) {
            open(KEY_SHOP to shop, KEY_COMPLETE to onComplete)
        }
    }

    override val layout: Int get() = R.layout.dialog_welcome
    override val closeWithActivity: Boolean get() = false
    override val isCancelable: Boolean get() = false

    private val welcomeName: TextView? get() = dialog_welcome_top_text
    private val welcomeImage: ImageView? get() = dialog_welcome_image
    private val welcomeAddress: TextView? get() = dialog_welcome_address
    private val confirm: TextView? get() = dialog_welcome_confirm_button
    private val back: ImageView? get() = dialog_welcome_close
    private val welcomeShopIndicator: ImageView? get() = welcome_shop_indicator
    private val welcomeDeliveryTime: TextView? get() = welcome_delivery_time

    private var shop: ShortShop? = null
    private var onConfirm: (() -> Unit)? = null

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        welcomeAddress?.text = shop?.address
        welcomeName?.text = getString(R.string.welcome_to_shop, shop?.name ?: "")
        welcomeImage?.glide(shop?.image)
        shop?.deliveryTimeMinutesFrom?.let {
            shop?.deliveryTimeMinutesTo?.let { it1 ->
                welcomeDeliveryTime?.text = getString(
                    R.string.from_to_mins,
                    it, it1
                )
            } ?: run { welcomeDeliveryTime?.isGone = true }
        } ?: run { welcomeDeliveryTime?.isGone = true }
        welcomeShopIndicator?.setImageResource(if (shop?.isMakingDelivery == true) R.drawable.indicator_green else R.drawable.indicator_red)
        back?.onClick { close() }
        confirm?.apply {
            confirm?.onClick {
                close()
                onConfirm?.invoke()
            }
            setTextColor(foregroundApplication.getColor(R.color.blueStartPage))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setStroke(1.dpToPx, foregroundApplication.getColor(R.color.blueStartPage))
                cornerRadius = 16.dpToPx.toFloat()
            }
        }

    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_SHOP] as? ShortShop)?.let { shop = it }
        (arguments[KEY_COMPLETE] as? (() -> Unit))?.let { onConfirm = it }
    }


}
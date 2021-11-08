package il.co.superclick.login

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import il.co.superclick.*
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.glide
import il.co.superclick.utilities.link
import il.co.superclick.utilities.preloadImage
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.ui.updateMargins
import com.dm6801.framework.utilities.background
import com.dm6801.framework.utilities.suspendCatch
import com.dm6801.framework.utilities.withMain
import il.co.superclick.cart.CartFragment
import kotlinx.android.synthetic.main.dialog_shop_confirm.*

class ConfirmShopDialog : BaseDialog() {

    companion object : Comp<ConfirmShopDialog>() {
        private val database get() = Locator.database

        //val user get() = database.user
        private val shop get() = database.shop
        private val notifications = Locator.notifications

        fun open(context: Context?) {
            if (isOpen) return
            context.preloadImage(shop?.image)
            open()
        }
    }

    override val layout = R.layout.dialog_shop_confirm
    override val widthFactor = 0.7f
    override val heightFactor = 0.46f
    private val imageView: ImageView? get() = shop_confirm_image
    private val descriptionTextView: TextView? get() = shop_confirm_description
    private val confirmButton: TextView? get() = shop_confirm_button
    private val cancelButton: TextView? get() = shop_confirm_cancel_button

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setWindow()
        loadDetails()
        initConfirm()
        initCancel()
    }

    private fun setWindow() {
        window?.attributes?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        val margin = 32.dpToPx
        view?.updateMargins(top = margin, bottom = margin)
    }

    private fun loadDetails() {
        shop?.image?.let {
            imageView?.glide(it) { centerCrop() }
        }
        shop?.description?.let { descriptionTextView?.text = it }
    }

    private fun initConfirm() {
        confirmButton?.onClick { confirmShop() }
    }

    private fun confirmShop() {
        if (shop != null) {
            close()
            CartFragment.open()
        } else {
            val shopId = database.shopId ?: return
            background {
                suspendCatch {
                    database.setShop(shopId)
                    if (database.isNotifications) notifications.sendOneSignalId()
                    loadDetails()
                    close()
                    withMain { CartFragment.open() }
                }
            }
        }
    }

    private fun initCancel() {
        cancelButton?.link("נסה שוב" to {
            close()
            database.deleteShopId()
        })
    }

}
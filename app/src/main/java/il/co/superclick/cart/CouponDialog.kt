package il.co.superclick.cart

import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.data.Cart.updateLiveData
import il.co.superclick.data.Coupon
import il.co.superclick.data.Database
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.language.Language
import il.co.superclick.language.LanguageManager
import il.co.superclick.remote.Remote
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.dialog_coupon_number.*

class CouponNumberDialog : BaseDialog() {

    override val layout: Int get() = R.layout.dialog_coupon_number
    override val isBackgroundDim: Boolean get() = true
    override val closeWithActivity: Boolean get() = false
    override val isCancelable: Boolean get() = false
    override val heightFactor: Float get() = 0.45f
    override val widthFactor: Float get() = if(LanguageManager.locale == Language.French.locale) 0.9f else 0.8f

    private val back: ImageView? get() = dialog_coupon_close
    private val couponErrorMessage: TextView? get() = dialog_coupon_error
    private val couponEditText: EditText? get() = coupon_field
    private val deny: TextView? get() = dialog_coupon_deny
    private val confirm: TextView? get() = dialog_coupon_confirm

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setListeners()
        confirm?.backgroundTintList = mainColor
        setWindow()
    }

    private fun setWindow() {
        window?.attributes?.gravity = Gravity.CENTER
        window?.setBackgroundDrawableResource(R.drawable.rect_white_corners_10)
    }

    private fun setListeners() {
        back?.onClick(2000) { dismiss() }
        deny?.onClick(2000) { dismiss() }
        confirm?.onClick(2000) {
            couponEditText?.text?.let { coupon ->
                Remote.checkCoupon(
                    coupon.toString(),
                    onSuccess = { discount ->
                        Database.coupon = Coupon(coupon = coupon.toString(), discount)
                        updateLiveData()
                        dismiss()
                        CouponConfirmed().show()
                    },
                    onError = { message ->
                        couponErrorMessage?.run {
                            isVisible = true
                            text = (foregroundActivity ?: foregroundApplication).getString(R.string.dialog_coupon_error)
                        }
                        couponEditText?.doOnTextChanged { _, _, _, _ ->
                            couponErrorMessage?.run {
                                isInvisible = true
                                text = (foregroundActivity ?: foregroundApplication).getString(R.string.dialog_coupon_error)
                            }
                        }
                    }
                )
            }
        }
    }

}
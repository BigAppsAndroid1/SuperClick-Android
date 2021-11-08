package il.co.superclick.login

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.text.underline
import androidx.core.widget.doOnTextChanged
import il.co.superclick.*
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.utilities.disable
import il.co.superclick.utilities.enable
import com.dm6801.framework.infrastructure.AbstractFragment
import com.dm6801.framework.ui.filterEmoticons
import com.dm6801.framework.ui.hideKeyboard
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.background
import com.dm6801.framework.utilities.suspendCatch
import com.dm6801.framework.utilities.toast
import com.dm6801.framework.utilities.withMain

class LoginFragment : AbstractFragment() {

    companion object : Comp() {
        private val database get() = Locator.database

        fun open() {
            if (foregroundFragment?.javaClass == clazz) return
            open()
        }
    }

    override val layout = -1
    private val codeEditText: EditText? get() = null
    private val submitButton: Button? get() = null
    private val smsButton: Button? get() = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCodeEdit()
        initPhoneButton()
    }

    private fun initCodeEdit() {
        codeEditText?.filterEmoticons()
        codeEditText?.doOnTextChanged { _, _, _, _ ->
            if (codeEditText?.text?.toString()?.isNotBlank() == true)
                submitButton?.enable()
            else
                submitButton?.disable()
        }
        submitButton?.disable()
        submitButton?.onClick(3_000) {
            codeEditText?.text?.toString()?.trim()?.let(::setShop)
        }
    }

    private fun setShop(shopCode: String) = background {
        suspendCatch {
            hideKeyboard()
            val shop = database.setShop(shopCode = shopCode)
            withMain {
                if (shop != null) ConfirmShopDialog.open(context)
                else com.dm6801.framework.ui.getString(R.string.shop_not_found_toast)?.let(::toast)
            }
        }
    }

    private fun initPhoneButton() {
        smsButton?.text = SpannableStringBuilder().underline { append(getString(R.string.login_sms_submit)) }
        smsButton?.onClick {
            SmsDialog.open {
                ConfirmShopDialog.open(context ?: return@open)
            }
        }
    }

}
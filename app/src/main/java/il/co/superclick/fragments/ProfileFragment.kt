package il.co.superclick.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.login.SmsDialog
import il.co.superclick.utilities.disable
import il.co.superclick.utilities.enable
import il.co.superclick.widgets.UserDetailsWidget
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.infrastructure.showProgressBar
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.background
import com.dm6801.framework.utilities.suspendCatch
import com.dm6801.framework.utilities.withMain
import il.co.superclick.utilities.mainColor
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : BaseFragment() {

    companion object : Comp() {
        private val database get() = Locator.database
    }

    override val layout = R.layout.fragment_profile
    override val themeBackground: Drawable? get() = getDrawable(R.drawable.bg_pay)
    private val userDetails: UserDetailsWidget? get() = profile_user_details
    private val submit: Button? get() = profile_submit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSubmit()
        setToolbar()
    }

    private fun setToolbar() {
        menuBar?.toggleCartButton()
    }

    private fun initSubmit() {
        submit?.backgroundTintList = mainColor
        userDetails?.setListener(onChanged = ::toggleSubmit)
        submit?.onClick { updateUserDetails() }
    }

    private fun toggleSubmit() {
        if (userDetails?.isFormValid() == true) submit?.enable()
        else submit?.disable()
    }

    private fun updateUserDetails() {
        if (userDetails?.isFormValid() != true) return
        val userDetails = userDetails?.getUserDetails() ?: return
        showProgressBar()
        background {
            suspendCatch {
                val response = database.upsertUser(userDetails)
                hideProgressBar()
                withMain {
                    if (response?.rawData != null) close()
                    else SmsDialog.open(SmsDialog.Step.Sms, userDetails.phone) { close() }
                }
            }
        }
    }

}
package il.co.superclick.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.dial
import il.co.superclick.utilities.link
import com.dm6801.framework.ui.getColor
import com.dm6801.framework.ui.onClick
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.setThemeColor
import kotlinx.android.synthetic.main.fragment_contact_shop.*

class ContactShopFragment : BaseFragment() {

    companion object : Comp() {
        private const val PHONE_RES = R.string.contact_shop_phone_label
        private val LINK_COLOR by lazy { getColor(R.color.colorPrimary) ?: Color.BLUE }
        private val shop get() = Locator.database.shop
    }

    override val layout = R.layout.fragment_contact_shop
    private val name: TextView? get() = contact_shop_name
    private val phone: TextView? get() = contact_shop_phone
    private val days: TextView? get() = contact_shop_work_days
    private val times: TextView? get() = contact_shop_work_times
    private val button: TextView? get() = contact_shop_button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        initDetails()
        setButton()
    }

    private fun setToolbar() {
        menuBar?.toggleCartButton()
        menuBar?.setFragmentTitle("יצירת קשר")
    }

    @SuppressLint("SetTextI18n", "StringFormatInvalid")
    private fun initDetails() {
        val shop = shop ?: return
        name?.setThemeColor()
        name?.text = "${shop.name}${shop.address?.let { ",\n$it" }}"
        shop.extraPhone?.let { phone ->
            this.phone?.text = foregroundActivity?.getString(PHONE_RES, phone)
            this.phone?.link(phone to { dial(phone); Unit }, color = LINK_COLOR)
        }
        days?.text = shop.workingTimes.joinToString("\n") { il.co.superclick.utilities.getString(R.string.day)+" "+it.dayLocalized() }
        times?.text = shop.workingTimes.joinToString("\n") { "${it.from} - ${it.to}" }
    }

    private fun setButton() {
        button?.setTextColor(mainColor)
        button?.onClick { close() }
    }

}
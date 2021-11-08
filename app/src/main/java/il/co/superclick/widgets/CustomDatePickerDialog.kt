package il.co.superclick.widgets

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.dm6801.framework.infrastructure.foregroundActivity
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.date.DayPickerView
import il.co.superclick.data.Shop
import java.util.*

class CustomDatePickerDialog : DatePickerDialog() {

    companion object {
        fun show(
            beforeShow: (CustomDatePickerDialog.() -> Unit)? = null,
            onDateSet: OnDateSetListener
        ): CustomDatePickerDialog? {
            return CustomDatePickerDialog().apply {
                accentColor = Shop.getShopColor()
                initialize(onDateSet, Calendar.getInstance())
                beforeShow?.invoke(this)
                show(
                    foregroundActivity?.supportFragmentManager ?: return null,
                    "DatePickerDialog"
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val nextMonthButton =
            view?.findViewById<View?>(com.wdullaer.materialdatetimepicker.R.id.mdtp_next_month_arrow)
                ?.apply { isInvisible = true }
        view?.findViewById<View?>(com.wdullaer.materialdatetimepicker.R.id.mdtp_previous_month_arrow)
            ?.isInvisible = true
        ((nextMonthButton?.parent as? ViewGroup)?.getChildAt(0) as? DayPickerView)
            ?.onPageListener = null
    }

}
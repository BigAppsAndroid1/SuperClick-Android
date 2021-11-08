package il.co.superclick.order

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatSpinner
import il.co.superclick.R
import il.co.superclick.data.OrderType
import il.co.superclick.data.Time
import il.co.superclick.utilities.getHebrewWeekDay
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.toDayHoursDate
import kotlinx.android.synthetic.main.view_days_of_week_picker.view.*
import java.util.*

class DaysOfWeekPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val DELIVERY_LABEL by lazy {
            getString(R.string.days_of_week_delivery_prompt, "") ?: ""
        }
        private val PICKUP_LABEL by lazy {
            getString(R.string.days_of_week_pickup_prompt, "") ?: ""
        }
    }

    val spinner: AppCompatSpinner? get() = days_of_week_text
    var type: OrderType? = null; private set

    val selected: Time?
        get() {
            val index = spinner?.selectedItemPosition ?: 0
            return if (index == 0) null
            else times.getOrNull(index - 1)
        }

    init {
        inflate(context, R.layout.view_days_of_week_picker, this)
        setBackgroundResource(R.drawable.rect_white_corners_8)
        elevation = 4f
    }

    private var times: List<Time> = emptyList()

    fun set(type: OrderType, times: List<Time>, onSelected: () -> Unit, date: Date? = null) {
        this.type = type
        this.times = times
        val label = when (type) {
            OrderType.Delivery -> DELIVERY_LABEL
            OrderType.Pickup -> PICKUP_LABEL
        }

        spinner?.prompt = label
        spinner?.adapter = ArrayAdapter(
            context,
            R.layout.item_days_of_week_picker,
            if(date != null && DateUtils.isToday(date.time)){
                listOf(label) + times.map { if (toDayHoursDate(it.from).before(Date()) && toDayHoursDate(it.to).after(Date())) "$label עכשיו" else "\u202D${it.from} - ${it.to}" + " :\u202B${it.date?.times(1000)?.getHebrewWeekDay()}"}
            } else {
                listOf(label) + times.map { "\u202D${it.from} - ${it.to}" + " :\u202B${it.date?.times(1000)?.getHebrewWeekDay()}"}
            }
        )
        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                onSelected()
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onSelected()
            }
        }
    }

    fun setFirstSelected(){
        try {
            val size = spinner?.adapter?.count ?: return
            if (size > 1)
                spinner?.setSelection(1)
            else
                spinner?.setSelection(0)

        }catch (t:Throwable){}
    }

}
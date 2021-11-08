package il.co.superclick.toppings

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.dm6801.framework.infrastructure.AbstractDialog
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.getColor
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.ui.onClick
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.toppings.Pizza.rotate
import il.co.superclick.utilities.getString
import il.co.superclick.utilities.mainColor
import il.co.superclick.utilities.toppingsLimitToast
import kotlinx.android.synthetic.main.dialog_choose_pizza_slices.*
import kotlin.properties.Delegates

internal typealias ConfirmLambda = (Array<Int>) -> Unit

class DialogChoosePizzaSlices : BaseDialog() {

    companion object : AbstractDialog.Comp<DialogChoosePizzaSlices>() {
        private const val KEY_ON_CONFIRM = "KEY_ON_CONFIRM"
        private const val KEY_MAX_TOPPINGS = "KEY_MAX_TOPPINGS"
        private const val KEY_NUMBER_PIECES = "KEY_NUMBER_PIECES"
        private val CORNER_RADIUS = 110.dpToPx.toFloat()
        private var idTopping by Delegates.notNull<Int>()
        private var indexPizza by Delegates.notNull<Int>()
        private var toppingCodeName by Delegates.notNull<String>()
        private var choosedSlices = arrayOf<Int>()

        fun open(
            maxToppings:Int,
            numberSelectedPieces:Int,
            toppingCodeName: String,
            indexPizza: Int,
            idTopp: Int,
            slices: Array<Int>,
            onConfirm: ConfirmLambda
        ) {
            Companion.toppingCodeName = toppingCodeName
            Companion.indexPizza = indexPizza
            choosedSlices = slices
            idTopping = idTopp
            DialogChoosePizzaSlices.open(
                KEY_ON_CONFIRM to onConfirm,
                KEY_MAX_TOPPINGS to maxToppings, KEY_NUMBER_PIECES to numberSelectedPieces)
        }
    }

    override val layout = R.layout.dialog_choose_pizza_slices
    override val gravity: Int = Gravity.CENTER
    override val widthFactor = 0.94f
    override val heightFactor = 0.86f
    private val confirm: TextView? get() = dialog_confirm_button
    private val cancel: TextView? get() = dialog_confirm_cancel
    private val chooseAllPizza: CheckBox? get() = btn_choose_all_pizza
    private val slice1: CheckBox? get() = chb_slice1
    private val slice2: CheckBox? get() = chb_slice2
    private val slice3: CheckBox? get() = chb_slice3
    private val slice4: CheckBox? get() = chb_slice4
    private val titleText: TextView? get() = pizza_title_dialog
    private val checkBoxes by lazy {
        listOf(slice1, slice2, slice3, slice4)
    }

    private var maxToppings = 0
    private var numberSelectedPieces = 0
    private var onConfirm: ((Array<Int>) -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_ON_CONFIRM] as? ConfirmLambda)?.let { onConfirm = it }
        (arguments[KEY_MAX_TOPPINGS ] as? Int)?.let { maxToppings = it }
        (arguments[KEY_NUMBER_PIECES] as? Int)?.let { numberSelectedPieces = it }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        confirm?.apply {
            onClick {
                onConfirm?.invoke(getCheckedSlices())
                dismiss()
            }
            setTextColor(mainColor)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setStroke(1.dpToPx, mainColor)
                cornerRadius = 16.dpToPx.toFloat()
            }
        }
        cancel?.apply {
            getColor(R.color.greyLight)?.let {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setStroke(1.dpToPx, it)
                    cornerRadius = 16.dpToPx.toFloat()
                }
                setTextColor(it)
            }
            onClick { cancel() }
        }
        titleText?.setTextColor(mainColor)
        setListeners()
        checkSlices()


    }

    private fun getCheckedSlices(): Array<Int> {
        val resalt = mutableListOf<Int>()
        for (i in 0..3) {
            if (checkBoxes[i]?.isChecked == true)
                resalt.add(i + 1)
        }
        return resalt.toTypedArray()
    }

    private fun checkSlices() {
        choosedSlices.forEach {
            numberSelectedPieces -= 1
            checkBoxes[it - 1]?.isChecked = true
        }
    }

    private fun setListeners() {
        slice1?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkMaxSlices(1)
            } else {
                numberSelectedPieces -= 1
                slice1?.background = null
            }
        }
        slice2?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkMaxSlices(2)
            } else {
                numberSelectedPieces -= 1
                slice2?.background = null
            }
        }
        slice3?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkMaxSlices(3)
            } else {
                numberSelectedPieces -= 1
                slice3?.background = null
            }

        }
        slice4?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkMaxSlices(4)
            } else {
                numberSelectedPieces -= 1
                slice4?.background = null
            }

        }
        chooseAllPizza?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxes.forEach { checkBox -> checkBox?.isChecked = true }
            } else {
                checkBoxes.forEach { checkBox -> checkBox?.isChecked = false }
            }
        }
    }

    private fun checkMaxSlices(slice:Int){
        numberSelectedPieces += 1
        if((numberSelectedPieces)<= maxToppings * 4 || maxToppings == 0 ) {
            drawTopping(slice)
        }else{
            numberSelectedPieces -= 1
            checkBoxes[slice-1]?.isChecked = false
            toppingsLimitToast(maxToppings)
        }

    }

    private fun drawTopping(slice: Int) {
        val checkBoxSlice = checkBoxes[slice - 1]
        val baseBitmap = Pizza.getBitmapByCodename(toppingCodeName)?.rotate(
            Pizza.getDegreeRotationImageTopping(
                slice
            )
        )
        baseBitmap?.let {
            checkBoxSlice?.background = BitmapDrawable(foregroundApplication.resources, it)
        } ?: run {
            checkBoxSlice?.background = getDrawable(R.drawable.topping_placeholder)?.toBitmap()?.rotate(
                Pizza.getDegreeRotationImageTopping(slice)
            )?.toDrawable(foregroundApplication.resources)
        }
    }

    override fun onCancel() {
        super.onCancel()
        dismiss()
    }
}







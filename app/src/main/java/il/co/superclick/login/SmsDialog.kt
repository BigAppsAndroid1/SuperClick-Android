package il.co.superclick.login

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.dm6801.framework.ui.getString
import il.co.superclick.infrastructure.Locator
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseDialog
import il.co.superclick.remote.Remote
import com.dm6801.framework.ui.showKeyboard
import com.dm6801.framework.utilities.*
import il.co.superclick.order.MapFragment
import il.co.superclick.utilities.onClick
import il.co.superclick.utilities.requestFocusCursorEnd
import il.co.superclick.utilities.setThemeColor
import il.co.superclick.utilities.valueOrEmpty
import kotlinx.android.synthetic.main.dialog_sms.*

class SmsDialog : BaseDialog() {

    companion object : Comp<SmsDialog>() {
        private const val KEY_STEP = "KEY_STEP"
        private const val KEY_PHONE = "KEY_PHONE"
        private const val KEY_ON_CONFIRM = "KEY_ON_CONFIRM"
        private val database = Locator.database
        private val notifications = Locator.notifications

        fun open(step: Step = Step.Phone, phone: String? = null, onConfirm: () -> Unit) {
            open(KEY_STEP to step, KEY_PHONE to phone, KEY_ON_CONFIRM to onConfirm)
        }
    }

    override val layout = R.layout.dialog_sms
    override val isBackgroundDim: Boolean get() = true
    override val closeWithActivity: Boolean get() = false
    override val isCancelable: Boolean get() = false
    private val title: TextView? get() = sms_title
    private val label: TextView? get() = sms_label
    private val edit: EditText? get() = sms_edit
    private val submit: TextView? get() = sms_submit
    private val cancel: TextView? get() = sms_cancel
    private val editText1: EditText? get() = sms_number_edit_1
    private val editText2: EditText? get() = sms_number_edit_2
    private val editText3: EditText? get() = sms_number_edit_3
    private val editText4: EditText? get() = sms_number_edit_4
    private val edits: List<EditText>
        get() = listOfNotNull(
            sms_number_edit_1,
            sms_number_edit_2,
            sms_number_edit_3,
            sms_number_edit_4
        )
    private var initialStep: Step? = null
    private var phone: String? = null
    private var onConfirm: (() -> Unit)? = null

    enum class Step(val title: Int, val label: Int, val editType: Int, val gravity: Int) {
        Phone(
            R.string.dialog_sms_phone_title,
            R.string.dialog_sms_phone_label,
            InputType.TYPE_CLASS_PHONE,
            Gravity.START or Gravity.CENTER_VERTICAL
        ),
        Sms(
            R.string.dialog_sms_verify_title,
            R.string.dialog_sms_verify_label,
            InputType.TYPE_CLASS_NUMBER,
            Gravity.CENTER
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_STEP] as? Step)?.let { initialStep = it }
        (arguments[KEY_PHONE] as? String)?.let { phone = it }
        (arguments[KEY_ON_CONFIRM] as? () -> Unit)?.let { onConfirm = it }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setWindow()
        initStep()
    }

    private fun setWindow() {
        window?.attributes?.gravity = Gravity.CENTER
        window?.setBackgroundDrawableResource(R.drawable.rect_white_corners_10)
    }

    private fun initStep() {
        step(initialStep ?: Step.Phone)
    }

    private fun cleanCode(){
        edits.forEach { it.text.clear() }
        edits.first().requestFocus()
    }

    private fun step(step: Step) {
        title?.setText(step.title)
        title?.setThemeColor()
        submit?.setThemeColor()
        cancel?.setThemeColor()
        label?.setText(step.label)
        edits.forEach { it.text.clear() }
        setPasswordListener()
        when (step) {
            Step.Phone -> {
                cancel?.onClick {
                    MapFragment.isCanAction = true
                    close() }
                submit?.onClick {
                    MapFragment.isCanAction = true
                    submitPhone() }
            }
            Step.Sms -> {
                cancel?.onClick {
                    when (initialStep) {
                        Step.Phone -> step(Step.Phone)
                        else -> {MapFragment.isCanAction = true;close(); dismiss()}
                    }
                }
                submit?.onClick(5_000) {submitSms() }
            }
        }
        editText1?.requestFocus()
        delay(100) { showKeyboard(editText1) }
    }

    private fun setPasswordListener() {
        editText1?.addTextChangedListener(EditableTextWatcher(editText1) { text ->
            setText(if (text.length > 1) text.take(1) else text)
            if (editText?.text?.length == 1) editText2?.requestFocusCursorEnd()
        })
        editText2?.addTextChangedListener(EditableTextWatcher(editText2) { text ->
            setText(if (text.length > 1) text.take(1) else text)
            if (editText?.text?.length == 1) editText3?.requestFocusCursorEnd()
        })
        editText3?.addTextChangedListener(EditableTextWatcher(editText3) { text ->
            setText(if (text.length > 1) text.take(1) else text)
            if (editText?.text?.length == 1) editText4?.requestFocusCursorEnd()
        })
        editText4?.addTextChangedListener(EditableTextWatcher(editText4) { text ->
            setText(if (text.length > 1) text.take(1) else text)
        })
        editText2?.apply {
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL && text.isNullOrBlank())
                    editText1?.requestFocusCursorEnd()
                false
            }
        }
        editText3?.apply {
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL && text.isNullOrBlank())
                    editText2?.requestFocusCursorEnd()
                false
            }
        }
        editText4?.apply {
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL && text.isNullOrBlank())
                    editText3?.requestFocusCursorEnd()
                false
            }
        }
    }

    private fun submitPhone() {
        edit?.text?.trim()
            ?.takeIf { it.matches(Patterns.PHONE.toRegex()) }?.toString()
            ?.let(::sendPhone)
    }

    private fun sendPhone(phone: String) = background {
        suspendCatch {
            val isExist = Remote.verifyPhone(phone)
            withMain {
                if (isExist) {
                    this@SmsDialog.phone = phone
                    step(Step.Sms)
                } else {
                    getString(R.string.user_not_found_toast)?.let(::toast)
                }
            }
        }
    }

    private fun submitSms() {
        val phone = phone ?: return
        getPassword().takeIf { it.length == 4 }
            ?.let {
                authorizePhone(phone, it)
            }
    }

    private fun getPassword(): String {
        return edits.joinToString("") { it.valueOrEmpty }
    }

    private fun authorizePhone(phone: String, sms: String) = background {
        suspendCatch {
            val user = database.authorizePhone(phone, sms)
            if (user != null) {
//                database.setShop(user.shopId)
                if (database.isNotifications) notifications.sendOneSignalId()
                withMain {
                    close()
                    onConfirm?.invoke()
                }
            } else withMain { getString(R.string.sms_invalid_toast)?.let(::toast); this@SmsDialog.cleanCode() }
        } ?: withMain { getString(R.string.sms_invalid_toast)?.let(::toast); this@SmsDialog.cleanCode() }
    }

    class EditableTextWatcher(
        editText: EditText?,
        val onTextChanged: EditableTextWatcher.(text: String) -> Unit
    ) : TextWatcher {
        var editText: EditText? by weakRef(editText)
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged(s?.toString() ?: "")
        }

        fun setText(text: String) {
            editText?.apply {
                removeTextChangedListener(this@EditableTextWatcher)
                setText(text)
                addTextChangedListener(this@EditableTextWatcher)
            }
        }
    }

}
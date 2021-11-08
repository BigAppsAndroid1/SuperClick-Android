package il.co.superclick.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.CompoundButtonCompat
import il.co.superclick.R
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.value
import il.co.superclick.utilities.watch
import com.dm6801.framework.ui.*
import il.co.superclick.data.User
import kotlinx.android.synthetic.main.widget_user_details.view.*

class UserDetailsWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val PADDING by lazy { 4.dpToPx }
        private const val LAYOUT = R.layout.widget_user_details
        private const val VALID_BACKGROUND = R.drawable.edit_text_shadow_
        private const val INVALID_BACKGROUND = R.drawable.edit_text_shadow_invalid
        private val VALID_TEXT by lazy { getColor(R.color.text_color) ?: Color.BLACK }
        private val INVALID_TEXT by lazy { getColor(R.color.text_color_invalid) ?: Color.RED }
        private val database get() = Locator.database
        private val user get() = database.user
    }

    private val name: EditText? get() = user_details_name
    private val phone: EditText? get() = user_details_phone
    private val email: EditText? get() = user_details_email
    private val detailsTitle: TextView? get() = user_details_label
    private val street: EditText? get() = user_details_street
    private val streetNumber: EditText? get() = user_details_street_number
    private val entranceCode: EditText? get() = user_details_entrance_code
    private val apartment: EditText? get() = user_details_apartment
    private val floor: EditText? get() = user_details_floor
    private val city: EditText? get() = user_details_city
    private var wasFormEdit: Boolean = false
    private var onChanged: (() -> Unit)? = null
    private var onDone: (() -> Unit)? = null

    init {
        inflate(context, LAYOUT, this)
        if (!isInEditMode) updatePadding(PADDING)
        clickable()
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setLayout()
    }

    private fun setLayout() {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        loadUserDetails()
    }

    fun setListener(onDone: (() -> Unit)? = null, onChanged: () -> Unit) {
        this.onDone = onDone
        this.onChanged = onChanged
    }

    private fun loadUserDetails() {
        name.init(user?.name, isRequired = true) { it.toString().trim().isNotEmpty() }
        phone.init(user?.phone, isRequired = true) { value -> value.toString().run { matches(Patterns.PHONE.toRegex()) && length == 10 } }
        email.init(user?.email, isRequired = true) { value -> value.toString().run { matches(Patterns.EMAIL_ADDRESS.toRegex()) } }
        setIsAddressRequired(false)
    }

    fun getUserDetails(): User? {
        return (user ?: User.Empty()).copy(
            name = name?.value ?: return null,
            phone = phone?.value ?: return null,
            email = email?.value ?: return null,
            streetName = street?.value ?: return null,
            streetNumber = streetNumber?.value ?: return null,
            entranceCode = entranceCode?.value,
            apartmentNumber = apartment?.value?.toIntOrNull(),
            floorNumber = floor?.value,
            city = city?.value ?: return null,
        )
    }

    fun setIsAddressRequired(isRequired: Boolean){
        street.init(user?.streetName, isRequired = isRequired) { it.toString().trim().isNotEmpty() }
        streetNumber.init(user?.streetNumber, isRequired = isRequired) { it.toString().trim().isNotEmpty() }
        entranceCode.init(user?.entranceCode, isRequired = false)
        apartment.init(user?.apartmentNumber?.toString(), isRequired = false)
        floor.init(user?.floorNumber, isRequired = false)
        city.init(user?.city, isRequired = isRequired) { it.toString().trim().isNotEmpty() }
        city?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                requestFocus()
                onDone?.invoke()
            }
            false
        }
    }

    fun hideAddressDetails() {
        detailsTitle?.isVisible = false
        street?.isVisible = false
        streetNumber?.isVisible = false
        entranceCode?.isVisible = false
        floor?.isVisible = false
        apartment?.isVisible = false
        city?.isVisible = false
    }

    fun isFormValid(): Boolean {
        if (name?.text?.trim()?.isEmpty() == true) return false
        if (email?.text?.matches(Patterns.EMAIL_ADDRESS.toRegex()) == false)
            return false
        children.forEach {
            val isRequired = it.tag
            if (it is EditText && it.text.isNullOrBlank() && isRequired == true)
                return false
        }
        return true
    }

    private fun <T : View> T?.init(
        value: Any?,
        isRequired: Boolean = true,
        predicate: (T.(Any?) -> Boolean)? = null
    ) {
        when (this) {
            is EditText -> {
                setText(value)
                filterEmoticons()
            }
            is CheckBox -> isChecked = value as? Boolean ?: false
        }
        watch(isRequired, predicate)
    }

    private fun <T : View> T?.watch(
        isRequired: Boolean = true,
        predicate: (T.(Any?) -> Boolean)? = null
    ) {
        val _predicate = predicate
            ?: if (isRequired) { _: T, _: Any? -> toString().trim().isNotEmpty() }
            else { _: T, _: Any? -> true }

        val onChanged = { _: T, _: Any? ->
            wasFormEdit = true
            onChanged?.invoke()
            Unit
        }

        this?.tag = isRequired

        val onValid =
            when (this) {
                is EditText -> { _: T ->
                    setBackgroundResource(VALID_BACKGROUND)
                    setTextColor(VALID_TEXT)
                }
                is CheckBox -> { _: T ->
                    CompoundButtonCompat.setButtonTintList(this, null)
                }
                else -> { _: T ->
                }
            }

        val onInvalid =
            when (this) {
                is EditText -> { _: T ->
                    setBackgroundResource(INVALID_BACKGROUND)
                    setTextColor(VALID_TEXT)
                }
                is CheckBox -> { _: T ->
                    CompoundButtonCompat.setButtonTintList(
                        this,
                        ColorStateList.valueOf(INVALID_TEXT)
                    )
                }
                else -> { _: T -> }
            }

        watch(onChanged, onValid, if(isRequired) onInvalid else onValid, _predicate)
    }

}
package il.co.superclick.widgets

import android.content.Context
import android.content.res.TypedArray
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.res.use
import il.co.superclick.R
import kotlinx.android.synthetic.main.widget_search.view.*

class SearchWidget @JvmOverloads constructor(
        context: Context,
        private val attrs: AttributeSet? = null,
        private val defStyleAttr: Int = 0,
        private val defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    val editText: EditText? get() = search_widget_field
    var text: CharSequence?
        get() = editText?.text
        set(value) {
            editText?.setText(value)
        }
    private val clearButton: ImageView? get() = search_widget_clear_button

    init {
        inflate(this.context, R.layout.widget_search, this)
        obtainStyledAttributes(context)
        initClearButton()
    }

    private fun obtainStyledAttributes(context: Context?) {
        context?.theme?.obtainStyledAttributes(attrs, R.styleable.SearchWidget, defStyleAttr, defStyleRes)
                ?.use { typedArray -> onStyledAttributes(typedArray) }
    }

    private fun onStyledAttributes(typedArray: TypedArray) {
        typedArray.getText(R.styleable.SearchWidget_android_hint)?.let { hint ->
            editText?.hint = hint
        }
    }

    private fun initClearButton() {
        clearButton?.setOnClickListener {
            editText?.text?.clear()
        }
    }

    fun onTextChanged(block: (Editable) -> Unit) {
        editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let(block)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }


}
package il.co.superclick.utilities

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.dm6801.framework.utilities.weakRef

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
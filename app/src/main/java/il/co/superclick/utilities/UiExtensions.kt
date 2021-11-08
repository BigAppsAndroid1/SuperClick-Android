package il.co.superclick.utilities

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.runOnUiThread
import il.co.superclick.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val resources: Resources
    get() = foregroundActivity?.resources ?: foregroundApplication.resources


fun View.disable(alphaVal: Float = 0.5f) {
    isEnabled = false
    alpha = alphaVal
}

fun View.enable() {
    alpha = 1f
    isEnabled = true
}

@Suppress("UsePropertyAccessSyntax")
val TextView.value: String?
    get() = text?.toString()

fun TextView.setThemeColor() {
    setTextColor(mainColor)
}

fun getString(@StringRes res: Int, vararg args: Any): String =
    catch {
        resources.getString(res, *args)
    } ?: ""

private val clickAnimationBackground by lazy {
    ContextCompat.getDrawable(
        foregroundApplication,
        R.drawable.rect_gray_corners_20
    )
}

fun View.animateClick(duration: Long = 400, onAnimationComplete: (() -> Unit)? = null) {
    runOnUiThread {
        (this as? ViewGroup)?.run {
            val hoverView = View(this.context)
            hoverView.layoutParams = ViewGroup.LayoutParams(this.width, this.height)
            hoverView.scaleY = 0f
            hoverView.scaleX = 0f
            hoverView.background = clickAnimationBackground
            addView(hoverView)
            ValueAnimator.ofFloat(0f, 0.98f).apply {
                addUpdateListener {
                    val value = animatedValue as Float
                    hoverView.scaleX = value
                    hoverView.scaleY = value
                }
                setDuration(duration)
                start()
            }

            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(duration)
                removeView(hoverView)
                onAnimationComplete?.invoke()
            }
        }
    }
}

fun ImageView.glide(
    source: Any?,
    context: Context = foregroundActivity?.baseContext ?: foregroundApplication.baseContext,
    isPlaceHolder: Boolean? = false,
    transform: (RequestBuilder<Drawable>.() -> RequestBuilder<Drawable>)? = null
) {
    if (source == null) {
        setImageDrawable(null)
        return
    }
    try {
        Glide.with(context).load(source).apply {
            if (isPlaceHolder == true) {
                placeholder(R.drawable.no_image)
            }
            timeout(60000)
            transform?.invoke(this)
        }.into(this)
    } catch (_: Exception) {
    }
}

fun Context?.preloadImage(source: Any?, size: Pair<Int, Int>? = null) = this?.apply {
    try {
        if (size != null)
            Glide.with(this).load(source).timeout(60000).preload(size.first, size.second)
        else
            Glide.with(this).load(source).timeout(60000).preload()
    } catch (t: Throwable) {
        t.printStackTrace()
    }
} ?: Unit

fun <T : View> T.tag(obj: Any?): T {
    tag = obj
    return this
}

fun View.getLocationOnScreen(): Pair<Int, Int> {
    val loc = IntArray(2)
    getLocationOnScreen(loc)
    return loc[0] to loc[1]
}

fun View.getLocationInWindow(): Pair<Int, Int> {
    val loc = IntArray(2)
    getLocationInWindow(loc)
    return loc[0] to loc[1]
}

fun TextView.link(
    vararg actions: Pair<String, (() -> Unit)?>,
    color: Int? = null
): TextView? {
    return try {
        val _color = color ?: Color.BLUE
        val completeText = this.text?.toString() ?: return this
        val spannableString = SpannableString(completeText)
        actions.forEach { (text, action) ->
            try {
                val startPosition = completeText.indexOf(text)
                if (startPosition == -1) return this
                val endPosition = startPosition + text.length
                val clickableSpan = object : ClickableSpan() {
                    private val coolDown = 2_000L
                    private var lastClickTime = 0L

                    override fun onClick(widget: View) {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime > coolDown) {
                            lastClickTime = now
                            action?.invoke()
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.bgColor = Color.TRANSPARENT
                        ds.linkColor = _color
                        ds.color = _color
                        ds.isUnderlineText = true
                    }
                }
                spannableString.setSpan(
                    clickableSpan, startPosition, endPosition,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            } catch (_: Exception) {
            }
        }
        this.highlightColor = Color.TRANSPARENT
        this.text = spannableString
        this.movementMethod = LinkMovementMethod.getInstance()
        this
    } catch (e: Exception) {
        e.printStackTrace()
        this
    }
}

fun String.underlineText(): SpannableString {
    val spannableString = SpannableString(this)
    spannableString.setSpan(
        UnderlineSpan(),
        0,
        this.length,
        SpannableString.SPAN_EXCLUSIVE_INCLUSIVE
    )
    return spannableString
}
fun String.underline(): SpannableString {
    val spannableString = SpannableString(this)
    val startPosition = this.indexOf(this)
    if (startPosition == -1) return spannableString
    val endPosition = startPosition + this.length
    spannableString.setSpan(
        UnderlineSpan(),
        startPosition,
        endPosition,
        SpannableString.SPAN_EXCLUSIVE_INCLUSIVE
    )
    return spannableString
}

val EditText?.valueOrEmpty: String get() = this?.text?.toString() ?: ""

fun EditText.requestFocusCursorEnd() =
    requestFocusWithPosition(text?.length ?: 0)

fun EditText.requestFocusWithPosition(position: Int) {
    requestFocus()
    setSelection(position)
}

fun <T : View> T?.watch(
    onChanged: T.(Any?) -> Unit,
    onValid: T.() -> Unit,
    onInvalid: T.() -> Unit,
    predicate: T.(Any?) -> Boolean
) = catch {
    when (this) {
        is EditText -> {
            val action = { text: String ->
                if (predicate(text)) onValid()
                else onInvalid()
                onChanged(text)
            }
            doOnTextChanged { text, _, _, _ -> action(text?.toString() ?: "") }
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) action(text?.toString() ?: "")
            }
        }
        is CheckBox -> {
            val action = {
                if (predicate(isChecked)) onValid()
                else onInvalid()
                onChanged(isChecked)
            }
            setOnCheckedChangeListener { _, _ -> action() }
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) action() }
        }
        is AppCompatSpinner -> {
            val action = { position: Int? ->
                if (predicate(position)) onValid()
                else onInvalid()
                onChanged(position)
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    action(null)
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    action(position)
                }
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) action(selectedItemPosition)
            }
        }
        else -> {
        }
    }
}

// region maxToppings toast
private const val MAX_TOPPINGS_DELAY = 1000
private var lastTimeToastAppear:Long = 0
fun toppingsLimitToast(limit: Int?){
    if (System.currentTimeMillis() - lastTimeToastAppear > MAX_TOPPINGS_DELAY) {
        lastTimeToastAppear = System.currentTimeMillis()
        Toast.makeText(
            foregroundActivity,
            getString(
                R.string.max_slices_alert,
                limit ?: 0
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}
//endregion
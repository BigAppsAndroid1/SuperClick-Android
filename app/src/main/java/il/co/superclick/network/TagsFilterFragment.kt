package il.co.superclick.network

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.AbstractFragment
import com.dm6801.framework.utilities.catch
import com.dm6801.framework.utilities.delay
import il.co.superclick.MainActivity
import il.co.superclick.R
import il.co.superclick.data.Tag
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.fragment_tags_filter.*

class TagsFilterFragment : AbstractFragment() {

    companion object : Comp() {
        private const val ON_CONFIRM = "ON_CONFIRM"

        private val tags get() = Locator.database.network?.tags

        fun open(onConfirm: () -> Unit) = catch {
            if (foregroundFragment?.javaClass == clazz) return@catch
            open(ON_CONFIRM to onConfirm, replace = false)
        }
    }

    override val activity: MainActivity? get() = super.activity as? MainActivity
    override val layout = R.layout.fragment_tags_filter
    private val container: View? get() = tag_filter_container
    private val tagsList: RecyclerView? get() = tags_list
    private val confirm: TextView? get() = confirm_filter_choice
    private val tagsAdapter: TagsAdapter = TagsAdapter()
    private val selectedTags: MutableList<Tag> = mutableListOf<Tag>().apply { tags?.map { it.copy().apply { isChecked = it.isChecked } }?.toMutableList()?.let { addAll(it) } }
    private var onConfirm: (()->Unit)? = null

    @Suppress("UNCHECKED_CAST")
    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[ON_CONFIRM] as? (() -> Unit))?.let{ onConfirm = it}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnClickListener { close() }
        confirm?.onClick {
            selectedTags.forEach { selected -> tags?.firstOrNull { selected.id == it.id }?.isChecked = selected.isChecked }
            onConfirm?.invoke()
            close()
        }
        setTagsRecycler()
    }

    private fun setTagsRecycler() {
        tagsList?.adapter = tagsAdapter
        tagsAdapter.submitList(selectedTags)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val anim: Animation? = if (enter) {
            AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
                .apply {
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {}

                        override fun onAnimationStart(animation: Animation?) {
                            animation ?: return
                            delay(100) { container?.alpha = 1f }
                        }

                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                }
        } else {
            AnimationUtils.loadAnimation(
                context,
                R.anim.slide_out_right
            )
        }
        return AnimationSet(true).apply { addAnimation(anim) }
    }

    private fun close(anim: Boolean = true, doAfter: (() -> Unit)? = null) {
        if (anim) {
            delay(200) {
                Companion.close()
                doAfter?.let {
                    kotlinx.coroutines.delay(670)
                    it.invoke()
                }
            }
        } else {
            Companion.close()
            doAfter?.let {
                delay(400) { it.invoke() }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        close()
        return true
    }

}
package il.co.superclick.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import il.co.superclick.infrastructure.Locator
import il.co.superclick.utilities.getString
import kotlinx.android.synthetic.main.fragment_info.*

class InfoFragment : BaseFragment() {

    companion object : Comp() {
        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_CONTENT = "KEY_CONTENT"
        private val shop get() = Locator.database.shop

        fun open(type: Type) {
            val shop = shop ?: return
            when (type) {
                Type.AboutShop ->
                    open(
                        title = getString(R.string.menu_about_shop, ""),
                        content = shop.about ?: ""
                    )
                else ->
                    open(
                        title = getString(type.title),
                        content = shop.info[type.key] ?: ""
                    )
            }
        }

        fun open(title: String, content: String) {
            open(KEY_TITLE to title, KEY_CONTENT to content)
        }
    }

    enum class Type(val key: String, val title: Int) {
        AboutShop("", -1),
        AboutApp("about", R.string.about_title),
        Regulations("rules", R.string.regulations_title),
        Returns(
            "return_policy",
            R.string.returns_policy_title
        ),
        Privacy(
            "privacy_policy",
            R.string.privacy_policy_title
        )
    }

    override val layout = R.layout.fragment_info
    private val contentText: TextView? get() = info_content

    private var title: String? = null
    private var content: String? = null

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_TITLE] as? String)?.let { title = it }
        (arguments[KEY_CONTENT] as? String)?.let { content = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentText?.text = content
        menuBar?.toggleCartButton()
        menuBar?.setFragmentTitle(title ?: return)
    }

}
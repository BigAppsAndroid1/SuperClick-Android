package il.co.superclick.history

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.infrastructure.BaseFragment
import kotlinx.android.synthetic.main.fragment_receipt.*

class ReceiptWebView : BaseFragment(){

    companion object: Comp(){
        private const val KEY_LINK = "KEY_LINK"
        fun open(link: String){
            open(KEY_LINK to link)
        }
    }

    override val layout: Int get() = R.layout.fragment_receipt
    private val backButton: ImageView? get() = back
    private val webView: WebView? get() = web_view
    private var billLink: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backButton?.onClick {
            webView?.clearCache(true)
            close()
        }
        main{
            webView?.apply {
                webViewClient = object: WebViewClient() {

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return true
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        if (view.title.equals(""))
                            view.reload()
                        else
                            hideProgressBar()
                    }
                }
                settings.pluginState = WebSettings.PluginState.ON
                settings.setSupportZoom(true)
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                billLink?.let { loadUrl(it) }
            }
        }
    }

    override fun onArguments(arguments: Map<String, Any?>) {
        super.onArguments(arguments)
        (arguments[KEY_LINK] as? String)?.let{ billLink = it}
    }

}
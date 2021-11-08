package il.co.superclick.infrastructure


import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import com.dm6801.framework.infrastructure.AbstractDialog
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R


abstract class BaseDialog : AbstractDialog() {

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view.updateLayoutParams<FrameLayout.LayoutParams> {
            this.gravity = this@BaseDialog.gravity
        }
        (window?.decorView as ViewGroup)
            .getChildAt(0).startAnimation(
                AnimationUtils.loadAnimation(
                    context, R.anim.slide_in_bottom
                )
            )

        if (widthFactor == null || heightFactor == null)
            window?.decorView?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDismiss() {
        super.onDismiss()
        (window?.decorView as ViewGroup)
            .getChildAt(0).startAnimation(
                AnimationUtils.loadAnimation(
                    context, R.anim.slide_out_bottom
                )
            )
    }

}
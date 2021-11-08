package il.co.superclick.utilities

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.dm6801.framework.infrastructure.hideProgressBar
import com.dm6801.framework.ui.onClick
import com.stfalcon.imageviewer.StfalconImageViewer
import il.co.superclick.R

fun showMagnifiedImage(image: ImageView, path: String) {
    val imageUri = path.toUri()
    val overlayView = createOverlayView(image) ?: return

    val viewer =
        StfalconImageViewer.Builder(image.context, listOf(imageUri)) { loaderImageView, uri ->
            Glide.with(loaderImageView).load(uri).into(loaderImageView)
            loaderImageView.setPadding(0, 0, 0, 0)
            loaderImageView.cropToPadding = true
        }
            .allowSwipeToDismiss(false)
            .withHiddenStatusBar(false)
            .withBackgroundColor(Color.WHITE)
            .withOverlayView(overlayView)
            .show()
    hideProgressBar()

    overlayView.findViewById<ImageView?>(R.id.dialog_image_close)
        ?.apply {
            onClick { viewer.dismiss() }
        }
}

@SuppressLint("InflateParams")
private fun createOverlayView(image: ImageView): View? {
    return image.context?.let {
        LayoutInflater.from(it).inflate(R.layout.image_zoom_overlay, null)
    }
}
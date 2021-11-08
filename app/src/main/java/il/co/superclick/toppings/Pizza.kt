package il.co.superclick.toppings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.core.graphics.drawable.toBitmap
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.getDrawable
import com.dm6801.framework.utilities.catch

object Pizza {

    fun getBitmapByCodename(codename: String?): Bitmap? = catch {
        getDrawable(
            foregroundApplication.resources.getIdentifier(
                codename,
                "drawable",
                foregroundApplication.packageName
            )
        )?.toBitmap()
    }

    fun getDegreeRotationImageTopping(slice: Int): Float {
        when (slice) {
            1 -> return 270f
            2 -> return 180f
            4 -> return 90f
        }
        return 0f
    }

    fun overlayBitmapToppings(bmp1: Bitmap, bmp2: Bitmap): Bitmap? {
        val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(bmp1, Matrix(), null)
        canvas.drawBitmap(bmp2, 0f, 0f, null)
        return bmOverlay
    }

    fun Bitmap.rotate(degrees: Float = 180F): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
    }

}
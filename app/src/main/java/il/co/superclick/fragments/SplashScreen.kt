package il.co.superclick.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.dm6801.framework.utilities.delay
import com.dm6801.framework.utilities.main
import il.co.superclick.MainActivity
import il.co.superclick.R


class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val videoHolder = FullScreenVideoView(this)
            setContentView(videoHolder)
            val video = Uri.parse("android.resource://" + packageName + "/" + R.raw.splash_video)
            videoHolder.setVideoURI(video)
            videoHolder.start()
            videoHolder.setOnCompletionListener { jump() }
            main{
               delay(8000){
                   videoHolder.stopPlayback()
                   jump()
               }
            }
        } catch (ex: Exception) {
            jump()
        }
    }

    private fun jump() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

class FullScreenVideoView : VideoView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val metrics = context.resources.displayMetrics
        (context as Activity).windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenHeight = metrics.heightPixels
        val screenWidth = screenHeight * 9 / 16
        setMeasuredDimension(screenWidth, screenHeight)
    }
}
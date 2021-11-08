package il.co.superclick.utilities

import android.media.MediaPlayer
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import il.co.superclick.data.Shop
import kotlinx.coroutines.delay

fun playSound(res: Int, delay: Long? = null) =
    lifecycleScope { delay?.let { delay(it) }; SoundPlayer.play(res) }

fun playCartSound(delay: Long? = null) =
    lifecycleScope { delay?.let { delay(it) }; playSound(R.raw.cart) }

fun playToppingsSound(delay: Long? = null) =
    lifecycleScope { delay?.let { delay(it) }; playSound(R.raw.toppings) }

fun playTickSound(delay: Long? = null) =
    lifecycleScope { delay?.let { delay(it) }; playSound(R.raw.tick) }


object SoundPlayer {

    fun play(res: Int) {
        if (Shop.isShopWithSound == true) {
            try {
                MediaPlayer.create(foregroundApplication, res).run {
                    setVolume(1f, 1f)
                    setOnCompletionListener {
                        release()
                    }
                    setOnErrorListener { _, _, _ ->
                        release()
                        false
                    }
                    setOnPreparedListener {
                        start()
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

}
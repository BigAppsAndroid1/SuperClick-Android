package il.co.superclick.utilities

import android.os.Looper
import androidx.lifecycle.*
import com.dm6801.framework.utilities.catch

fun <T> LiveData<T>.set(value: T): Boolean {
    if (this !is MutableLiveData) return false
    return try {
        if (Thread.currentThread() == Looper.getMainLooper().thread) setValue(value)
        else postValue(value)
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }
}

interface LiveDataSubscriber<T> {
    val lifecycleOwner: LifecycleOwner
    fun subscribe(liveData: LiveData<T>, action: (T) -> Unit) {
        liveData.observe(lifecycleOwner, Observer(action))
    }

    fun unsubscribe(liveData: LiveData<T>) = catch {
        liveData.removeObservers(lifecycleOwner)
    } ?: Unit
}
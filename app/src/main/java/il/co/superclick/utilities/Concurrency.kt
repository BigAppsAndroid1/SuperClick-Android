package il.co.superclick.utilities

import androidx.lifecycle.lifecycleScope
import com.dm6801.framework.infrastructure.foregroundActivity
import com.dm6801.framework.infrastructure.foregroundFragment
import com.dm6801.framework.utilities.coRoutineExceptionHandler
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@Suppress("LocalVariableName")
fun lifecycleScope(
    context: CoroutineContext? = null,
    work: suspend CoroutineScope.() -> Unit
): Job {
    val scope = foregroundFragment?.lifecycleScope
        ?: foregroundActivity?.lifecycleScope
        ?: CoroutineScope(context ?: Dispatchers.Main)

    return scope.launch(
        context = (context ?: scope.coroutineContext) + coRoutineExceptionHandler,
        block = work
    )
}

fun delay(
    ms: Long,
    dispatcher: CoroutineContext = Dispatchers.Main,
    block: suspend CoroutineScope.() -> Unit
): Deferred<Any> =
    CoroutineScope(dispatcher).async {
        delay(ms)
        block()
    }
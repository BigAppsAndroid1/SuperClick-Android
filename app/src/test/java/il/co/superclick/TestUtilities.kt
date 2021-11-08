package il.co.superclick

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

fun <T> runTest(block: suspend CoroutineScope.() -> T?) {
    runBlocking {
        val result = block()
        if (result is Job) result.join()
    }
}

fun Any.getResource(name: String): String {
    return try {
        this::class.java.classLoader?.getResource(name)?.readText()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } ?: ""
}
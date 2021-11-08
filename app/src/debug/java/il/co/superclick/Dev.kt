package il.co.superclick

@Suppress("UNUSED_PARAMETER", "unused")
object Dev {

    val isDev: Boolean
        get() = BuildConfig.DEBUG && BuildConfig.BUILD_TYPE != "release"

    operator fun invoke(action: Dev.() -> Unit): Boolean = isDev

    fun init() {}

    fun loadShop() {}

    fun loadUser() {}

}
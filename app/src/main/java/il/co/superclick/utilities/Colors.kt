package il.co.superclick.utilities

import android.content.res.ColorStateList
import com.dm6801.framework.ui.getColor
import il.co.superclick.R
import il.co.superclick.data.Shop

val newColor by lazy {
    ColorStateList.valueOf(getColor(R.color.orange) ?: Shop.getShopColor())
}
val saleColor by lazy {
    ColorStateList.valueOf(getColor(R.color.red) ?: Shop.getShopColor())
}
val mainColor get() = ColorStateList.valueOf(Shop.getShopColor())


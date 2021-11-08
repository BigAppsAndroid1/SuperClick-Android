package il.co.superclick.product_list

import android.app.ActionBar
import android.text.Layout
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import androidx.recyclerview.widget.SimpleItemAnimator
import com.dm6801.framework.ui.dpToPx
import com.dm6801.framework.ui.updatePadding
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.data.Level
import il.co.superclick.data.ShopProduct
import il.co.superclick.infrastructure.LayoutManager
import il.co.superclick.infrastructure.RecyclerAdapter
import il.co.superclick.utilities.getLocationOnScreen
import il.co.superclick.utilities.playTickSound

class ProductHorizontalAdapter(
    val mealCallback: ((
        product: ShopProduct,
        amount: Float?,
        toppings: MutableList<MutableList<Pair<Int, List<Int>>>>?,
        options: MutableList<MutableList<Pair<Int, List<Int>>>>?
    )->Unit)? = null,
    var level: Level? = null,
    val preloadThreshold: Int = 10,
    val preload: ((end: Boolean) -> Unit)? = null
) : RecyclerAdapter<ProductViewHolder, ProductAdapterItem>(), ProductsAdapter {

    override val layout = R.layout.item_product3
    override val adapterClasses = listOf(ProductsAdapter::class.java)
    override val viewHolderClass = ProductViewHolder::class.java
    override val layoutManager: LayoutManager? get() = recyclerView?.layoutManager as? LayoutManager
    override var focusedProductId: Int? = null
    override var focusedPosition: Int? = null
    override var canMealProductButtonAction: Boolean? = true

    init {
        setHasStableIds(true)
    }

    override fun submitList(list: List<ShopProduct>) {
        submitList(list.map { ProductAdapterItem(it) })
    }

    @Suppress("UNUSED_PARAMETER")
    fun submitList(list: List<ShopProduct>, a: Unit = Unit, callback: () -> Unit) {
        submitList(list.map { ProductAdapterItem(it) }) {
            setInitialMargin()
            callback.invoke()
        }
    }


    override fun internalOnBindViewHolder(
        holder: ProductViewHolder,
        position: Int,
        payloads: MutableList<Any>?
    ) {
        holder.mealCallback = mealCallback
        holder.level = level
        super.internalOnBindViewHolder(holder, position, payloads)
        val countFromEnd = ((currentList ?: emptyList()).size - position).coerceAtLeast(0)
        if (countFromEnd <= preloadThreshold && (currentList?.size ?:0 ) >= 20) preload?.invoke(false)
    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.layoutManager = LayoutManager(recyclerView.context, 1)
        SnapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var previousPos = 0
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val view = recyclerView.findChildViewUnder(dx.toFloat(), dy.toFloat()) ?: run { return }
                layoutManager?.visibleItemPositions?.forEach { pos ->
                    (recyclerView.layoutManager?.findViewByPosition(pos) as? ViewGroup)?.children?.first()?.updateLayoutParams {
                        this.width = 220.dpToPx
                        this.height = 280.dpToPx
                    }
                }
                val holder = recyclerView.findContainingViewHolder(view) ?: return
                if (previousPos != holder.adapterPosition) playTickSound()
                previousPos = holder.adapterPosition
                val snapView = SnapHelper.findSnapView(recyclerView) ?: return
                (snapView as? ViewGroup)?.children?.first()?.updateLayoutParams {
                    this.width = 250.dpToPx
                    this.height = 340.dpToPx
                   // this.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                super.onScrolled(recyclerView, dx, dy)

            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    preload?.invoke(recyclerView.canScrollHorizontally(Layout.DIR_RIGHT_TO_LEFT))
                    val view = SnapHelper.findSnapView(recyclerView) ?: return
                    (view as? ViewGroup)?.children?.first()?.updateLayoutParams {
                        this.width = 250.dpToPx
                        this.height = 340.dpToPx
                        //this.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        })
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun setInitialMargin() {
        main {
            kotlinx.coroutines.delay(200)
            for (pos in 0..(recyclerView?.adapter?.itemCount ?: 0)) {
                (recyclerView?.layoutManager?.findViewByPosition(pos) as? ViewGroup)?.children?.first()?.updateLayoutParams {
                    this.width = 220.dpToPx
                    this.height = 280.dpToPx
                }
            }
            val view = SnapHelper.findSnapView(recyclerView ?: return@main)
            (view as? ViewGroup)?.children?.first()?.updateLayoutParams {
                this.width = 250.dpToPx
                this.height = 340.dpToPx
                //this.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    class SnapHelper : LinearSnapHelper() {
        companion object {
            private val instance = LinearSnapHelper()
            fun attachToRecyclerView(rv: RecyclerView) {
                instance.attachToRecyclerView(null)
                instance.attachToRecyclerView(rv)
            }

            fun findSnapView(recyclerView: RecyclerView): View? {
                return instance.findSnapView(recyclerView.layoutManager)
            }
        }

        override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager,
            velocityX: Int,
            velocityY: Int
        ): Int {
            if (layoutManager !is ScrollVectorProvider) {
                return RecyclerView.NO_POSITION
            }
            val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
            val currentPosition = layoutManager.getPosition(currentView)
            return if (currentPosition == RecyclerView.NO_POSITION) {
                RecyclerView.NO_POSITION
            } else currentPosition
        }
    }
}


package il.co.superclick.cart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundApplication
import il.co.superclick.R
import java.util.*

abstract class SwipeHelper(private val recyclerView: RecyclerView) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
    private var buttons: MutableList<UnderlayButton>? = null
    private var gestureDetector: GestureDetector? = null
    private var swipedPos = -1
    private var swipeThreshold = 0.5f
    private val buttonsBuffer: MutableMap<Int, MutableList<UnderlayButton>>
    private var recoverQueue: Queue<Int>? = null
    private val gestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            buttons?.forEach {
                if (it.onClick(e.x, e.y)) return@forEach
            }
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = OnTouchListener { view, e ->
        if (swipedPos < 0) return@OnTouchListener false
        val point = Point(e.rawX.toInt(), e.rawY.toInt())
        val swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos)
        val swipedItem = swipedViewHolder?.itemView
        val rect = Rect()
        swipedItem?.getGlobalVisibleRect(rect)
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_MOVE) {
            if (rect.top < point.y && rect.bottom > point.y) gestureDetector?.onTouchEvent(e) else {
                recoverQueue?.add(swipedPos)
                swipedPos = -1
                recoverSwipedItem()
            }
        }
        false
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = swipeThreshold
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = 0.1f * defaultValue
    override fun getSwipeVelocityThreshold(defaultValue: Float): Float = 5.0f * defaultValue

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition
        if (swipedPos != pos) recoverQueue?.add(swipedPos)
        swipedPos = pos
        if (buttonsBuffer.containsKey(swipedPos)) buttons = buttonsBuffer[swipedPos]!! else buttons?.clear()
        buttonsBuffer.clear()
        swipeThreshold = 0.5f * (buttons?.size ?: 0)
        recoverSwipedItem()
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView
        if (pos < 0) {
            swipedPos = pos
            return
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX > 0) {
                var buffer: MutableList<UnderlayButton> = ArrayList()
                if (!buttonsBuffer.containsKey(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer)
                    buttonsBuffer[pos] = buffer
                } else {
                    buffer = buttonsBuffer[pos]!!
                }
                translationX = itemView.width * 0.23f
                drawButtons(c, itemView, buffer, pos, translationX)
            }
        }
        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            translationX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    @Synchronized
    private fun recoverSwipedItem() {
        while (recoverQueue?.isNotEmpty() == true) {
            val pos = recoverQueue?.poll()
            if (pos != null) {
                if (pos > -1) {
                    recyclerView.adapter?.notifyItemChanged(pos)
                }
            }
        }
    }

    private fun drawButtons(
        c: Canvas,
        itemView: View,
        buffer: List<UnderlayButton>,
        pos: Int,
        dX: Float
    ) {
        var right = dX
        val left = 0f
        val bgColor = itemView.context.getColor(R.color.white)
        for (button in buffer) {
            button.onDraw(
                c,
                RectF(
                    left,
                    itemView.top.toFloat(),
                    right,
                    itemView.bottom.toFloat()
                ),
                bgColor,
                pos
            )
            right = left
        }
    }

    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    abstract fun instantiateUnderlayButton(
        viewHolder: RecyclerView.ViewHolder?,
        underlayButtons: MutableList<UnderlayButton>?
    )

    class UnderlayButton(
        private var iconDrawable: Drawable? = null,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private var pos = 0
        private var clickRegion: RectF? = null
        fun onClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                clickListener.onClick(pos)
                return true
            }
            return false
        }

        fun onDraw(c: Canvas, rect: RectF, color: Int, pos: Int ) {
            val p = Paint()
            p.color = color
            c.drawRoundRect(rect, 30f, 30f, p)
            val left = rect.right * 0.33
            val right = rect.right * 0.66
            val top = rect.top + (rect.bottom - rect.top) * 0.35
            val bottom = rect.top + (rect.bottom - rect.top) * 0.65
            getIcon(foregroundApplication.applicationContext)?.apply {
                setBounds(left.toInt(), top.toInt() , right.toInt(), bottom.toInt())
                draw(c)
            }
            clickRegion = rect
            this.pos = pos
        }

        private fun getIcon(context: Context): Drawable? {
            if (iconDrawable == null) iconDrawable = ContextCompat.getDrawable(context, R.drawable.trash)
            return iconDrawable
        }
    }

    interface UnderlayButtonClickListener {
        fun onClick(pos: Int)
    }

    init {
        buttons = ArrayList()
        gestureDetector = GestureDetector(recyclerView.context, gestureListener)
        recyclerView.setOnTouchListener(onTouchListener)
        buttonsBuffer = HashMap()
        recoverQueue = object : LinkedList<Int>() {
            override fun add(element: Int): Boolean {
                return if (contains(element)) false else super.add(element)
            }
        }
        attachSwipe()
    }
}
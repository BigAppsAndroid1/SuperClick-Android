package il.co.superclick.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.*
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.onClick
import com.dm6801.framework.utilities.background
import com.dm6801.framework.utilities.main
import il.co.superclick.R
import il.co.superclick.data.Category
import il.co.superclick.data.ProductsRepository
import il.co.superclick.data.Shop
import il.co.superclick.infrastructure.Locator
import il.co.superclick.infrastructure.foregroundFragment
import il.co.superclick.product_list.ProductListFragment
import il.co.superclick.utilities.glide
import kotlinx.android.synthetic.main.item_category.view.*
import kotlinx.android.synthetic.main.view_categories.view.*

@Suppress("UsePropertyAccessSyntax")
class CategoriesBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val KEY_IS_SELECTED = "KEY_IS_SELECTED"
        private val database get() = Locator.database
        private val categories get() = database.shop?.categories
    }

    val categoriesDiffer: DiffUtil.ItemCallback<Item> = object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem && oldItem.isSelected == newItem.isSelected
        }

        override fun getChangePayload(oldItem: Item, newItem: Item): Any? {
            return if (oldItem.isSelected != newItem.isSelected) KEY_IS_SELECTED
            else super.getChangePayload(oldItem, newItem)
        }
    }

    val recycler: RecyclerView? get() = categories_bar_recycler
    private val adapter: Adapter? get() = recycler?.adapter as? Adapter
    val subCategoriesRecycler: RecyclerView? get() = sub_categories_bar_recycler
    private val subCategoriesAdapter: SubCategoriesAdapter? get() = subCategoriesRecycler?.adapter as? SubCategoriesAdapter
    private var category: String? = null
    private val subCategories get() = database.shop?.subCategories?.filter { subCat -> subCat.parentId == (categories?.firstOrNull { it.name == category }?.id ?: 0) }

    init {
        if ((categories?.size ?: 0) > 1 || isInEditMode) {
            inflate(context, R.layout.view_categories, this)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Shop.getShopColor())
        } else {
            recycler?.isGone = true
            isGone = true
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initRecycler()
    }

    private fun initRecycler() {
        val categories = categories ?: return
        recycler?.adapter = Adapter(categories, categories.indexOfFirst { it.name == category })
    }

    private fun initSubCategoriesRecycler(){
        val subCategories = subCategories ?: return
        subCategoriesRecycler?.adapter = SubCategoriesAdapter(subCategories, 0)
        (foregroundFragment as? ProductListFragment)?.viewModel?.subCategory?.postValue(null)
    }

    fun set(category: String?) {
        if (category.isNullOrBlank() || this.category == category) return
        val index = categories?.indexOfFirst { it.name == category } ?: -1
        if (index == -1) return
        this.category = category
        adapter?.select(index)
        initSubCategoriesRecycler()
        ProductListFragment.open(category)
    }

    @Suppress("UNUSED_PARAMETER")
    private inner class Adapter(
        categories: List<Category>,
        initialPosition: Int
    ) : ListAdapter<Item, Adapter.ViewHolder>(categoriesDiffer) {

        init {
            setHasStableIds(true)
            submitList(categories)
        }

        override fun onCurrentListChanged(
            previousList: MutableList<Item>,
            currentList: MutableList<Item>
        ) {
            super.onCurrentListChanged(previousList, currentList)
            recycler?.scrollToPosition(0)
            recycler?.postDelayed(500) {
                if (!areAllItemsShowing()) offsetItem()
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun submitList(
            categories: List<Category>,
            callback: (() -> Unit)? = null
        ) {
            submitList(categories.mapIndexed { index, category ->
                Item(
                    index.toLong(),
                    category,
                    false
                )
            }, callback)
        }

        fun areAllItemsShowing(): Boolean {
            return (recycler?.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition() == currentList.lastIndex
        }

        fun select(index: Int) {
            if (index !in 0 until itemCount) return
            val previous = currentList.indexOfFirst { it.isSelected }
            if (previous != -1) {
                offsetItem(index, previous)
                adapter?.getItem(previous)?.isSelected = false
                notifyItemChanged(previous)
            }
            adapter?.getItem(index)?.isSelected = true
            notifyItemChanged(index)
        }

        private fun offsetItem(index: Int = 0, previousIndex: Int = 0) {
            val itemView = recycler?.findViewHolderForAdapterPosition(index)?.itemView
            val posX = itemView?.x?.toInt() ?: return
            val middle =  (itemView.parent as? ViewGroup)?.width?.div(2) ?: return
            val x = when {
                itemView.width > 0 -> itemView.width
                itemView.measuredWidth> 0 -> itemView.measuredWidth
                else -> null
            }?.div(2) ?: return
            recycler?.scrollBy(posX-middle+x, 0)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position !in 0 until itemCount) return
            holder.bind(getItem(position) ?: return)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            when {
                payloads.contains(KEY_IS_SELECTED) -> holder.update(getItem(position))
            }
        }

        override fun getItemId(position: Int): Long {
            val id = if (position in 0 until itemCount) getItem(position)?.id ?: -1 else -1
            return if (id != -1L) id else super.getItemId(position)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView? get() = itemView.item_category_name
            private val imageView: ImageView? get() = itemView.item_category_icon

            fun bind(item: Item) {
                textView?.text = item.category.displayName.trim()
                imageView?.glide(item.category.iconUrl)
                setIsSelected(item.isSelected)
                itemView.onClick {
                    select()
                    initSubCategoriesRecycler()
                    ProductListFragment.open(item.category.name)
                }
            }

            fun update(item: Item) {
                setIsSelected(item.isSelected)
            }

            private fun setIsSelected(isSelected: Boolean) {
                if (isSelected) select()
                else unSelect()
            }

            private fun select() {
                imageView?.setBackgroundResource(R.drawable.circle_white)
            }

            private fun unSelect() {
                imageView?.setBackgroundResource(R.drawable.circle_white_border)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private inner class SubCategoriesAdapter(
        categories: List<Category>,
        initialPosition: Int
    ) : ListAdapter<Item, SubCategoriesAdapter.ViewHolder>(categoriesDiffer) {

        init {
            setHasStableIds(true)
            submitList(categories)
        }

        override fun onCurrentListChanged(
            previousList: MutableList<Item>,
            currentList: MutableList<Item>
        ) {
            super.onCurrentListChanged(previousList, currentList)
            subCategoriesRecycler?.scrollToPosition(0)
            subCategoriesRecycler?.postDelayed(500) {
                if (!areAllItemsShowing()) offsetItem()
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun submitList(
            categories: List<Category>,
            callback: (() -> Unit)? = null
        ) {
            submitList(categories.mapIndexed { index, category ->
                Item(
                    index.toLong(),
                    category,
                    false
                )
            }, callback)
        }

        fun areAllItemsShowing(): Boolean {
            return (subCategoriesRecycler?.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition() == currentList.lastIndex
        }

        fun select(index: Int) {
            if (index !in 0 until itemCount) return
            val previous = currentList.indexOfFirst { it.isSelected }
            if (previous != -1) {
                offsetItem(index, previous)
                subCategoriesAdapter?.getItem(previous)?.isSelected = false
                notifyItemChanged(previous)
            }
            subCategoriesAdapter?.getItem(index)?.isSelected = true
            notifyItemChanged(index)
        }

        private fun offsetItem(index: Int = 0, previousIndex: Int = 0) {
            val itemView = subCategoriesRecycler?.findViewHolderForAdapterPosition(index)?.itemView
            val posX = itemView?.x?.toInt() ?: return
            val middle =  (itemView.parent as? ViewGroup)?.width?.div(2) ?: return
            val x = when {
                itemView.width > 0 -> itemView.width
                itemView.measuredWidth> 0 -> itemView.measuredWidth
                else -> null
            }?.div(2) ?: return
            subCategoriesRecycler?.scrollBy(posX-middle+x, 0)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_sub_category, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position !in 0 until itemCount) return
            holder.bind(getItem(position) ?: return)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            when {
                payloads.contains(KEY_IS_SELECTED) -> holder.update(getItem(position))
            }
        }

        override fun getItemId(position: Int): Long {
            val id = if (position in 0 until itemCount) getItem(position)?.id ?: -1 else -1
            return if (id != -1L) id else super.getItemId(position)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bind(item: Item) {
                (itemView as? TextView)?.text = item.category.displayName.trim()
                setIsSelected(item.isSelected)
                itemView.onClick {
                    background {
                        ProductsRepository.fetchCategory(item.category.parentId.toString()) {
                            main {
                                (foregroundFragment as? ProductListFragment)?.viewModel?.subCategory?.postValue(item.category.name)
                            }
                        }
                    }
                    subCategoriesAdapter?.select(adapterPosition)
                }
            }

            fun update(item: Item) {
                setIsSelected(item.isSelected)
            }

            private fun setIsSelected(isSelected: Boolean) {
                if (isSelected) select()
                else unSelect()
            }

            private fun select() {
                itemView.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                (itemView as? TextView)?.setTextColor(Color.BLACK)
            }

            private fun unSelect() {
                itemView.backgroundTintList = null
                (itemView as? TextView)?.setTextColor(Color.WHITE)
            }
        }


    }

    data class Item(
        val id: Long,
        val category: Category,
        var isSelected: Boolean
    )

}
package il.co.superclick.network

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dm6801.framework.infrastructure.foregroundApplication
import com.dm6801.framework.ui.getDrawable
import il.co.superclick.R
import il.co.superclick.data.Tag
import il.co.superclick.utilities.onClick
import kotlinx.android.synthetic.main.item_tag.view.*

class TagsAdapter: ListAdapter<Tag, TagsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagName: TextView? get() = itemView.tag_name

        fun bind(item: Tag) {
            tagName?.text = item.name
            if (item.isChecked) {
                tagName?.setTextColor(foregroundApplication.getColor(R.color.blue))
                tagName?.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            }
            else {
                tagName?.setTextColor(Color.WHITE)
                tagName?.backgroundTintList = null
                tagName?.background = getDrawable(R.drawable.button_sides_border_rounded_1dp)
            }

            itemView.onClick {
                item.isChecked = !item.isChecked
                notifyItemChanged(position)
            }
        }
    }

}
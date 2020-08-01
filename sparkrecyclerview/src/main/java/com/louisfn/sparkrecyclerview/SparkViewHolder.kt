package com.louisfn.sparkrecyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class SparkViewHolder<I : Any>(private val _adapter: SparkAdapter<*>, itemView: View) : RecyclerView.ViewHolder(itemView) {

    @Suppress("UNCHECKED_CAST")
    private val adapter: SparkAdapter<I>
        get() {
            return _adapter as SparkAdapter<I>
        }

    val elementPosition: Int
        get() = adapter.calculateElementPosition(adapterPosition)

    init {
        this.setupClickListener()
        this.setupLongClickListener()
    }

    open fun setupClickListener() {
        itemView.setOnClickListener {
            val element = adapter.elements.getOrNull(elementPosition) ?: return@setOnClickListener
            onClick(element, elementPosition)
            adapter.clickListener?.invoke(element, elementPosition)
        }
    }

    open fun setupLongClickListener() {
        itemView.setOnLongClickListener {
            val element = adapter.elements.getOrNull(elementPosition) ?: return@setOnLongClickListener false
            val isConsume = onLongClick(element, elementPosition)
            adapter.longClickListener?.invoke(element, elementPosition)
            return@setOnLongClickListener isConsume && adapter.longClickListener != null
        }
    }

    open fun onClick(element: I, elementPosition: Int) {}
    open fun onLongClick(element: I, elementPosition: Int): Boolean = true

    open fun onViewAttachedToWindow() {}
    open fun onViewDetachedFromWindow() {}
    open fun onViewRecycled() {}

    abstract fun reset()
    abstract fun bind(element: I, position: Int, payloads: MutableList<Any>)
}

internal class SparkHeaderFooterViewHolder<I : Any>(adapter: SparkAdapter<*>, itemView: View) : SparkViewHolder<I>(adapter, itemView) {
    override fun reset() {}
    override fun bind(element: I, position: Int, payloads: MutableList<Any>) {}
    override fun setupClickListener() {}
    override fun setupLongClickListener() {}
}

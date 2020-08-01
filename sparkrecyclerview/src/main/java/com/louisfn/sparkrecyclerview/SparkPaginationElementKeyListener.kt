package com.louisfn.sparkrecyclerview

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class SparkPaginationElementKeyListener<E : Any>(val adapter: SparkAdapter<E>) : RecyclerView.OnScrollListener() {

    companion object {
        const val DEFAULT_VISIBLE_THRESHOLD = 5
    }

    var visibleItemsThreshold = DEFAULT_VISIBLE_THRESHOLD

    private var previousFirstElement: E? = null
    private var previousLastElement: E? = null
    private var layoutManager = adapter.layoutManager

    init {
        val layoutManager = layoutManager
        if (layoutManager is GridLayoutManager) {
            visibleItemsThreshold *= layoutManager.spanCount
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (adapter.elements.isEmpty())
            return

        if (getLastVisibleItemPosition() + visibleItemsThreshold >= (adapter.itemCount - 1) && (dx > 0 || dy > 0)) {
            val previousLastElement = previousLastElement
            val lastElement = getLastElement()
            val canLoadMore = canLoadAfter(lastElement, adapter.elements.size, recyclerView as SparkRecyclerView)
                ?: (previousLastElement == null || !adapter.areElementsTheSame(previousLastElement, lastElement))
            if (canLoadMore) {
                loadAfter(lastElement, adapter.elements.size, recyclerView)
                this.previousLastElement = lastElement
            }
        }

        if (getFirstVisibleItemPosition() - visibleItemsThreshold <= 0 && (dx < 0 || dy < 0)) {
            val previousFirstElement = previousFirstElement
            val firstElement = getFirstElement()
            val canLoadMore = canLoadBefore(firstElement, adapter.elements.size, recyclerView as SparkRecyclerView)
                ?: (previousFirstElement == null || !adapter.areElementsTheSame(previousFirstElement, firstElement))
            if (canLoadMore) {
                loadBefore(firstElement, adapter.elements.size, recyclerView)
                this.previousFirstElement = firstElement
            }
        }
    }

    abstract fun loadBefore(element: E, elementCount: Int, recyclerView: SparkRecyclerView)

    abstract fun loadAfter(element: E, elementCount: Int, recyclerView: SparkRecyclerView)

    open fun canLoadBefore(element: E, elementCount: Int, recyclerView: SparkRecyclerView): Boolean? = null

    open fun canLoadAfter(element: E, elementCount: Int, recyclerView: SparkRecyclerView): Boolean? = null

    fun reset() {
        previousFirstElement = null
        previousLastElement = null
    }

    // Private methods

    private fun getFirstElement(): E {
        return adapter.elements.first()
    }

    private fun getLastElement(): E {
        return adapter.elements.last()
    }

    private fun getFirstVisibleItemPosition(): Int {
        return when (val layoutManager = layoutManager) {
            is GridLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            else -> throw IllegalStateException("LayoutManager ${layoutManager!!::class.java} is not managed")
        }
    }

    private fun getLastVisibleItemPosition(): Int {
        return when (val layoutManager = layoutManager) {
            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
            else -> throw IllegalStateException("LayoutManager ${layoutManager!!::class.java} is not managed")
        }
    }

}
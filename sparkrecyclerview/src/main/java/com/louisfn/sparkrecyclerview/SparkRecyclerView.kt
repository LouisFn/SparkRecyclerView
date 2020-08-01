package com.louisfn.sparkrecyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicBoolean


@Suppress("unused", "MemberVisibilityCanBePrivate")
open class SparkRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    var emptyView: View? = null
    var showEmptyViewIfEmpty: Boolean = true
    var showEmptyViewWithHeaderOrFooter = false
    var removeAdapterWhenViewIsDetached = true

    private var maxHeight: Int = -1

    private var paginationDataSource: SparkPaginationElementKeyDataSource<*>? = null
    private var paginationListener: SparkPaginationElementKeyListener<*>? = null

    private var reorderTouchHelper: ItemTouchHelper? = null
    private var scrollToNextInsertedItem = AtomicBoolean(false)
    private var scrollToTopAfterChangedItems = AtomicBoolean(false)
    private var scrollToBottomAfterChangedItems = AtomicBoolean(false)

    // RecyclerView

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.SparkRecyclerView, 0, 0)
            maxHeight = typedArray.getDimensionPixelSize(R.styleable.SparkRecyclerView_maxHeight, -1)
            typedArray.recycle()
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        require(adapter == null || adapter is SparkAdapter<*>) { "Adapter must be a SparkAdapter" }

        this.adapter?.unregisterAdapterDataObserver(adapterObserver)
        super.setAdapter(adapter)
        setupSparkAdapterIfExist()
        adapter?.registerAdapterDataObserver(adapterObserver)
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
        setupSparkAdapterIfExist()
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (removeAdapterWhenViewIsDetached) {
            (adapter as? SparkAdapter<*>)?.unregisterAllAdapterDataObservers()
            adapter = null
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        if (maxHeight > -1) {
            val heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
            super.onMeasure(widthSpec, heightMeasureSpec)
        } else {
            super.onMeasure(widthSpec, heightSpec)
        }
    }

    // Public methods

    fun isEmptyViewShown(): Boolean {
        return emptyView != null && emptyView?.visibility == View.VISIBLE
    }

    fun enableDragAndDrop() {
        val adapter = adapter as? SparkAdapter<*> ?: return

        val callback = SparkDragNDropElementTouchCallback(adapter)
        reorderTouchHelper = ItemTouchHelper(callback).apply {
            attachToRecyclerView(this@SparkRecyclerView)
        }
    }

    fun disableDragAndDrop() {
        reorderTouchHelper?.attachToRecyclerView(null)
    }

    fun scrollToNextInsertedItem() {
        scrollToNextInsertedItem.set(true)
    }

    fun scrollToTopAfterChangedItems() {
        scrollToTopAfterChangedItems.set(true)
    }

    fun scrollToBottomAfterChangedItems() {
        scrollToBottomAfterChangedItems.set(true)
    }

    fun scrollToTop() {
        scrollToPosition(0)
    }

    fun scrollToBottom() {
        val adapter = adapter ?: return
        if (adapter.itemCount == 0)
            return
        scrollToPosition(adapter.itemCount - 1)
    }

    fun smoothScrollToTop() {
        smoothScrollToPosition(0)
    }

    fun smoothScrollToBottom() {
        val adapter = adapter ?: return
        if (adapter.itemCount == 0)
            return
        smoothScrollToPosition(adapter.itemCount - 1)
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Any> setPaginationDataSource(dataSource: SparkPaginationElementKeyDataSource<E>) {
        val sparkAdapter = adapter as? SparkAdapter<E> ?: throw IllegalStateException("Adapter should inherit from SparkAdapter")
        this.paginationDataSource = dataSource
        this.paginationListener = object : SparkPaginationElementKeyListener<E>(sparkAdapter) {
            override fun loadBefore(element: E, elementCount: Int, recyclerView: SparkRecyclerView) {
                dataSource.loadBefore(element, elementCount)
            }

            override fun loadAfter(element: E, elementCount: Int, recyclerView: SparkRecyclerView) {
                dataSource.loadAfter(element, elementCount)
            }

            override fun canLoadAfter(element: E, elementCount: Int, recyclerView: SparkRecyclerView): Boolean? {
                return dataSource.canLoadAfter(element, elementCount)
            }

            override fun canLoadBefore(element: E, elementCount: Int, recyclerView: SparkRecyclerView): Boolean? {
                return dataSource.canLoadBefore(element, elementCount)
            }
        }
        paginationListener?.let {
            it.visibleItemsThreshold = dataSource.visibleItemsThreshold
            addOnScrollListener(it)
        }
    }

    fun disablePaginationDataSource() {
        paginationListener?.let {
            removeOnScrollListener(it)
        }
        paginationListener = null
        paginationDataSource = null
    }

    fun resetPagination() {
        paginationDataSource?.reset()
        paginationListener?.reset()
    }

    // Private methods

    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            refreshEmptyViewVisibility()
            scrollToTopOrBottomIfNeededAfterChangedItems()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            refreshEmptyViewVisibility()
            if (scrollToNextInsertedItem.compareAndSet(true, false))
                scrollToPosition(positionStart)
            scrollToTopOrBottomIfNeededAfterChangedItems()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            refreshEmptyViewVisibility()
            scrollToTopOrBottomIfNeededAfterChangedItems()
        }
    }

    private fun scrollToTopOrBottomIfNeededAfterChangedItems() {
        if (scrollToTopAfterChangedItems.compareAndSet(true, false))
            scrollToTop()
        if (scrollToBottomAfterChangedItems.compareAndSet(true, false))
            scrollToBottom()
    }

    private fun refreshEmptyViewVisibility() {
        val adapter = adapter as? SparkAdapter<*> ?: return
        val emptyView = emptyView ?: return

        if (showEmptyViewWithHeaderOrFooter) {
            val shouldDisplayEmptyView = showEmptyViewIfEmpty && adapter.elements.isEmpty()
            isInvisible = adapter.itemCount == 0 && shouldDisplayEmptyView
            emptyView.isInvisible = !shouldDisplayEmptyView
        } else {
            val shouldDisplayEmptyView = showEmptyViewIfEmpty && adapter.itemCount == 0
            isInvisible = shouldDisplayEmptyView
            emptyView.isInvisible = !shouldDisplayEmptyView
        }

    }

    private fun setupSparkAdapterIfExist() {
        val adapter = adapter as? SparkAdapter<*> ?: return
        adapter.layoutManager = this.layoutManager
    }

}

package com.louisfn.sparkrecyclerview

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SparkDragNDropElementTouchCallback<E : Any>(private val adapter: SparkAdapter<E>) : ItemTouchHelper.Callback() {

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (adapter.isHeader(viewHolder.adapterPosition) || adapter.isFooter(viewHolder.adapterPosition))
            return 0
        if (!adapter.canSelectedElement(adapter.calculateElementPosition(viewHolder.adapterPosition)))
            return 0

        var dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        if (adapter.layoutManager is GridLayoutManager) {
            dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        }
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (source.adapterPosition == RecyclerView.NO_POSITION || target.adapterPosition == RecyclerView.NO_POSITION)
            return false
        if (adapter.isHeaderOrFooter(source.adapterPosition) || adapter.isHeaderOrFooter(target.adapterPosition))
            return false

        val fromIndex = adapter.calculateElementPosition(source.adapterPosition)
        val toIndex = adapter.calculateElementPosition(target.adapterPosition)

        if (!adapter.canReleasedElement(toIndex))
            return false

        adapter.onElementMove(fromIndex, toIndex)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    @Suppress("UNCHECKED_CAST")
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        viewHolder ?: return
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            adapter.onElementSelected(viewHolder as SparkViewHolder<E>)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        adapter.onElementReleased(viewHolder as SparkViewHolder<E>)
    }
}
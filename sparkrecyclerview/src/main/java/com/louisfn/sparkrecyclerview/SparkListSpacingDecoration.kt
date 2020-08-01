package com.louisfn.sparkrecyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@Suppress("unused")
class SparkListSpacingDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val layoutManager = parent.layoutManager
        check(layoutManager is LinearLayoutManager) { "The LayoutManager from RecyclerView must be Linear" }

        when (layoutManager.orientation) {
            LinearLayoutManager.HORIZONTAL -> addSpaceForHorizontalList(outRect, view, parent)
            LinearLayoutManager.VERTICAL -> addSpaceForVerticalList(outRect, view, parent)
        }
    }

    private fun addSpaceForVerticalList(outRect: Rect, view: View, parent: RecyclerView) {
        val position = parent.getChildAdapterPosition(view)
        val hasHeader = (parent.adapter as? SparkAdapter<*>)?.hasHeader() ?: false

        if ((hasHeader && position >= 2) || (!hasHeader && position >= 1)) {
            outRect.top = space
        }
    }

    private fun addSpaceForHorizontalList(outRect: Rect, view: View, parent: RecyclerView) {
        if (parent.getChildAdapterPosition(view) != 0) {
            outRect.left = space
        }
    }

}
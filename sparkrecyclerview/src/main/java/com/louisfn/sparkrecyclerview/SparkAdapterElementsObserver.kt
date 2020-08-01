package com.louisfn.sparkrecyclerview

import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

//
// Created by Louis Farin on 2019-11-23.
// Copyright (c) 2019 do.ne. All rights reserved.
//

abstract class SparkAdapterElementsObserver : RecyclerView.AdapterDataObserver() {

    private lateinit var adapter: WeakReference<SparkAdapter<*>>

    internal fun setAdapter(adapter: SparkAdapter<*>) {
        this.adapter = WeakReference(adapter)
    }

    open fun onElementRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {}

    open fun onElementRangeInserted(positionStart: Int, itemCount: Int) {}

    open fun onElementRangeRemoved(positionStart: Int, itemCount: Int) {}

    open fun onElementRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {}

    // AdapterDataObserver

    final override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        check(this::adapter.isInitialized)

        val adapter = adapter.get() ?: return
        var adapterPositionStart = positionStart
        var adapterItemCount = itemCount
        if (adapter.isHeader(positionStart)) {
            adapterPositionStart++
            adapterItemCount--
        }
        if (adapter.isFooter(positionStart + itemCount - 1)) {
            adapterItemCount--
        }

        if (itemCount > 0)
            onElementRangeChanged(adapter.calculateElementPosition(adapterPositionStart), adapterItemCount, payload)
    }

    final override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        check(this::adapter.isInitialized)

        val adapter = adapter.get() ?: return
        var adapterPositionStart = positionStart
        var adapterItemCount = itemCount
        if (adapter.isHeader(positionStart)) {
            adapterPositionStart++
            adapterItemCount--
        }
        if (adapter.isFooter(positionStart + itemCount - 1)) {
            adapterItemCount--
        }

        if (itemCount > 0)
            onElementRangeInserted(adapter.calculateElementPosition(adapterPositionStart), adapterItemCount)
    }

    final override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        check(this::adapter.isInitialized)

        val adapter = adapter.get() ?: return
        var adapterPositionStart = positionStart
        var adapterItemCount = itemCount
        if (adapter.isHeader(positionStart)) {
            adapterPositionStart++
            adapterItemCount--
        }
        if (adapter.isFooter(positionStart + itemCount - 1)) {
            adapterItemCount--
        }

        if (itemCount > 0)
            onElementRangeRemoved(adapter.calculateElementPosition(adapterPositionStart), adapterItemCount)
    }

    final override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        super.onItemRangeMoved(fromPosition, toPosition, itemCount)

        check(this::adapter.isInitialized)

        val adapter = adapter.get() ?: return
        var adapterFromPosition = fromPosition
        var adapterToPosition = toPosition
        var count = itemCount

        if (adapter.isHeader(fromPosition)) {
            adapterFromPosition++
            count--
        }
        if (adapter.isFooter(toPosition)) {
            adapterToPosition--
            count--
        }

        if (count > 0)
            onElementRangeMoved(adapter.calculateElementPosition(adapterFromPosition), adapter.calculateElementPosition(adapterToPosition), count)
    }

}
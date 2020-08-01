package com.louisfn.sparkrecyclerview

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class SparkAsyncListDiffer<E : Any>(private val adapter: SparkAdapter<E>) {

    companion object {
        private val mainThreadExecutor = MainThreadExecutor()
        private val backgroundThreadExecutor = Executors.newFixedThreadPool(2)
    }

    var currentList: List<SparkItem<E>> = emptyList()
        private set

    private var maxScheduledGeneration: Int = 0
    private val updateCallback = AdapterListUpdateCallback(adapter)

    fun submitList(newList: List<SparkItem<E>>?, inBackground: Boolean = true) {
        val runGeneration = ++maxScheduledGeneration

        if (newList === currentList) {
            return
        }

        // fast simple remove all
        if (newList == null) {
            val countRemoved = currentList.size
            currentList = emptyList()
            // notify last, after list is updated
            updateCallback.onRemoved(0, countRemoved)
            return
        }

        // fast simple first insert
        if (currentList.none { !it.isHeaderOrFooter }) {
            currentList = newList
            adapter.notifyDataSetChanged()
            return
        }

        val oldList = currentList

        if (inBackground) {
            backgroundThreadExecutor.execute {
                val result = calculateDiff(oldList, newList)
                mainThreadExecutor.execute {
                    if (maxScheduledGeneration == runGeneration) {
                        latchList(newList, result)
                    }
                }
            }
        } else {
            latchList(newList, calculateDiff(oldList, newList))
        }
    }

    private fun calculateDiff(oldList: List<SparkItem<E>>, newList: List<SparkItem<E>>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]

                if (!oldItem.isHeaderOrFooter && !newItem.isHeaderOrFooter)
                    return adapter.areElementsTheSame(oldItem.element!!, newItem.element!!)

                return (oldItem.isHeader && newItem.isHeader) || (oldItem.isFooter && newItem.isFooter)
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]

                if (!oldItem.isHeaderOrFooter && !newItem.isHeaderOrFooter)
                    return adapter.areContentsTheSame(oldItem.element!!, newItem.element!!)
                if (oldItem.isHeaderOrFooter && newItem.isHeaderOrFooter)
                    return true

                throw AssertionError()
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]

                if (!oldItem.isHeaderOrFooter && !newItem.isHeaderOrFooter) {
                    return adapter.getChangePayload(oldItem.element!!, newItem.element!!)
                }

                throw AssertionError()
            }
        })
    }

    private fun latchList(newList: List<SparkItem<E>>, diffResult: DiffUtil.DiffResult) {
        currentList = newList
        diffResult.dispatchUpdatesTo(updateCallback)
    }

}

private class MainThreadExecutor : Executor {
    private val mHandler = Handler(Looper.getMainLooper())
    override fun execute(command: Runnable) {
        mHandler.post(command)
    }
}
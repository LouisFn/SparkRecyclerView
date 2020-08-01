package com.louisfn.sparkrecyclerview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

//
// Created by Louis Farin on 2019-10-20.
// Copyright (c) 2019 do.ne. All rights reserved.
//

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class SparkAdapter<E : Any> : RecyclerView.Adapter<SparkViewHolder<*>>() {

    companion object {
        const val TYPE_HEADER = 123
        const val TYPE_FOOTER = 456
        const val TYPE_ELEMENT = 789
    }

    var layoutManager: RecyclerView.LayoutManager? = null
    val elements: List<E>
        get() = diff.currentList.mapNotNull { it.element }

    var clickListener: ((element: E, position: Int) -> Unit)? = null
    var longClickListener: ((element: E, position: Int) -> Unit)? = null
    var dragAndDropListener: OnDragAndDropListener<E>? = null
    var bindViewHolderListener: ((element: E, elementPosition: Int) -> Unit)? = null
    var viewHolderAttachedListener: ((elementPosition: Int) -> Unit)? = null
    var viewHolderDetachedListener: ((elementPosition: Int) -> Unit)? = null

    @Suppress("LeakingThis")
    private val diff = SparkAsyncListDiffer(this)
    private var headerView: View? = null
    private var footerView: View? = null
    private var pendingElements: List<E> = emptyList()
    private val adapterDataObservers = CopyOnWriteArrayList<RecyclerView.AdapterDataObserver>()

    // LifeCycle

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): SparkViewHolder<*> {
        return when (type) {
            TYPE_HEADER -> SparkHeaderFooterViewHolder<Any>(this as SparkAdapter<Any>, headerView!!)
            TYPE_FOOTER -> SparkHeaderFooterViewHolder<Any>(this as SparkAdapter<Any>, footerView!!)
            else -> onCreateElementViewHolder(viewGroup, type)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(vh: SparkViewHolder<*>, position: Int) {
        onBindViewHolder(vh, position, mutableListOf())
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(vh: SparkViewHolder<*>, position: Int, payloads: MutableList<Any>) {
        val sparkItem = diff.currentList[position]
        if (!sparkItem.isHeaderOrFooter) {
            val elementPosition = calculateElementPosition(position)
            (vh as SparkViewHolder<Any>).apply {
                val element: E = elements[elementPosition]
                reset()
                bind(element, elementPosition, payloads)
                bindViewHolderListener?.invoke(element, elementPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return diff.currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        val sparkItem = diff.currentList[position]
        return when {
            sparkItem.isHeader -> TYPE_HEADER
            sparkItem.isFooter -> TYPE_FOOTER
            else -> getElementViewType(calculateElementPosition(position))
        }
    }

    override fun onViewAttachedToWindow(holder: SparkViewHolder<*>) {
        holder.onViewAttachedToWindow()
        val sparkItem = diff.currentList.getOrNull(holder.adapterPosition)
        if (sparkItem != null && !sparkItem.isHeaderOrFooter)
            viewHolderAttachedListener?.invoke(holder.elementPosition)
    }

    override fun onViewDetachedFromWindow(holder: SparkViewHolder<*>) {
        holder.onViewDetachedFromWindow()
        val sparkItem = diff.currentList.getOrNull(holder.adapterPosition)
        if (sparkItem != null && !sparkItem.isHeaderOrFooter)
            viewHolderDetachedListener?.invoke(holder.elementPosition)
    }

    override fun onViewRecycled(holder: SparkViewHolder<*>) {
        holder.onViewRecycled()
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        adapterDataObservers.add(observer)
        super.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        if (adapterDataObservers.contains(observer)) {
            adapterDataObservers.remove(observer)
            super.unregisterAdapterDataObserver(observer)
        }
    }

    // Abstract Methods

    abstract fun onCreateElementViewHolder(parent: ViewGroup, type: Int): SparkViewHolder<*>

    abstract fun areElementsTheSame(oldElement: E, newElement: E): Boolean

    abstract fun areContentsTheSame(oldElement: E, newElement: E): Boolean

    // Open methods

    open fun getChangePayload(oldElement: E, newElement: E): Any? = null

    protected open fun getElementViewType(position: Int): Int = TYPE_ELEMENT

    // Public Methods

    open fun addAll(elementsToAdd: List<E>?) {
        if (elementsToAdd == null) {
            return
        }

        val newElements =
            pendingElements.toMutableList().apply {
                addAll(elementsToAdd)
            }

        submitList(newElements)
    }

    open fun add(element: E) {
        val newElements =
            pendingElements.toMutableList().apply {
                add(element)
            }

        submitList(newElements)
    }

    open fun removeAll(elementsToRemove: List<E>) {
        val newElements =
            pendingElements.toMutableList().apply {
                removeAll(elementsToRemove)
            }

        submitList(newElements)
    }

    open fun remove(element: E) {
        val newElements =
            pendingElements.toMutableList().apply {
                remove(element)
            }

        submitList(newElements)
    }

    open fun set(element: E, index: Int) {
        val newElements =
            pendingElements.toMutableList().apply {
                set(index, element)
            }

        submitList(newElements)
    }

    open fun submitList(newElements: List<E>, inBackground: Boolean = true) {
        pendingElements = newElements

        val sparkItems = newElements.map { SparkItem(it) }.toMutableList()
        if (headerView != null) sparkItems.add(0, SparkItem(isHeader = true))
        if (footerView != null) sparkItems.add(SparkItem(isFooter = true))
        diff.submitList(sparkItems, inBackground)
    }

    fun notifyElementChanged(elementPosition: Int) {
        notifyItemChanged(calculateAdapterPosition(elementPosition))
    }

    fun notifyElementChanged(element: E?) {
        val elementToNotify = element ?: return
        val index = elements.indexOfFirst { areElementsTheSame(it, elementToNotify) }
        if (index != -1)
            notifyElementChanged(index)
    }

    fun setHeader(view: View) {
        headerView = view
        submitList(pendingElements)
    }

    fun setFooter(view: View) {
        footerView = view
        submitList(pendingElements)
    }

    fun removeHeader() {
        headerView = null
        submitList(pendingElements)
    }

    fun removeFooter() {
        footerView = null
        submitList(pendingElements)
    }

    fun hasHeader(): Boolean {
        return diff.currentList.firstOrNull()?.isHeader == true
    }

    fun hasFooter(): Boolean {
        return diff.currentList.lastOrNull()?.isFooter == true
    }

    fun isHeader(adapterPosition: Int): Boolean {
        return diff.currentList.getOrNull(adapterPosition)?.isHeader == true
    }

    fun isFooter(adapterPosition: Int): Boolean {
        return diff.currentList.getOrNull(adapterPosition)?.isFooter == true
    }

    fun isHeaderOrFooter(adapterPosition: Int): Boolean {
        return isHeader(adapterPosition) || isFooter(adapterPosition)
    }

    fun isFirstItem(adapterPosition: Int): Boolean {
        return adapterPosition == 0
    }

    fun isLastItem(adapterPosition: Int): Boolean {
        return adapterPosition == elements.size - 1
    }

    fun isFirstElement(element: E): Boolean {
        return elements.firstOrNull()?.let {
            areElementsTheSame(it, element)
        } ?: false
    }

    fun isLastElement(element: E): Boolean {
        return elements.lastOrNull()?.let {
            areElementsTheSame(it, element)
        } ?: false
    }

    fun calculateElementPosition(adapterPosition: Int): Int {
        check(!isHeaderOrFooter(adapterPosition))
        return adapterPosition - if (hasHeader()) 1 else 0
    }

    fun calculateAdapterPosition(elementPosition: Int): Int {
        return elementPosition + if (hasHeader()) 1 else 0
    }

    fun setOnElementClickListener(listener: (element: E, position: Int) -> Unit) {
        clickListener = listener
    }

    fun setOnElementLongClickListener(listener: (element: E, position: Int) -> Unit) {
        longClickListener = listener
    }

    fun setOnBindViewHolderListener(listener: (element: E, position: Int) -> Unit) {
        bindViewHolderListener = listener
    }

    fun setOnViewHolderAttachedListener(listener: (position: Int) -> Unit) {
        viewHolderAttachedListener = listener
    }

    fun setOnViewHolderDetachedListener(listener: (position: Int) -> Unit) {
        viewHolderDetachedListener = listener
    }

    fun setOnDragAndDropListener(listener: OnDragAndDropListener<E>) {
        dragAndDropListener = listener
    }

    fun unregisterAllAdapterDataObservers() {
        adapterDataObservers.forEach {
            unregisterAdapterDataObserver(it)
        }
    }

    // Drag And Drop

    open fun onElementMove(fromIndex: Int, toIndex: Int) {
        val newElements = pendingElements.toMutableList()

        if (fromIndex < toIndex) {
            for (i in fromIndex until toIndex) {
                Collections.swap(newElements, i, i + 1)
            }
        } else {
            for (i in fromIndex downTo toIndex + 1) {
                Collections.swap(newElements, i, i - 1)
            }
        }

        submitList(newElements, false)
    }

    open fun onElementSelected(viewHolder: SparkViewHolder<E>) {
        val elementPosition = viewHolder.elementPosition
        dragAndDropListener?.onElementSelected(elementPosition, elements[elementPosition])
    }

    open fun onElementReleased(viewHolder: SparkViewHolder<E>) {
        val elementPosition = viewHolder.elementPosition
        dragAndDropListener?.onElementReleased(elementPosition, elements[elementPosition])
    }

    open fun canSelectedElement(index: Int): Boolean {
        return true
    }

    open fun canReleasedElement(toIndex: Int): Boolean {
        return true
    }

    interface OnDragAndDropListener<E> {
        fun onElementSelected(index: Int, element: E)

        fun onElementReleased(index: Int, element: E)
    }
}
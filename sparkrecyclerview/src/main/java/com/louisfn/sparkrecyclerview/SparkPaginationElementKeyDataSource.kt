package com.louisfn.sparkrecyclerview

abstract class SparkPaginationElementKeyDataSource<E> {

    var visibleItemsThreshold = SparkPaginationElementKeyListener.DEFAULT_VISIBLE_THRESHOLD

    abstract fun loadBefore(element: E, elementCount: Int)

    abstract fun loadAfter(element: E, elementCount: Int)

    abstract fun reset()

    // By default, the SparkRecyclerView checks if the first or last element has changed since the last call to loadBefore/loadAfter to detect if we need to reload
    // more data. But for special case, it may be useful to use a different strategy (e.g if items can move) In this case,
    // we have to override these methods. If the method returns null the SparkRecyclerView will keep the default strategy.
    open fun canLoadBefore(element: E, elementCount: Int): Boolean? = null

    open fun canLoadAfter(element: E, elementCount: Int): Boolean? = null
}
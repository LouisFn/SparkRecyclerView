package com.louisfn.sparkrecyclerview

internal data class SparkItem<E>(
    var element: E? = null,
    var isHeader: Boolean = false,
    var isFooter: Boolean = false
) {

    val isHeaderOrFooter
        get() = isHeader || isFooter
}

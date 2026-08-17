package com.asptechinc.daymark.utils

object ListReorderer {
    fun <T> moveItem(
        items: MutableList<T>,
        fromPosition: Int,
        toPosition: Int,
    ): MutableList<T> {
        if (fromPosition !in items.indices || toPosition !in items.indices) {
            return items
        }

        val reorderedItems = items.toMutableList()
        val movedItem = reorderedItems.removeAt(fromPosition)
        reorderedItems.add(toPosition, movedItem)
        return reorderedItems
    }
}

package com.asptechinc.daymark.utils

object ActivityPersistence {
    fun parseCategoryId(rawValue: String?): Int? =
        rawValue
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.toIntOrNull()

    fun parseTagIds(rawValue: String?): MutableList<Int> =
        rawValue
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.split(",")
            ?.mapNotNull { item ->
                item.trim().takeIf { it.isNotEmpty() && it != "null" }?.toIntOrNull()
            }?.toMutableList()
            ?: mutableListOf()
}

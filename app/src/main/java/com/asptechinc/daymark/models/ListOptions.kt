package com.asptechinc.daymark.models

import kotlinx.serialization.Serializable

@Serializable
data class ListOptions(
    val searchText: String = "",
    val categoryId: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
    val showArchived: Boolean? = null,
    val showCompleted: Boolean? = null,
    val sortByName: Boolean = false,
)

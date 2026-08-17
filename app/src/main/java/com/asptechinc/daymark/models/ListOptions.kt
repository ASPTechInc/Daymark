package com.asptechinc.daymark.models

data class ListOptions(
    var searchText: String = "",
    var categoryId: Int? = null,
    var month: Int? = null,
    var year: Int? = null,
    var sortByName: Boolean = false,
)

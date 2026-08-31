package com.asptechinc.daymark.models

data class ChangelogVersion(
    val version: String,
    val date: String?,
    val description: String?,
    val sections: List<ChangelogSection>,
)

data class ChangelogSection(
    val title: String,
    val items: List<String>,
)

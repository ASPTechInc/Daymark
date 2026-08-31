package com.asptechinc.daymark.repository

import com.asptechinc.daymark.data.CategoryDao
import com.asptechinc.daymark.data.TagDao
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import kotlinx.coroutines.flow.Flow

class MetadataRepository(
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
) {
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun addAllCategories(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }

    suspend fun addAllTags(tags: List<Tag>) {
        tagDao.insertAll(tags)
    }
}

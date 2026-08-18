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

    suspend fun addCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun addAllCategories(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }

    suspend fun addTag(tag: Tag) {
        tagDao.insert(tag)
    }

    suspend fun addAllTags(tags: List<Tag>) {
        tagDao.insertAll(tags)
    }

    suspend fun clearAll() {
        categoryDao.deleteAll()
        tagDao.deleteAll()
    }
}

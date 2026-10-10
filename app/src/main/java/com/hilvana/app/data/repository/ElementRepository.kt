package com.hilvana.app.data.repository

import com.hilvana.app.data.local.dao.ElementDao
import com.hilvana.app.data.local.entity.toEntity
import com.hilvana.app.domain.model.Element
import com.hilvana.app.domain.model.ElementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElementRepository @Inject constructor(
    private val dao: ElementDao,
) {
    fun observeAll(): Flow<List<Element>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByType(type: ElementType): Flow<List<Element>> =
        dao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    fun observePendingReminders(): Flow<List<Element>> =
        dao.observePendingReminders().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Element? = dao.getById(id)?.toDomain()

    suspend fun save(element: Element): Long = dao.upsert(element.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)
}

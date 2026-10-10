package com.hilvana.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.hilvana.app.domain.model.Element
import com.hilvana.app.domain.model.ElementType
import java.time.Instant

@Entity(tableName = "elements")
data class ElementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val body: String,
    val isDone: Boolean,
    val dueAt: Long?,
    val remindAt: Long?,
    val repeatRule: String?,
    val tags: String,
    val color: Int?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain() = Element(
        id = id,
        type = ElementType.valueOf(type),
        title = title,
        body = body,
        isDone = isDone,
        dueAt = dueAt?.let { Instant.ofEpochMilli(it) },
        remindAt = remindAt?.let { Instant.ofEpochMilli(it) },
        repeatRule = repeatRule,
        tags = if (tags.isBlank()) emptyList() else tags.split(","),
        color = color,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}

fun Element.toEntity() = ElementEntity(
    id = id,
    type = type.name,
    title = title,
    body = body,
    isDone = isDone,
    dueAt = dueAt?.toEpochMilli(),
    remindAt = remindAt?.toEpochMilli(),
    repeatRule = repeatRule,
    tags = tags.joinToString(","),
    color = color,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

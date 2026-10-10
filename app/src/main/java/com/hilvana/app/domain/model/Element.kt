package com.hilvana.app.domain.model

import java.time.Instant

enum class ElementType { NOTE, IDEA, TASK, REMINDER }

data class Element(
    val id: Long = 0,
    val type: ElementType = ElementType.NOTE,
    val title: String = "",
    val body: String = "",
    val isDone: Boolean = false,
    val dueAt: Instant? = null,
    val remindAt: Instant? = null,
    val repeatRule: String? = null,
    val tags: List<String> = emptyList(),
    val color: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

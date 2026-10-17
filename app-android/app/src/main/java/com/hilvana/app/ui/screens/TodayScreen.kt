package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.SectionHeader
import com.hilvana.app.ui.groupForToday

@Composable
fun TodayScreen(
    items: List<Item>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier,
    banner: (@Composable () -> Unit)? = null,
    goalCard: (@Composable () -> Unit)? = null,
) {
    val groups = groupForToday(items)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (banner != null) item { banner() }
        if (goalCard != null) item { goalCard() }

        if (groups.isEmpty) {
            item { EmptyState("Nada pendiente hoy\nToca + o la burbuja para anotar algo.") }
        }
        section("Atrasado", groups.overdue, onOpen, onToggleDone)
        section("Hoy", groups.today, onOpen, onToggleDone)
        section("Próximos 7 días", groups.upcoming, onOpen, onToggleDone)
    }
}

private fun LazyListScope.section(
    title: String,
    items: List<Item>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "header-$title") { SectionHeader(title) }
    items(items, key = { "$title-${it.id}" }) { item ->
        ItemCard(item = item, onClick = { onOpen(item) }, onToggleDone = { onToggleDone(item) })
    }
}

package com.hilvana.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hilvana.app.ui.search.SortOrder

/** Chip "Ordenar: …" que abre la lista de órdenes posibles. */
@Composable
fun SortMenu(
    selected: SortOrder,
    onSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    /** Relevancia solo tiene sentido cuando hay algo buscado. */
    options: List<SortOrder> = SortOrder.entries,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { open = true },
            label = { Text("Ordenar: ${selected.label}") },
            leadingIcon = { ChipIcon(Icons.AutoMirrored.Outlined.Sort) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                    trailingIcon = {
                        if (option == selected) Icon(Icons.Filled.Check, contentDescription = "Elegido")
                    },
                )
            }
        }
    }
}

package com.hilvana.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.CheckItem
import com.hilvana.app.data.progressText
import com.hilvana.app.data.toggled

/** Pasos de una tarea: marcar, quitar y agregar ("Enter" agrega y deja escribir el siguiente). */
@Composable
fun ChecklistEditor(
    steps: List<CheckItem>,
    onChange: (List<CheckItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newStep by rememberSaveable { mutableStateOf("") }
    fun add() {
        val text = newStep.trim()
        if (text.isNotEmpty()) {
            onChange(steps + CheckItem(text))
            newStep = ""
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "Pasos" + if (steps.isNotEmpty()) " · ${steps.progressText}" else "",
            style = MaterialTheme.typography.labelLarge,
        )
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = step.done, onCheckedChange = { onChange(steps.toggled(index)) })
                Text(
                    step.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (step.done) TextDecoration.LineThrough else null,
                    color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onChange(steps.filterIndexed { i, _ -> i != index }) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar paso")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newStep,
                onValueChange = { newStep = it },
                placeholder = { Text("Agregar paso…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = ::add, enabled = newStep.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar paso")
            }
        }
    }
}

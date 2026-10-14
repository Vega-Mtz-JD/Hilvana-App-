package com.hilvana.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.RepeatRule

/** "¿Se repite?": No · Cada día · Lun a vie · Cada semana · Cada mes · Cada año. */
@Composable
fun RepeatSelector(
    repeat: RepeatRule?,
    onChange: (RepeatRule?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("🔁 ¿Se repite?", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = repeat == null, onClick = { onChange(null) }, label = { Text("No") })
            RepeatRule.entries.forEach { rule ->
                FilterChip(selected = repeat == rule, onClick = { onChange(rule) }, label = { Text(rule.label) })
            }
        }
    }
}

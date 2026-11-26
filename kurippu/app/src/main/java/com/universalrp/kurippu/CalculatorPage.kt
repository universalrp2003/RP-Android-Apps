package com.universalrp.kurippu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CalculatorPage(state: CalculatorState, onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(if (state.error) "பூஜ்ஜியத்தால் வகுக்க முடியாது / Cannot divide by zero" else if (state.approximate) "தோராயம் / Approximate" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Text(state.display, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.End, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp).testTag("calcDisplay"))
        listOf(listOf("C", "⌫", "±", "÷"), listOf("7", "8", "9", "−"), listOf("4", "5", "6", "+"), listOf("1", "2", "3", "×"), listOf("0", ".", "=")).forEach { keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.forEach { key -> OutlinedButton(onClick = { onKey(key) }, modifier = Modifier.weight(if (key == "=") 2f else 1f).heightIn(min = 52.dp).testTag("calc-$key"), contentPadding = PaddingValues(2.dp)) { Text(key, style = MaterialTheme.typography.titleMedium) } }
            }
        }
        if (state.history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            state.history.forEachIndexed { index, line ->
                Text(line, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("calcHistory-$index"))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

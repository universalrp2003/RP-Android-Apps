package com.universalrp.animalvoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Observation is a human choice, not a prediction or verified call meaning. Never persisted. */
@Composable
fun ContextGuide(crowExample: Boolean, tamil: Boolean) {
    var observation by remember { mutableStateOf(Observation.NONE) }
    fun tr(ta: String, en: String) = if (tamil) ta else en
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("இது என்ன சொல்லலாம்?", "What might it mean?"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(tr("இந்த ஒலியின் பொருளை AI இன்னும் கண்டறியவில்லை. நீங்கள் நேரில் பார்த்ததைத் தேர்வு செய்யலாம்; அது மொழிபெயர்ப்பு அல்ல.", "The AI has not determined what this call means. You can note what you personally observed; this is not a translation."))
        Text(tr("நீங்கள் பார்த்தது:", "What you saw:"), fontWeight = FontWeight.Bold)
        Observation.entries.forEach { choice ->
            Row(Modifier.fillMaxWidth().selectable(selected = observation == choice, role = Role.RadioButton, onClick = { observation = choice })) {
                RadioButton(selected = observation == choice, onClick = null)
                Text(tr(choice.tamil, choice.english), modifier = Modifier.weight(1f).padding(top = 13.dp))
            }
        }
        Text(tr("இது உங்கள் தேர்வு மட்டுமே; ஒலி அல்லது விலங்கின் நோக்கத்தை உறுதிப்படுத்தாது. சேமிக்கப்படாது.", "This is only your observation, not a sound-based prediction or proof of intent. It is not saved."), style = MaterialTheme.typography.bodySmall)
        if (crowExample) {
            Text(tr("ஆராய்ச்சி உதாரணம்: அமெரிக்கக் காகங்களின் சில குரல்கள் உணவு, ஆபத்து மற்றும் பிற சூழல்களில் ஆய்வு செய்யப்பட்டன. இது இந்தியக் காகத்தின் இந்த ஒலிக்கான பொருள் அல்ல.", "Research example: some American crow calls have been studied in food, threat and other contexts. This does not tell us what this call or an Indian crow means."), style = MaterialTheme.typography.bodyMedium)
            Text("Mates et al., Bioacoustics 2015 · doi:10.1080/09524622.2014.933446", style = MaterialTheme.typography.bodySmall)
        }
    }
}

enum class Observation(val tamil: String, val english: String) {
    NONE("தெரியவில்லை / பார்க்கவில்லை", "Not sure / did not see"),
    FOOD("அருகில் உணவு இருந்தது", "Food was nearby"),
    ANIMAL("அருகில் வேறு விலங்கு இருந்தது", "Another animal was nearby"),
    GROUP("அதே வகை உயிர்கள் அருகில் இருந்தன", "Others of the same kind were nearby")
}

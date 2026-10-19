package dev.cazimir.voicevault.quicknote

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cazimir.voicevault.capture.Haptics
import dev.cazimir.voicevault.ui.theme.VoiceVaultTheme
import dev.cazimir.voicevault.vault.NoteSummary
import dev.cazimir.voicevault.vault.VaultWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Eyes-on counterpart to the eyes-free voice pipeline: a plain text field with no save button.
 * Whatever is typed becomes a new inbox note the moment this screen goes away by any normal
 * means (screen off, home, back, app switch, or tapping outside the input) - mirrors the
 * capture pipeline's "screen-off means done" rule, just without a microphone in the loop.
 * The last five captured notes (voice or quick-text) are shown above the field as context/recall,
 * read-only - this screen never edits past notes, only creates new ones.
 *
 * The screen is disposed of on the way out, and it keeps the display awake while it is up, for
 * the same reasons the capture overlay does - see [onStop] and [onCreate].
 */
class QuickNoteActivity : ComponentActivity() {

    private var text by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
            else -> ""
        }
        enableEdgeToEdge()
        setContent {
            VoiceVaultTheme {
                QuickNoteScreen(
                    text = text,
                    onTextChange = { text = it },
                    onDismiss = ::finish
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        saveIfNonBlank()
        finish()
    }

    private fun saveIfNonBlank() {
        val text = this.text
        if (text.isBlank()) return
        val captureId = VaultWriter.captureIdFor(this)
        VaultWriter.writeNote(this, text, captureId)
        this.text = ""
        Haptics(this).stopped()
    }
}

@Composable
private fun QuickNoteScreen(text: String, onTextChange: (String) -> Unit, onDismiss: () -> Unit) {
    var recentNotes by remember { mutableStateOf(emptyList<NoteSummary>()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val noOpTap = remember { MutableInteractionSource() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
        recentNotes = withContext(Dispatchers.IO) { VaultWriter.recentNotes(context, 5) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202124))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Recent notes",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(recentNotes, key = { it.captureId }) { note -> RecentNoteCard(note, noOpTap) }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clickable(interactionSource = noOpTap, indication = null) {},
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "What's on your mind today?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Quick note…") }
                )
            }
        }
    }
}

@Composable
private fun RecentNoteCard(note: NoteSummary, noOpTap: MutableInteractionSource) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = noOpTap, indication = null) {},
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.captureId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

package com.habitflow.app.ui.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.habitflow.app.data.local.entity.Mood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalEntryScreen(
    habitId: Long,
    viewModel: AddJournalEntryViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(habitId) { viewModel.init(habitId) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) viewModel.addPhotoBytes(bytes)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Journal Entry") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Text("How did it go?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Mood.values().forEach { mood ->
                    val selected = state.mood == mood
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.update { it.copy(mood = mood) } },
                        contentAlignment = Alignment.Center
                    ) { Text(moodEmojiPublic(mood)) }
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.text,
                onValueChange = { t -> viewModel.update { it.copy(text = t) } },
                label = { Text("Write about today...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )

            Spacer(Modifier.height(16.dp))
            Text("Photos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                photoPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo") }
                }
                items(state.photoPaths) { path ->
                    Box(Modifier.size(72.dp)) {
                        AsyncImage(
                            model = java.io.File(path),
                            contentDescription = "Journal photo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removePhoto(path) },
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save(onDone) },
                enabled = state.text.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Save Entry") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun moodEmojiPublic(mood: Mood) = when (mood) {
    Mood.GREAT -> "😄"
    Mood.GOOD -> "🙂"
    Mood.OKAY -> "😐"
    Mood.LOW -> "😕"
    Mood.ROUGH -> "😞"
}

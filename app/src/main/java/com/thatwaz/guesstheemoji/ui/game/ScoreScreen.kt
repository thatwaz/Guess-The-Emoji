package com.thatwaz.guesstheemoji.ui.game

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.data.ScoreEntry
import com.thatwaz.guesstheemoji.data.ScoreStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- UI filter models ---
private enum class ScoreSort { NEWEST, HIGHEST, LOWEST }
private enum class DateFilter(val label: String, val days: Int?) {
    ALL("All", null),
    LAST_7("7d", 7),
    LAST_30("30d", 30)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(
    prefs: Prefs,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val context = LocalContext.current
    val store = rememberScoreStore(prefs)
    val scores by store.scoresFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // dialogs
    var showClearConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ScoreEntry?>(null) }

    // filters
    var sort by remember { mutableStateOf(ScoreSort.NEWEST) }
    var dateFilter by remember { mutableStateOf(DateFilter.ALL) }

    // ✅ list state MUST be passed into LazyColumn
    val listState = rememberLazyListState()

    // Compute list shown
    val filteredSorted = remember(scores, sort, dateFilter) {
        val now = System.currentTimeMillis()
        val byDate = dateFilter.days?.let { days ->
            val cutoff = now - days * 24L * 60L * 60L * 1000L
            scores.filter { it.endedAtEpochMs >= cutoff }
        } ?: scores

        when (sort) {
            ScoreSort.NEWEST -> byDate.sortedByDescending { it.endedAtEpochMs }
            ScoreSort.HIGHEST -> byDate.sortedByDescending { it.score }
            ScoreSort.LOWEST -> byDate.sortedBy { it.score }
        }
    }

    // ✅ scroll to top when sort/date changes OR when the displayed list changes
    LaunchedEffect(sort, dateFilter, filteredSorted.firstOrNull()?.endedAtEpochMs) {
        if (filteredSorted.isNotEmpty()) {
            listState.animateScrollToItem(0)
            // change to animateScrollToItem(0) if you want
        }
    }

    // Clear confirm dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all scores?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    scope.launch { store.clearScores() }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Delete single confirm dialog
    val entryToDelete = pendingDelete
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this score?") },
            text = {
                Text(
                    "Score ${entryToDelete.score} • Tier ${entryToDelete.tier} • Puzzle ${entryToDelete.puzzleNumber}\n" +
                            formatDateTime(entryToDelete.endedAtEpochMs)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { store.deleteScore(entryToDelete) }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Your Scores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (scores.isNotEmpty()) {
                FiltersBar(
                    sort = sort,
                    onSort = { sort = it },
                    dateFilter = dateFilter,
                    onDateFilter = { dateFilter = it }
                )
            }

            if (scores.isEmpty()) {
                Text(
                    text = "No scores yet.\nFinish a run (lose all lives) and it’ll show up here.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,                 // ✅ THIS was missing
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredSorted,
                        key = { it.endedAtEpochMs }
                    ) { entry ->
                        ScoreCard(
                            entry = entry,
                            onShare = { shareScore(context, entry) },
                            onDelete = { pendingDelete = entry }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = scores.isNotEmpty()
                ) { Text("Clear") }

                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.weight(1f)
                ) { Text("Home") }

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Replay")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersBar(
    sort: ScoreSort,
    onSort: (ScoreSort) -> Unit,
    dateFilter: DateFilter,
    onDateFilter: (DateFilter) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null
                )
                Spacer(Modifier.padding(start = 8.dp))
                Text("Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            // Date filter
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DateFilter.entries.forEachIndexed { i, opt ->
                    SegmentedButton(
                        selected = (opt == dateFilter),
                        onClick = { onDateFilter(opt) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = DateFilter.entries.size
                        )
                    ) { Text(opt.label) }
                }
            }

            // Sort
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    "Newest" to ScoreSort.NEWEST,
                    "High" to ScoreSort.HIGHEST,
                    "Low" to ScoreSort.LOWEST
                )
                options.forEachIndexed { i, (label, value) ->
                    SegmentedButton(
                        selected = (value == sort),
                        onClick = { onSort(value) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = options.size
                        )
                    ) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(
    entry: ScoreEntry,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Score: ${entry.score}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurface
                )
                Text(
                    text = "Tier ${entry.tier} • Puzzle ${entry.puzzleNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = formatDateTime(entry.endedAtEpochMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            // Icon actions (share/delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

private fun formatDateTime(epochMs: Long): String {
    val df = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return df.format(Date(epochMs))
}

private fun shareScore(context: Context, entry: ScoreEntry) {
    val playStoreLink =
        "https://play.google.com/store/apps/details?id=com.thatwaz.guesstheemoji"

    val text = buildString {
        append("🧠 I just scored ${entry.score} in Guess The Emoji!\n\n")
        append("Tier: ${entry.tier}\n")
        append("Puzzle reached: ${entry.puzzleNumber}\n\n")
        append("Think you can beat me? 😄\n")
        append("Play here 👉 $playStoreLink")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    context.startActivity(
        Intent.createChooser(intent, "Share your score")
    )
}



@Composable
private fun rememberScoreStore(prefs: Prefs): ScoreStore {
    return remember(prefs) { ScoreStore(prefs) }
}

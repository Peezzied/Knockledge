package group.two.knockledge.ui.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Sentence
import group.two.knockledge.data.model.TranscriptMetadata
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KiwiSoda
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.MutedSand
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.OutlineVariant
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceIndigo
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.VcrOsdMono
import java.util.Locale

/**
 * Tab options for audio playback navigation: Chapter markers and full Transcript.
 */
enum class PlaybackTab {
    CHAPTERS,
    TRANSCRIPT,
}

/**
 * Main audio playback controls component matching Figma Anchor Transcript (node 39:11).
 *
 * Layout hierarchy:
 * 1. Chapter / Transcript tabs
 * 2. Scrollable transcript / chapter list (with plain text inactive rows and highlighted active card)
 * 3. Dedicated 328 × 152dp Original Audio player card at bottom
 */
@Composable
fun AudioPlaybackControls(
    state: AudioPlaybackState,
    chapters: List<ChapterMarker>,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (ChapterMarker) -> Unit,
    modifier: Modifier = Modifier,
    transcript: TranscriptMetadata? = null,
) {
    var selectedTab by remember(chapters, transcript) {
        mutableStateOf(
            if (chapters.isEmpty() && transcript != null && transcript.sentences.isNotEmpty()) {
                PlaybackTab.TRANSCRIPT
            } else {
                PlaybackTab.CHAPTERS
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Active Chapter Header if present
        state.activeChapter?.let { active ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF252146),
                ),
                border = BorderStroke(1.dp, AntiqueBrass),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "NOW PLAYING: ${active.title.uppercase(Locale.US)}",
                        fontFamily = VcrOsdMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 1. Chapter and Transcript Tabs
        PlaybackTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. Tab Content: Chapters or Transcript
        when (selectedTab) {
            PlaybackTab.CHAPTERS -> {
                if (chapters.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chapters.forEach { chapter ->
                            val isActive = state.activeChapter?.startTimeMs == chapter.startTimeMs
                            ChapterItem(
                                chapter = chapter,
                                isActive = isActive,
                                onClick = { onSelectChapter(chapter) },
                            )
                        }
                    }
                } else {
                    EmptyPlaybackStateCard(message = stringResource(R.string.no_chapters))
                }
            }
            PlaybackTab.TRANSCRIPT -> {
                val sentences = transcript?.sentences.orEmpty()
                if (sentences.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        sentences.forEachIndexed { index, sentence ->
                            val isActive = isSentenceActive(
                                sentence = sentence,
                                index = index,
                                sentences = sentences,
                                currentPositionMs = state.currentPositionMs,
                            )
                            TranscriptSentenceItem(
                                sentence = sentence,
                                isActive = isActive,
                                onClick = { onSeekTo(sentence.startTimeMs) },
                            )
                        }
                    }
                } else if (!transcript?.fullText.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceIndigo,
                        ),
                        border = BorderStroke(1.dp, OutlineVariant),
                    ) {
                        Text(
                            text = transcript.fullText,
                            fontFamily = VcrOsdMono,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = AntiqueBrass,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    EmptyPlaybackStateCard(message = stringResource(R.string.no_transcript))
                }
            }
        }

        // 3. Dedicated Original Audio Player at the bottom
        OriginalAudioPlayer(
            state = state,
            chapters = chapters,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Tab bar selector for Chapter and Transcript sections.
 */
@Composable
fun PlaybackTabs(
    selectedTab: PlaybackTab,
    onTabSelected: (PlaybackTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackTabItem(
            title = stringResource(R.string.chapter_list_title),
            isSelected = selectedTab == PlaybackTab.CHAPTERS,
            onClick = { onTabSelected(PlaybackTab.CHAPTERS) },
            modifier = Modifier.weight(1f),
        )
        PlaybackTabItem(
            title = stringResource(R.string.transcript_label),
            isSelected = selectedTab == PlaybackTab.TRANSCRIPT,
            onClick = { onTabSelected(PlaybackTab.TRANSCRIPT) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Individual tab item with 48dp minimum interaction target and brass indicator line.
 */
@Composable
fun PlaybackTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title.uppercase(Locale.US),
                fontFamily = KiwiSoda,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ParchmentCream else MutedSand,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(AntiqueBrass),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OutlineVariant),
                )
            }
        }
    }
}

/**
 * Individual transcript sentence item matching Figma visual specifications:
 * - Inactive: plain text row without card background or border.
 * - Active: highlighted indigo card, brass border, 8dp corners, 10dp padding, cream text (18sp, 26dp line-height).
 * - Speaker and timestamp on one line, left-aligned in brass.
 */
@Composable
fun TranscriptSentenceItem(
    sentence: Sentence,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isActive) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF252146),
            ),
            border = BorderStroke(1.dp, AntiqueBrass),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text(
                        text = sentence.speaker.ifBlank { "SPEAKER" }.uppercase(Locale.US),
                        fontFamily = VcrOsdMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueBrass,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimeMs(sentence.startTimeMs),
                        fontFamily = VcrOsdMono,
                        fontSize = 12.sp,
                        color = AccentGold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sentence.text,
                    fontFamily = VcrOsdMono,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = ParchmentCream,
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = sentence.speaker.ifBlank { "SPEAKER" }.uppercase(Locale.US),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueBrass,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeMs(sentence.startTimeMs),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    color = AntiqueBrass.copy(alpha = 0.8f),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sentence.text,
                fontFamily = VcrOsdMono,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                color = AntiqueBrass,
            )
        }
    }
}

/**
 * Dedicated 328 × 152dp Original Audio player card matching Figma reference.
 */
@Composable
fun OriginalAudioPlayer(
    state: AudioPlaybackState,
    chapters: List<ChapterMarker>,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF252146),
        ),
        border = BorderStroke(1.dp, AccentGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Heading & Current-Time Bubble
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ORIGINAL AUDIO",
                    fontFamily = KiwiSoda,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ParchmentCream,
                    modifier = Modifier.height(24.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = formatTimeMs(state.currentPositionMs),
                            fontFamily = VcrOsdMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextInk,
                        )
                    }
                }
            }

            // Scrubber with Progress and Chapter Tick Marks
            ChapterProgressBar(
                currentPositionMs = state.currentPositionMs,
                durationMs = state.durationMs,
                chapters = chapters,
                onSeekTo = onSeekTo,
            )

            // Current and Total Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTimeMs(state.currentPositionMs),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    color = MutedSand,
                )
                Text(
                    text = formatTimeMs(state.durationMs),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    color = MutedSand,
                )
            }

            // Controls Row: Seek-Back (-10s), Play/Pause, Seek-Forward (+10s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Seek-Back Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceIndigo)
                        .border(BorderStroke(1.dp, AntiqueBrass), RoundedCornerShape(4.dp))
                        .clickable { onSeekTo((state.currentPositionMs - 10000L).coerceAtLeast(0L)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "-10S",
                        fontFamily = VcrOsdMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueBrass,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Centered 48dp Play/Pause Control
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentGold),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextInk,
                            strokeWidth = 2.dp,
                        )
                    } else if (state.isPlaying) {
                        Text(
                            text = "⏸",
                            color = TextInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            text = "▶",
                            color = TextInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Seek-Forward Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceIndigo)
                        .border(BorderStroke(1.dp, AntiqueBrass), RoundedCornerShape(4.dp))
                        .clickable { onSeekTo((state.currentPositionMs + 10000L).coerceAtMost(state.durationMs)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+10S",
                        fontFamily = VcrOsdMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueBrass,
                    )
                }
            }
        }
    }
}

/**
 * Scrubber with progress, thumb, and chapter tick indicators.
 */
@Composable
fun ChapterProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    chapters: List<ChapterMarker>,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationFloat = durationMs.coerceAtLeast(1L).toFloat()
    val progressFloat = currentPositionMs.coerceIn(0L, durationMs).toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val trackHeight = 4.dp.toPx()
            val cy = size.height / 2f
            val trackWidth = size.width
            val progressRatio = (progressFloat / durationFloat).coerceIn(0f, 1f)

            // Background Track
            drawRect(
                color = AntiqueBrass.copy(alpha = 0.4f),
                topLeft = Offset(0f, cy - trackHeight / 2f),
                size = Size(trackWidth, trackHeight),
            )

            // Progress Bar
            drawRect(
                color = AntiqueBrass,
                topLeft = Offset(0f, cy - trackHeight / 2f),
                size = Size(trackWidth * progressRatio, trackHeight),
            )

            // Chapter Tick Marks (width 2dp, height 16dp)
            chapters.forEach { chapter ->
                val tickRatio = (chapter.startTimeMs.toFloat() / durationFloat).coerceIn(0f, 1f)
                val tickX = trackWidth * tickRatio
                drawRect(
                    color = AccentGold,
                    topLeft = Offset(tickX - 1.dp.toPx(), 0f),
                    size = Size(2.dp.toPx(), size.height),
                )
            }
        }

        Slider(
            value = progressFloat,
            onValueChange = { onSeekTo(it.toLong()) },
            valueRange = 0f..durationFloat,
            colors = SliderDefaults.colors(
                thumbColor = AccentGold,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Individual chapter list item styled according to Obsidian & Brass hardware theme.
 */
@Composable
fun ChapterItem(
    chapter: ChapterMarker,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF252146) else SurfaceIndigo,
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) AccentGold else OutlineVariant,
        ),
        elevation = if (isActive) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chapter.title,
                    fontFamily = KiwiSoda,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) ParchmentCream else MutedSand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeMs(chapter.startTimeMs),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    color = AccentGold,
                )
            }

            if (chapter.keyBullets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                chapter.keyBullets.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        fontFamily = VcrOsdMono,
                        fontSize = 12.sp,
                        color = MutedSand,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Graceful placeholder card for empty states.
 */
@Composable
fun EmptyPlaybackStateCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceIndigo),
        border = BorderStroke(1.dp, OutlineVariant),
    ) {
        Text(
            text = message,
            fontFamily = VcrOsdMono,
            fontSize = 14.sp,
            color = MutedSand,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Determines whether a sentence is currently playing based on the playback position in milliseconds.
 */
fun isSentenceActive(
    sentence: Sentence,
    index: Int,
    sentences: List<Sentence>,
    currentPositionMs: Long,
): Boolean {
    val nextStartTime = if (index < sentences.lastIndex) sentences[index + 1].startTimeMs else Long.MAX_VALUE
    val effectiveEnd = if (sentence.endTimeMs > sentence.startTimeMs) sentence.endTimeMs else nextStartTime
    return currentPositionMs >= sentence.startTimeMs && currentPositionMs < effectiveEnd
}

/**
 * Formats milliseconds into mm:ss string format.
 */
fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true, backgroundColor = 0xFF14122B)
@Composable
fun AudioPlaybackControlsChapterTabPreview() {
    val sampleChapters = listOf(
        ChapterMarker(
            startTimeMs = 0L,
            title = "Introduction",
            keyBullets = listOf("Welcome and overview", "Agenda for today"),
        ),
        ChapterMarker(
            startTimeMs = 45000L,
            title = "Project Updates",
            keyBullets = listOf("Milestone 1 completed", "Next steps defined"),
        ),
    )

    KnockledgeTheme(darkTheme = true) {
        Surface(
            color = ObsidianMidnight,
            modifier = Modifier.padding(16.dp),
        ) {
            AudioPlaybackControls(
                state = AudioPlaybackState(
                    currentPositionMs = 15000L,
                    durationMs = 120000L,
                    isPlaying = true,
                    activeChapter = sampleChapters[0],
                ),
                chapters = sampleChapters,
                onTogglePlayPause = {},
                onSeekTo = {},
                onSelectChapter = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14122B)
@Composable
fun AudioPlaybackControlsTranscriptTabPreview() {
    val sampleSentences = listOf(
        Sentence(
            speaker = "Speaker 1",
            startTimeMs = 0L,
            endTimeMs = 14000L,
            text = "Welcome to Knockledge retroactive audio note.",
            confidence = 0.98f,
        ),
        Sentence(
            speaker = "Speaker 2",
            startTimeMs = 14000L,
            endTimeMs = 32000L,
            text = "This transcript highlights the currently active sentence.",
            confidence = 0.95f,
        ),
        Sentence(
            speaker = "Speaker 1",
            startTimeMs = 32000L,
            endTimeMs = 60000L,
            text = "Tap any sentence to seek audio playback immediately.",
            confidence = 0.97f,
        ),
    )

    val sampleTranscript = TranscriptMetadata(
        fullText = "Welcome to Knockledge retroactive audio note. This transcript highlights the currently active sentence. Tap any sentence to seek audio playback immediately.",
        sentences = sampleSentences,
        isDiarized = true,
        detectedLanguage = "en",
        engineType = TranscriptionEngineType.CLOUD_FIREBASE_AI,
    )

    KnockledgeTheme(darkTheme = true) {
        Surface(
            color = ObsidianMidnight,
            modifier = Modifier.padding(16.dp),
        ) {
            AudioPlaybackControls(
                state = AudioPlaybackState(
                    currentPositionMs = 18000L,
                    durationMs = 60000L,
                    isPlaying = true,
                ),
                chapters = emptyList(),
                transcript = sampleTranscript,
                onTogglePlayPause = {},
                onSeekTo = {},
                onSelectChapter = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14122B)
@Composable
fun AudioPlaybackControlsEmptyPreview() {
    KnockledgeTheme(darkTheme = true) {
        Surface(
            color = ObsidianMidnight,
            modifier = Modifier.padding(16.dp),
        ) {
            AudioPlaybackControls(
                state = AudioPlaybackState(
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    isPlaying = false,
                ),
                chapters = emptyList(),
                transcript = null,
                onTogglePlayPause = {},
                onSeekTo = {},
                onSelectChapter = {},
            )
        }
    }
}

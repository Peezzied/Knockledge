package two.knockledge.ui.playback

import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.audio.AudioPlayer
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.ui.playback.AudioPlaybackViewModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlaybackViewModelTest {

    private class FakeAudioPlayer : AudioPlayer {
        private val _state = MutableStateFlow(AudioPlaybackState())
        override val state: StateFlow<AudioPlaybackState> = _state
        val calls = mutableListOf<String>()

        override fun play(file: File, chapters: List<ChapterMarker>) {
            calls += "play"
            _state.value = AudioPlaybackState(isPlaying = true)
        }
        override fun play(url: String, chapters: List<ChapterMarker>) {
            calls += "play"
            _state.value = AudioPlaybackState(isPlaying = true)
        }
        override fun pause() {
            calls += "pause"
            _state.value = _state.value.copy(isPlaying = false)
        }
        override fun resume() {
            calls += "resume"
            _state.value = _state.value.copy(isPlaying = true)
        }
        override fun seekTo(positionMs: Long) { calls += "seek" }
        override fun stop() { calls += "stop" }
        override fun release() { calls += "release" }
    }

    private fun note(id: String) = Note(
        id = id,
        userId = "u1",
        title = "note-$id",
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        audio = AudioMetadata(
            localPath = "/nonexistent/$id.m4a",
            durationMs = 1_000L,
            sampleRate = 16_000,
            triggerType = TriggerType.MANUAL,
        ),
    )

    @Test
    fun playOrToggleOnNewNoteStartsPlayback() {
        val player = FakeAudioPlayer()
        val viewModel = AudioPlaybackViewModel(player)

        viewModel.playOrToggle(note("n1"))

        assertEquals(listOf("play"), player.calls)
    }

    @Test
    fun playOrToggleOnSameNotePausesPlayback() {
        val player = FakeAudioPlayer()
        val viewModel = AudioPlaybackViewModel(player)

        viewModel.playOrToggle(note("n1"))
        viewModel.playOrToggle(note("n1"))

        assertEquals(listOf("play", "pause"), player.calls)
    }

    @Test
    fun playOrToggleOnDifferentNoteSwitchesPlayback() {
        val player = FakeAudioPlayer()
        val viewModel = AudioPlaybackViewModel(player)

        viewModel.playOrToggle(note("n1"))
        viewModel.playOrToggle(note("n2"))

        assertEquals(listOf("play", "play"), player.calls)
    }

    @Test
    fun pausePlaybackPausesPlayer() {
        val player = FakeAudioPlayer()
        val viewModel = AudioPlaybackViewModel(player)

        viewModel.pausePlayback()

        assertEquals(listOf("pause"), player.calls)
    }

    @Test
    fun onClearedStopsPlayer() {
        val player = FakeAudioPlayer()
        val viewModel = AudioPlaybackViewModel(player)

        viewModel.onCleared()

        assertEquals(listOf("stop"), player.calls)
    }
}

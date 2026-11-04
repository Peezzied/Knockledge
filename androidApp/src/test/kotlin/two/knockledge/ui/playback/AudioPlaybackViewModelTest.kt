package two.knockledge.ui.playback

import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.audio.AudioPlayer
import group.two.knockledge.data.model.ChapterMarker
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

        override fun play(file: File, chapters: List<ChapterMarker>) { calls += "play" }
        override fun play(url: String, chapters: List<ChapterMarker>) { calls += "play" }
        override fun pause() { calls += "pause" }
        override fun resume() { calls += "resume" }
        override fun seekTo(positionMs: Long) { calls += "seek" }
        override fun stop() { calls += "stop" }
        override fun release() { calls += "release" }
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

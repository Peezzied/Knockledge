package group.two.knockledge.di

import dagger.Binds
import dagger.Module
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.sensor.AndroidKnockGestureDetector
import group.two.knockledge.sensor.KnockGestureDetector
import javax.inject.Singleton

@Module
abstract class KnockRecordingModule {

    @Binds
    @Singleton
    abstract fun bindGestureDetector(impl: AndroidKnockGestureDetector): KnockGestureDetector

    @Binds
    @Singleton
    abstract fun bindAudioRingBuffer(impl: FileLocalAudioRingBuffer): LocalAudioRingBuffer
}

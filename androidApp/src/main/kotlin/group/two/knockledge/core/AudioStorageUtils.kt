package group.two.knockledge.core

import java.io.File

fun audioBufferDir(cacheDir: File): File = File(cacheDir, "audio_buffer").also { it.mkdirs() }

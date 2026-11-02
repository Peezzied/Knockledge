package group.two.knockledge.core

import java.io.File

/** Cache directory holding the rewound audio chunks, created on first use. */
fun audioBufferDir(cacheDir: File): File = File(cacheDir, "audio_buffer").also { it.mkdirs() }

/** Total bytes of regular files under [dirs], recursing; a missing directory counts as 0. */
fun directorySizeBytes(vararg dirs: File): Long =
    dirs.sumOf { dir ->
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

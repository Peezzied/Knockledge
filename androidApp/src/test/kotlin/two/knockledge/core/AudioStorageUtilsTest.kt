package two.knockledge.core

import group.two.knockledge.core.directorySizeBytes
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioStorageUtilsTest {

    private val dir: File = createTempDirectory("storage_utils").toFile()

    @Test
    fun missingAndEmptyDirsCountAsZero() {
        val missing = File(dir, "does_not_exist")
        val empty = File(dir, "empty").apply { mkdirs() }

        assertEquals(0L, directorySizeBytes(missing))
        assertEquals(0L, directorySizeBytes(empty))
        assertEquals(0L, directorySizeBytes())
    }

    @Test
    fun sumsRegularFilesRecursivelyAcrossDirs() {
        val a = File(dir, "a").apply { mkdirs() }
        File(a, "one.bin").writeBytes(ByteArray(10))
        val nested = File(a, "nested").apply { mkdirs() }
        File(nested, "two.bin").writeBytes(ByteArray(32))

        val b = File(dir, "b").apply { mkdirs() }
        File(b, "three.bin").writeBytes(ByteArray(7))

        assertEquals(49L, directorySizeBytes(a, b))
    }
}

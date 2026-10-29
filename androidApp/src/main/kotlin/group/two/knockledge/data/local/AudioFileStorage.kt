package group.two.knockledge.data.local

import android.content.Context
import java.io.File

object AudioFileStorage {
    fun audioDir(context: Context): File = File(context.filesDir, "audio").apply { mkdirs() }
}

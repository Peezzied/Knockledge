package group.two.knockledge

import android.content.Context
import group.two.knockledge.di.DataGraph
import kotlinx.coroutines.CoroutineScope

/** Release stub: mock data is never seeded outside debug builds. */
object DebugSeeder {
    /** No-op. */
    fun start(context: Context, data: DataGraph, scope: CoroutineScope) = Unit
}

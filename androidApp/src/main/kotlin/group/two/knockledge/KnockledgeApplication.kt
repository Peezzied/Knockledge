package group.two.knockledge

import android.app.Application
import group.two.knockledge.di.DaggerRewinderComponent
import group.two.knockledge.di.RewinderComponent

class KnockledgeApplication : Application() {

    lateinit var component: RewinderComponent
        private set

    /** One graph per process, shared by MainActivity and RewinderService. */
    lateinit var graph: RewinderGraph
        private set

    override fun onCreate() {
        super.onCreate()
        component = DaggerRewinderComponent.factory().create(this)
        graph = component.graph()
    }
}

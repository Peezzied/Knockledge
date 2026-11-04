package two.knockledge

import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import group.two.knockledge.ui.AppTourStateMachine
import group.two.knockledge.ui.AppTourStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTourStateMachineTest {

    private fun machine(userId: String = "user-a"): Pair<AppSettings, AppTourStateMachine> {
        val settings = AppSettings(MemoryStore(), defaultSummaryModel = "qwen2b")
        return settings to AppTourStateMachine(settings, userId)
    }

    @Test
    fun maybeStartActivatesTourOnlyWhenFlagIsFalse() {
        val (settings, tour) = machine()

        tour.maybeStart()
        assertTrue(tour.state.value.active)
        assertEquals(AppTourStep.RECORD_BUTTON, tour.state.value.step)
        tour.skip()

        val second = AppTourStateMachine(settings, "user-a")
        second.maybeStart()
        assertFalse(second.state.value.active)
    }

    @Test
    fun seenFlagIsKeyedPerUser() {
        val (settings, tour) = machine("user-a")
        tour.skip()
        assertTrue(settings.isTourSeen("user-a"))
        assertFalse(settings.isTourSeen("user-b"))

        val other = AppTourStateMachine(settings, "user-b")
        other.maybeStart()
        assertTrue(other.state.value.active)
    }

    @Test
    fun nextAdvancesAndDoneFinishesTour() {
        val (_, tour) = machine()
        tour.start()

        tour.next()
        assertEquals(AppTourStep.RECORDINGS_LIST, tour.state.value.step)
        tour.next()
        assertEquals(AppTourStep.SETTINGS_GEAR, tour.state.value.step)
        tour.next()
        assertEquals(AppTourStep.BUFFER_SLIDER, tour.state.value.step)

        tour.next()
        assertFalse(tour.state.value.active)
    }

    @Test
    fun backMovesToPreviousStepButNotBeforeFirst() {
        val (_, tour) = machine()
        tour.start()

        tour.back()
        assertEquals(AppTourStep.RECORD_BUTTON, tour.state.value.step)

        tour.next()
        tour.next()
        tour.back()
        assertEquals(AppTourStep.RECORDINGS_LIST, tour.state.value.step)
    }

    @Test
    fun skipFinishesTourAndPersistsFlag() {
        val (settings, tour) = machine()
        tour.start()
        tour.next()
        tour.skip()

        assertFalse(tour.state.value.active)
        assertTrue(settings.isTourSeen("user-a"))
    }

    @Test
    fun replayStartsFromFirstStepEvenAfterSeen() {
        val (_, tour) = machine()
        tour.start()
        repeat(4) { tour.next() }
        assertFalse(tour.state.value.active)

        tour.start()
        assertTrue(tour.state.value.active)
        assertEquals(AppTourStep.RECORD_BUTTON, tour.state.value.step)
    }

    @Test
    fun doneSetsCompletedByDoneAndSkipDoesNot() {
        val (_, tour) = machine()
        tour.start()
        repeat(4) { tour.next() }
        assertTrue(tour.state.value.completedByDone)

        tour.start()
        assertFalse(tour.state.value.completedByDone)
        tour.skip()
        assertFalse(tour.state.value.completedByDone)
    }

    @Test
    fun startResumesAtGivenStepForRotationRestore() {
        val (_, tour) = machine()
        tour.start(AppTourStep.BUFFER_SLIDER.ordinal)
        assertTrue(tour.state.value.active)
        assertEquals(AppTourStep.BUFFER_SLIDER, tour.state.value.step)
    }
}

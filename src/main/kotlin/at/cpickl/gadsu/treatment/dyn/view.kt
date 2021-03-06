package at.cpickl.gadsu.treatment.dyn

import at.cpickl.gadsu.persistence.Persistable
import at.cpickl.gadsu.service.LOG
import at.cpickl.gadsu.treatment.Treatment
import at.cpickl.gadsu.treatment.dyn.treats.BloodPressure
import at.cpickl.gadsu.treatment.dyn.treats.BloodPressureRenderer
import at.cpickl.gadsu.treatment.dyn.treats.HaraDiagnosis
import at.cpickl.gadsu.treatment.dyn.treats.HaraDiagnosisRenderer
import at.cpickl.gadsu.treatment.dyn.treats.PulseDiagnosis
import at.cpickl.gadsu.treatment.dyn.treats.PulseDiagnosisRenderer
import at.cpickl.gadsu.treatment.dyn.treats.TongueDiagnosis
import at.cpickl.gadsu.treatment.dyn.treats.TongueDiagnosisRenderer
import at.cpickl.gadsu.view.logic.ChangeAware
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import java.util.HashMap
import javax.swing.JComponent
import javax.swing.JTabbedPane


interface DynTreatmentRenderer {

    companion object {
        val GAP = 8
    }

    /** will be reset after save */
    var originalDynTreatment: DynTreatment
    val view: JComponent

    fun initState(persistable: Persistable) = Unit
    fun readDynTreatment(): DynTreatment

    fun registerOnChange(changeListener: () -> Unit)
    fun isModified(): Boolean {
        return originalDynTreatment != readDynTreatment()
    }

}

@VisibleForTesting
class DynTreatmentTabbedPane(
        private var originalTreatment: Treatment,
        private val bus: EventBus
) : JTabbedPane(), ChangeAware {

    companion object {
        private val INDEX_ADD_BUTTON = 0
    }
    private val log = LOG(javaClass)

    @VisibleForTesting
    var renderers = HashMap<Int, DynTreatmentRenderer>()

    val isAddDynTreatButtonEnabled get() = isEnabledAt(INDEX_ADD_BUTTON)

    private lateinit var lateChangeListener: () -> Unit
    override fun onChange(changeListener: () -> Unit) {
        this.lateChangeListener = changeListener
    }

    fun addDynTreatment(dynTreatment: DynTreatment) {
        val addIndex = calcTabIndex(dynTreatment)
        log.trace("addDynTreatment(dynTreatment) .. calced index: $addIndex")

        val renderer = dynTreatment.call(object : DynTreatmentCallback<DynTreatmentRenderer> {
            override fun onHaraDiagnosis(haraDiagnosis: HaraDiagnosis) = HaraDiagnosisRenderer(haraDiagnosis)
            override fun onTongueDiagnosis(tongueDiagnosis: TongueDiagnosis) = TongueDiagnosisRenderer(tongueDiagnosis, bus)
            override fun onPulseDiagnosis(pulseDiagnosis: PulseDiagnosis) = PulseDiagnosisRenderer(pulseDiagnosis, bus)
            override fun onBloodPressure(bloodPressure: BloodPressure) = BloodPressureRenderer(bloodPressure)
        })

        insertTab(dynTreatment.title, null, renderer.view, null, addIndex)
        selectedIndex = addIndex
        recalcDynTreatmentsIndicesForAddAndAddIt(addIndex, renderer)

        renderer.initState(originalTreatment)
        renderer.registerOnChange(lateChangeListener)
        lateChangeListener()
        if (renderers.size == DynTreatmentFactory.size) {
            addDynTreatButtonEnabled(false)
        }
    }

    fun getDynTreatmentAt(tabIndex: Int): DynTreatment {
        log.trace("getDynTreatmentAt(tabIndex=$tabIndex)")
        return renderers[tabIndex]!!.originalDynTreatment
    }

    fun getAllDynTreatmentClasses(): List<Class<DynTreatment>> {
        return renderers.values.map { it.originalDynTreatment.javaClass }
    }

    fun selectedDynTreatmentType(): Class<DynTreatment>? {
        if (selectedIndex == -1) {
            return null
        }
        return getDynTreatmentAt(selectedIndex).javaClass
    }

    fun trySelectDynTreatment(dynTreatment: Class<DynTreatment>) {
        renderers.values.firstOrNull { it.originalDynTreatment.javaClass == dynTreatment }?.also { renderer ->
            selectedComponent = renderer.view
        }

    }

    fun removeDynTreatmentAt(tabIndex: Int) {
        log.trace("removeDynTreatmentAt(tabIndex=$tabIndex)")
        removeTabAt(tabIndex)
        renderers.remove(tabIndex)
        recalcDynTreatmentsIndicesForDelete(tabIndex)
        lateChangeListener()
        addDynTreatButtonEnabled(true)
    }

    fun readDynTreatments(): List<DynTreatment> {
        return renderers.values.map { it.readDynTreatment() }
    }

    @VisibleForTesting
    fun calcTabIndex(toAdd: DynTreatment): Int {
        var currentIndex = 1
        for (renderer in renderers.values) {
            if (DynTreatmentsFactory.dynTreatmentsFor(toAdd).order <
                    DynTreatmentsFactory.dynTreatmentsFor(renderer.originalDynTreatment).order) {
                break
            }
            currentIndex++
        }
        return currentIndex
    }

    @VisibleForTesting
    fun recalcDynTreatmentsIndicesForAddAndAddIt(addIndex: Int, renderer: DynTreatmentRenderer) {
        val newIndex = HashMap<Int, DynTreatmentRenderer>()
        renderers.entries.forEach { entry ->
            val key = if (entry.key >= addIndex) entry.key + 1 else entry.key
            newIndex.put(key, entry.value)
        }
        newIndex[addIndex] = renderer
        renderers = newIndex
    }

    @VisibleForTesting
    fun recalcDynTreatmentsIndicesForDelete(removedIndex: Int) {
        val newIndex = HashMap<Int, DynTreatmentRenderer>()
        renderers.entries.forEach { entry ->
            val key = if (entry.key > removedIndex) entry.key - 1 else entry.key
            newIndex.put(key, entry.value)
        }
        renderers = newIndex
    }

    fun isModified(): Boolean {
        return originalTreatment.areDynTreatmentsModified(renderers.values) ||
                renderers.values.any { it.isModified() }
    }

    fun wasSaved(newTreatment: Treatment) {
        originalTreatment = newTreatment
        renderers.values.forEach {
            it.originalDynTreatment = newTreatment.dynTreatmentByType(it.originalDynTreatment.javaClass)
        }
    }

    private fun Treatment.dynTreatmentByType(dynTreatment: Class<DynTreatment>): DynTreatment {
        return this.dynTreatments.first { it.javaClass == dynTreatment }
    }

    private fun addDynTreatButtonEnabled(enabled: Boolean) {
        setEnabledAt(INDEX_ADD_BUTTON, enabled)
    }

}

@VisibleForTesting
fun Treatment.areDynTreatmentsModified(renderers: Collection<DynTreatmentRenderer>): Boolean {
    return dynTreatments.map { it.javaClass }.sortedBy { it.name } !=
            renderers.map { it.originalDynTreatment.javaClass }.sortedBy { it.name }
}

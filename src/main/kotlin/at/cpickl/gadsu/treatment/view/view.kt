package at.cpickl.gadsu.treatment.view

import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.development.debugColor
import at.cpickl.gadsu.service.LOG
import at.cpickl.gadsu.service.minutes
import at.cpickl.gadsu.service.toMinutes
import at.cpickl.gadsu.treatment.*
import at.cpickl.gadsu.treatment.dyn.DynTreatment
import at.cpickl.gadsu.view.*
import at.cpickl.gadsu.view.components.gadsuWidth
import at.cpickl.gadsu.view.components.newEventButton
import at.cpickl.gadsu.view.components.newPersistableEventButton
import at.cpickl.gadsu.view.components.panels.GridPanel
import at.cpickl.gadsu.view.components.panels.VFillFormPanel
import at.cpickl.gadsu.view.language.Labels
import at.cpickl.gadsu.view.logic.ModificationAware
import at.cpickl.gadsu.view.logic.ModificationChecker
import at.cpickl.gadsu.view.swing.Pad
import at.cpickl.gadsu.view.swing.enforceWidth
import at.cpickl.gadsu.view.swing.transparent
import at.cpickl.gadsu.view.swing.withFont
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ComparisonChain
import com.google.common.eventbus.EventBus
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.inject.Inject
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

interface TreatmentView : ModificationAware, MainContent {
    fun readTreatment(): Treatment
    fun wasSaved(newTreatment: Treatment)
    fun enablePrev(enable: Boolean)
    fun enableNext(enable: Boolean)
    fun addDynTreatment(dynTreatment: DynTreatment)
    fun removeDynTreatmentAt(tabIndex: Int)
    fun getDynTreatmentAt(tabIndex: Int): DynTreatment
}


class SwingTreatmentView @Inject constructor(
        swing: SwingFactory,
        menuBar: GadsuMenuBar, // this is kind a design hack, but it was quicker to do ;)
        @Assisted private val client: Client,
        @Assisted private var treatment: Treatment
) : GridPanel(
        viewName = ViewNames.Treatment.MainPanel,
        _debugColor = Color.YELLOW
), TreatmentView {

    companion object {
        val DYN_TAB_TITLE_ADD = "+"
    }

    override val type = MainContentType.TREATMENT
    private val log = LoggerFactory.getLogger(javaClass)

    private val btnSave = swing.newPersistableEventButton(ViewNames.Treatment.SaveButton, { TreatmentSaveEvent() }).gadsuWidth()
    private val btnBack = swing.newEventButton(Labels.Buttons.Back, ViewNames.Treatment.BackButton, { TreatmentBackEvent() }).gadsuWidth()

    private val modificationChecker = ModificationChecker(this, btnSave, menuBar.treatmentSave)

    private val fields = Fields<Treatment>(modificationChecker)
    private val inpDateAndTime = fields.newDateAndTimePicker("Datum", treatment.date, { it.date }, ViewNames.Treatment.InputDatePrefix, JTextField.RIGHT)

    private val inpDuration = fields.newMinutesField("Dauer", { it.duration.toMinutes() }, ViewNames.Treatment.InputDuration, 2)
    private val inpAboutDiscomfort = fields.newTextArea("Zustand", { it.aboutDiscomfort }, ViewNames.Treatment.InputAboutDiscomfort)

    private val inpAboutContent = fields.newTextArea("Inhalt", { it.aboutContent }, ViewNames.Treatment.InputAboutContent)
    private val inpAboutDiagnosis = fields.newTextArea("Diagnose", { it.aboutDiagnosis }, ViewNames.Treatment.InputAboutDiagnosis)
    private val inpAboutFeedback = fields.newTextArea("Feedback", { it.aboutFeedback }, ViewNames.Treatment.InputAboutFeedback)
    private val inpAboutHomework = fields.newTextArea("Homework", { it.aboutHomework }, ViewNames.Treatment.InputAboutHomework)
    private val inpAboutUpcoming = fields.newTextArea("Upcoming", { it.aboutUpcoming }, ViewNames.Treatment.InputAboutUpcoming)
    private val inpNote = fields.newTextArea("Sonstige Anmerkungen", { it.note }, ViewNames.Treatment.InputNote)

    private val btnPrev = swing.newEventButton("<<", ViewNames.Treatment.ButtonPrevious, { PreviousTreatmentEvent() }).gadsuWidth()
    private val btnNext = swing.newEventButton(">>", ViewNames.Treatment.ButtonNext, { NextTreatmentEvent() }).gadsuWidth()

    private val subTreatmentView = DynTreatmentTabbedPane()
    private val bus: EventBus

    init {
        bus = swing.bus
        if (treatment.yetPersisted) {
            modificationChecker.disableAll()
        }
        btnSave.changeLabel(treatment)
        fields.updateAll(treatment)

        initComponents()
        prepareSubTreatmentView()
    }

    override fun enablePrev(enable: Boolean) {
        btnPrev.isEnabled = enable
    }

    override fun enableNext(enable: Boolean) {
        btnNext.isEnabled = enable
    }

    override fun addDynTreatment(dynTreatment: DynTreatment) {
        subTreatmentView.addDynTreatment(dynTreatment)
    }

    override fun removeDynTreatmentAt(tabIndex: Int) {
        subTreatmentView.removeDynTreatmentAt(tabIndex)
    }

    override fun getDynTreatmentAt(tabIndex: Int) = subTreatmentView.getDynTreatmentAt(tabIndex)

    private fun initComponents() {
        c.fill = GridBagConstraints.NONE
        c.weightx = 0.0
        c.gridheight = 2
        c.anchor = GridBagConstraints.NORTH
        c.insets = Insets(Pad.DEFAULT_SIZE, 0, 0, Pad.DEFAULT_SIZE) // top right
        add(initClientProfile())

        c.gridx++
        c.gridheight = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        c.anchor = GridBagConstraints.NORTHWEST
        c.insets = Pad.bottom(20)
        add(JLabel("Behandlung #${treatment.number} für ${client.preferredName}").withFont(Font.BOLD, 20))

        c.gridy++
        c.fill = GridBagConstraints.HORIZONTAL
        c.anchor = GridBagConstraints.NORTHWEST
        c.weightx = 0.0
        c.weighty = 0.0
        c.insets = Pad.ZERO
        add(initDetailPanel())

        c.gridx = 0
        c.gridy++
        c.gridwidth = 2
        c.fill = GridBagConstraints.BOTH
        c.gridwidth = 2
        c.weightx = 1.0
        c.weighty = 1.0
        add(initMainPanel())

        c.gridy++
        c.fill = GridBagConstraints.HORIZONTAL
        c.weighty = 0.0
        add(initButtonPanel())
    }

    private fun initMainPanel(): Component {
        val panel = GridPanel()
        with(panel.c) {
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            weighty = 1.0
            panel.add(initTextAreas())

            // TODO width still does not work properly
            gridx++
            subTreatmentView.enforceWidth(400)
            panel.add(subTreatmentView)
        }
        return panel
    }

    private fun prepareSubTreatmentView() {
        subTreatmentView.addTab(DYN_TAB_TITLE_ADD, JLabel("Füge einen neuen Behandlungsteil hinzu."))
//        val uiClass = subTreatmentView.ui.javaClass
        // AquaTabbedPaneContrastUI
        subTreatmentView.ui = object : BasicTabbedPaneUI() {
            override fun createMouseListener(): MouseListener {
                return object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        val index = subTreatmentView.ui.tabForCoordinate(subTreatmentView, e.x, e.y)
                        if (index == -1) {
                            return
                        }
                        val title = subTreatmentView.getTitleAt(index)

                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (title == DYN_TAB_TITLE_ADD) {
                                val tabBounds = subTreatmentView.getBoundsAt(index)
                                bus.post(DynTreatmentRequestAddEvent(PopupSpec(subTreatmentView, tabBounds.x, tabBounds.y + tabBounds.height)))
                            } else {
                                if (subTreatmentView.selectedIndex != index) {
                                    subTreatmentView.selectedIndex = index
                                } else if (subTreatmentView.isRequestFocusEnabled) {
                                    subTreatmentView.requestFocusInWindow()
                                }
                            }
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            if (title == DYN_TAB_TITLE_ADD) {
                                return
                            }
                            val popup = JPopupMenu()
                            val closeItem = JMenuItem("Löschen")
                            closeItem.addActionListener {
                                bus.post(DynTreatmentRequestDeleteEvent(index))
                            }
                            popup.add(closeItem)
                            val tabBounds = subTreatmentView.getBoundsAt(index)
                            popup.show(subTreatmentView, tabBounds.x, tabBounds.y + tabBounds.height)
                        }
                    }
                }
            }
        }
    }

    private fun initDetailPanel(): Component {
        val panel = GridPanel()
        with(panel.c) {
            fill = GridBagConstraints.HORIZONTAL
            panel.add(JLabel("Am "))
            gridx++
            panel.add(inpDateAndTime.toComponent())
            gridx++
            panel.add(JLabel(" und dauerte "))
            gridx++
            panel.add(inpDuration)
            gridx++
            panel.add(JLabel(" Minuten."))
            gridx++
            weightx = 1.0 // layout hack ;)
            panel.add(JPanel())
        }
        return panel
    }

    private fun initClientProfile(): Component {
        val panel = GridPanel()
        with(panel.c) {
            fill = GridBagConstraints.BOTH
            panel.add(JLabel(client.picture.toViewMedRepresentation()))
        }
        return panel
    }

    private fun initButtonPanel(): Component {
        val panel = JPanel(BorderLayout())
        panel.transparent()
        panel.debugColor = Color.ORANGE

        panel.add(JPanel().apply {
            transparent()
            add(btnSave)
            add(btnBack)

        }, BorderLayout.WEST)
        panel.add(JPanel().apply {
            transparent()
            add(btnPrev)
            add(btnNext)
        }, BorderLayout.EAST)

        return panel
    }

    private fun initTextAreas() = VFillFormPanel().apply {
        addFormInput(inpAboutDiscomfort)
        addFormInput(inpAboutContent)
        addFormInput(inpAboutDiagnosis)
        addFormInput(inpAboutFeedback)
        addFormInput(inpAboutHomework)
        addFormInput(inpAboutUpcoming)
        addFormInput(inpNote)
    }

    override fun isModified(): Boolean {
        if (fields.isAnyModified(treatment)) {
            return true
        }
        // additional checks not handled by fields instance
        return ComparisonChain.start()
                .compare(treatment.date, inpDateAndTime.selectedDate) // watch out for nulls!
                .result() != 0
    }

    override fun wasSaved(newTreatment: Treatment) {
        log.trace("wasSaved(newTreatment)")
        treatment = newTreatment

        fields.updateAll(newTreatment)
        btnSave.changeLabel(treatment)
        modificationChecker.trigger()
    }

    override fun closePreparations() {
        inpDateAndTime.hidePopup()
    }

    override fun asComponent() = this

    override fun readTreatment(): Treatment {
        log.trace("readTreatment()")
        // use full-init constructor (not copy method!) so to be aware of changes
        return Treatment(
                treatment.id,
                treatment.clientId,
                treatment.created,
                treatment.number,
                inpDateAndTime.selectedDate,
                minutes(inpDuration.numberValue),
                inpAboutDiscomfort.text,
                inpAboutDiagnosis.text,
                inpAboutContent.text,
                inpAboutFeedback.text,
                inpAboutHomework.text,
                inpAboutUpcoming.text,
                inpNote.text,
                readDynTreatments()
        )
    }

    private fun readDynTreatments(): MutableList<DynTreatment> {
        // FIXME #17 implement me
        println("FIXME implement treatment view readDynTreats()!!!")
        return mutableListOf()
    }

    override fun toString(): String {
        return "SwingTreatmentView(type=$type)"
    }

}

data class PopupSpec(val component: Component, val x: Int, val y: Int)

@VisibleForTesting class DynTreatmentTabbedPane : JTabbedPane() {
    private val log = LOG(javaClass)
    @VisibleForTesting var index = HashMap<Int, DynTreatment>()

    fun addDynTreatment(dynTreatment: DynTreatment) {
        val addIndex = calcTabIndex(dynTreatment)
        log.trace("addDynTreatment(dynTreatment) .. calced index: $addIndex")
        val tabContent = JLabel("foobar ${dynTreatment.title}") // FIXME custom renderer for each dyn treatment
        insertTab(dynTreatment.title, null, tabContent, null, addIndex)
        recalcDynTreatmentsIndicesForAdd(addIndex, dynTreatment)
    }

    fun getDynTreatmentAt(tabIndex: Int): DynTreatment {
        log.trace("getDynTreatmentAt(tabIndex=$tabIndex)")
        return index[tabIndex]!!
    }

    fun removeDynTreatmentAt(tabIndex: Int) {
        log.trace("removeDynTreatmentAt(tabIndex=$tabIndex)")
        removeTabAt(tabIndex)
        index.remove(tabIndex)
        recalcDynTreatmentsIndicesForDelete(tabIndex)
    }

    @VisibleForTesting fun calcTabIndex(toAdd: DynTreatment): Int {
        var currentIndex = 1
        for (dyn in index.values) {
            if (toAdd.tabLocationWeight < dyn.tabLocationWeight) {
                break
            }
            currentIndex++
        }
        return currentIndex
    }

    @VisibleForTesting fun recalcDynTreatmentsIndicesForAdd(addIndex: Int, dynTreatment: DynTreatment) {
        val newIndex = HashMap<Int, DynTreatment>()
        index.entries.forEach { entry ->
            val key = if (entry.key >= addIndex) entry.key + 1 else entry.key
            newIndex.put(key, entry.value)
        }
        newIndex.put(addIndex, dynTreatment)
        index = newIndex
    }

    @VisibleForTesting fun recalcDynTreatmentsIndicesForDelete(removedIndex: Int) {
        val newIndex = HashMap<Int, DynTreatment>()
        index.entries.forEach { entry ->
            val key = if (entry.key > removedIndex) entry.key - 1 else entry.key
            newIndex.put(key, entry.value)
        }
        index = newIndex
    }
}

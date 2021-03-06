package at.cpickl.gadsu.preferences.view

import at.cpickl.gadsu.preferences.PreferencesData
import at.cpickl.gadsu.preferences.PreferencesWindowClosedEvent
import at.cpickl.gadsu.preferences.ThresholdPrefData
import at.cpickl.gadsu.service.GapiCredentials
import at.cpickl.gadsu.view.MainFrame
import at.cpickl.gadsu.view.SwingFactory
import at.cpickl.gadsu.view.ViewNames
import at.cpickl.gadsu.view.addKTabs
import at.cpickl.gadsu.view.components.EventButton
import at.cpickl.gadsu.view.components.MyFrame
import at.cpickl.gadsu.view.swing.ClosableWindow
import at.cpickl.gadsu.view.swing.addCloseListener
import at.cpickl.gadsu.view.swing.registerCloseOnEscapeOrShortcutW
import at.cpickl.gadsu.view.swing.transparent
import com.github.christophpickl.kpotpourri.common.string.nullIfEmpty
import com.google.common.eventbus.EventBus
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import javax.inject.Inject
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane

val HGAP_FROM_WINDOW = 15

interface WritablePreferencesWindow {
    var txtApplicationDirectory: String set
    var txtLatestBackup: String set
    val btnCheckUpdate: EventButton get
}

interface PreferencesWindow : ClosableWindow, WritablePreferencesWindow {
    fun start()
    fun initData(preferencesData: PreferencesData)
    fun readData(): PreferencesData
}

class PreferencesSwingWindow @Inject constructor(
        private val mainFrame: MainFrame,
        private val bus: EventBus,
        swing: SwingFactory
) : MyFrame("Einstellungen"), PreferencesWindow {

    private val tabbedPane = JTabbedPane(JTabbedPane.NORTH, JTabbedPane.SCROLL_TAB_LAYOUT).transparent()
    private val tabGeneral = PrefsTabGeneral(swing)
    private val tabConnectivity = PrefsTabConnectivity()
    private val allTabs: List<PrefsTab> = listOf(tabGeneral, tabConnectivity)

    private val log = LoggerFactory.getLogger(javaClass)
    private var yetCreated: Boolean = false

    override var txtApplicationDirectory: String = ""
        set(value) {
            tabGeneral.inpApplicationDirectory.text = value
        }
    override var txtLatestBackup: String = ""
        set(value) {
            tabGeneral.inpLatestBackup.text = value
        }
    override val btnCheckUpdate: EventButton = tabGeneral.btnCheckUpdate

    init {
        registerCloseOnEscapeOrShortcutW()

        name = ViewNames.Preferences.Window
        addCloseListener { doClose(false) }

        tabbedPane.addKTabs(allTabs)

        val btnClose = JButton("Speichern und schlie\u00dfen")
        btnClose.addActionListener { doClose(true) }
        rootPane.defaultButton = btnClose

        val panelSouth = JPanel().apply {
            layout = BorderLayout()
            border = BorderFactory.createEmptyBorder(15, HGAP_FROM_WINDOW, 15, HGAP_FROM_WINDOW)
            transparent()
            add(btnClose, BorderLayout.EAST)
        }

        contentPane.layout = BorderLayout()
        contentPane.add(tabbedPane, BorderLayout.CENTER)
        contentPane.add(panelSouth, BorderLayout.SOUTH)
    }

    private fun doClose(persistData: Boolean) {
        isVisible = false
        bus.post(PreferencesWindowClosedEvent(persistData))
    }

    override fun initData(preferencesData: PreferencesData) {
        log.trace("initData(preferencesData={})", preferencesData)
        tabGeneral.initData(preferencesData)
        tabConnectivity.initData(preferencesData)
    }

    override fun readData(): PreferencesData {
        return PreferencesData(
                username = tabGeneral.inpUsername.text,
                checkUpdates = tabGeneral.inpCheckUpdates.isSelected,
                treatmentGoal = if (tabGeneral.inpTreatmentGoal.numberValue <= 0) null else tabGeneral.inpTreatmentGoal.numberValue,
                threshold = ThresholdPrefData(
                        daysAttention = tabGeneral.inpThresholdAttention.numberValue,
                        daysWarn = tabGeneral.inpThresholdWarn.numberValue,
                        daysFatal = tabGeneral.inpThresholdFatal.numberValue
                ),
                proxy = tabConnectivity.inpProxy.text.nullIfEmpty(),
                gcalName = tabConnectivity.inpGcalName.text.nullIfEmpty(),
                gmailAddress = tabConnectivity.inpGmailAddress.text.nullIfEmpty(),
                gapiCredentials = GapiCredentials.buildNullSafe(tabConnectivity.inpGapiClientId.text.nullIfEmpty(), tabConnectivity.inpGapiClientSecret.text.nullIfEmpty()),
                templateConfirmSubject = tabConnectivity.inpConfirmMailSubject.text.nullIfEmpty(),
                templateConfirmBody = tabConnectivity.inpConfirmMailBody.text.nullIfEmpty()
        )
    }

    override fun start() {
        if (!yetCreated) {
            yetCreated = true
            pack()
            setLocationRelativeTo(mainFrame.asJFrame())
        }
        if (!isVisible) {
            log.trace("Setting preferencies window visible.")
            isVisible = true
        } else {
            requestFocus()
        }
    }

    override fun closeWindow() {
        hideAndClose()
    }
}

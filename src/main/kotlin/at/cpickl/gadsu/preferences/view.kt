package at.cpickl.gadsu.preferences

import at.cpickl.gadsu.client.xprops.view.GridBagFill
import at.cpickl.gadsu.service.GapiCredentials
import at.cpickl.gadsu.service.nullIfEmpty
import at.cpickl.gadsu.version.CheckForUpdatesEvent
import at.cpickl.gadsu.view.MainFrame
import at.cpickl.gadsu.view.SwingFactory
import at.cpickl.gadsu.view.ViewNames
import at.cpickl.gadsu.view.components.EventButton
import at.cpickl.gadsu.view.components.MyFrame
import at.cpickl.gadsu.view.components.MyTextArea
import at.cpickl.gadsu.view.components.inputs.HtmlEditorPane
import at.cpickl.gadsu.view.components.inputs.NumberField
import at.cpickl.gadsu.view.components.newEventButton
import at.cpickl.gadsu.view.components.panels.FormPanel
import at.cpickl.gadsu.view.swing.ClosableWindow
import at.cpickl.gadsu.view.swing.addCloseListener
import at.cpickl.gadsu.view.swing.disableFocusable
import at.cpickl.gadsu.view.swing.disabled
import at.cpickl.gadsu.view.swing.enforceWidth
import at.cpickl.gadsu.view.swing.leftAligned
import at.cpickl.gadsu.view.swing.opaque
import at.cpickl.gadsu.view.swing.registerCloseOnEscape
import at.cpickl.gadsu.view.swing.scrolled
import at.cpickl.gadsu.view.swing.selectAllOnFocus
import at.cpickl.gadsu.view.swing.transparent
import at.cpickl.gadsu.view.swing.viewName
import com.google.common.eventbus.EventBus
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import javax.inject.Inject
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextField

private val HGAP_FROM_WINDOW = 15

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

private abstract class PrefsTab(val tabTitle: String) {

    protected val VGAP_BETWEEN_COMPONENTS = 10

    abstract fun asComponent(): JComponent
}

private class PrefsTabGeneral(swing: SwingFactory) : PrefsTab("Allgemein") {

    val inpUsername = JTextField().viewName { Preferences.InputUsername }
    val inpCheckUpdates = JCheckBox("Beim Start prüfen")
    val inpTreatmentGoal = NumberField(4).selectAllOnFocus().leftAligned()

    val inpApplicationDirectory = JTextField().disabled().disableFocusable()
    val inpLatestBackup = JTextField().disabled().disableFocusable()

    val btnCheckUpdate = swing.newEventButton("Jetzt prüfen", "", { CheckForUpdatesEvent() })

    override fun asComponent() = FormPanel(labelAnchor = GridBagConstraints.NORTHWEST).apply {
        border = BorderFactory.createEmptyBorder(10, HGAP_FROM_WINDOW, 0, HGAP_FROM_WINDOW)

        addDescriptiveFormInput("Dein Name", inpUsername, "Dein vollständiger Name wird unter anderem<br/>auf Rechnungen und Berichte (Protokolle) angezeigt.")
        addDescriptiveFormInput("Auto Update", initPanelCheckUpdates(), "Um immer am aktuellsten Stand zu bleiben,<br/>empfiehlt es sich diese Option zu aktivieren.",
                GridBagFill.None, addTopInset = VGAP_BETWEEN_COMPONENTS)
        addDescriptiveFormInput("Behandlungsziel*", inpTreatmentGoal, "Setze dir ein Ziel wieviele (unprotokollierte) Behandlungen du schaffen m\u00f6chtest.")

        addDescriptiveFormInput("Programm Ordner", inpApplicationDirectory, "Hier werden die progamm-internen Daten gespeichert.",
                addTopInset = VGAP_BETWEEN_COMPONENTS)
        addDescriptiveFormInput("Letztes Backup", inpLatestBackup, "Gadsu erstellt für dich täglich ein Backup aller Informationen.",
                addTopInset = VGAP_BETWEEN_COMPONENTS)

        c.gridwidth = 2
        add(HtmlEditorPane("<b>*</b> ... <i>Neustart erforderlich</i>").disableFocusable())
        addLastColumnsFilled()
    }

    private fun initPanelCheckUpdates() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        transparent()

        add(inpCheckUpdates)
        add(btnCheckUpdate)
    }

}

private class PrefsTabConnectivity : PrefsTab("Connectivity") {

    val inpProxy = JTextField()
    val inpGcalName = JTextField()
    val inpGmailAddress = JTextField()
    val inpGapiClientId = JTextField()
    val inpGapiClientSecret = JTextField()
    val inpConfirmMailSubject = JTextField()
    val inpConfirmMailBody = MyTextArea("", visibleRows = 6)

    override fun asComponent() = FormPanel(
            labelAnchor = GridBagConstraints.NORTHWEST,
            inputAnchor = GridBagConstraints.NORTHWEST).apply {
        border = BorderFactory.createEmptyBorder(10, HGAP_FROM_WINDOW, 0, HGAP_FROM_WINDOW)

        addDescriptiveFormInput("HTTP Proxy*", inpProxy, "Falls du \u00fcber einen Proxy ins Internet gelangst,<br/>dann konfiguriere diesen bitte hier. (z.B.: <tt>proxy.heim.at:8080</tt>)")
        addDescriptiveFormInput("Google Calendar*", inpGcalName, "Trage hier den Kalendernamen ein um die Google Integration einzuschalten.")
        addDescriptiveFormInput("GMail Addresse", inpGmailAddress, "Trage hier deine GMail Adresse ein für das Versenden von E-Mails.")
        addDescriptiveFormInput("Google API ID", inpGapiClientId, "Um die Google API nutzen zu können, brauchst du eine Zugangs-ID.<br/>" +
                "Credentials sind erstellbar in der Google API Console.<br/>" +
                "Bsp.: <tt>123456789012-aaaabbbbccccddddeeeefffffaaaabb.apps.googleusercontent.com</tt>")
        addDescriptiveFormInput("Google API Secret", inpGapiClientSecret, "Das zugehörige Passwort.<br/>" +
                "Bsp.: <tt>AABBCCDDDaabbccdd12345678</tt>")
        addDescriptiveFormInput("Mail Subject", inpConfirmMailSubject, "Bestätigungsmail Vorlage welche die selben Variablen nutzen kann wie der Mail Body.")
        // for available variables see: AppointmentConfirmationerImpl
        addDescriptiveFormInput("Mail Body", inpConfirmMailBody.scrolled(), "Bestätigungsmail Vorlage. Mögliche Variablen: \${name}, \${dateStart?datetime}, \${dateEnd?datetime}.")
        addLastColumnsFilled()
    }

}

class PreferencesSwingWindow @Inject constructor(
        private val mainFrame: MainFrame,
        private val bus: EventBus,
        swing: SwingFactory
) : MyFrame("Einstellungen"), PreferencesWindow {

    private val tabbedPane = JTabbedPane(JTabbedPane.NORTH, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val tabGeneral = PrefsTabGeneral(swing)
    private val tabConnectivity = PrefsTabConnectivity()
    private val allTabs: Array<PrefsTab> = arrayOf(tabGeneral, tabConnectivity)

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
        registerCloseOnEscape()

        name = ViewNames.Preferences.Window
        addCloseListener { doClose(false) }

        initTabbedPane()

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

    private fun initTabbedPane() {
        tabbedPane.transparent()
        // MINOR UI reuse logic from client detail tab

        var i: Int = 0
        allTabs.forEach { tab ->
            val tabContent: Component = JScrollPane(tab.asComponent().opaque()).transparent()
            tabbedPane.addTab("<html><body><table width='100'><span style='align:center'>${tab.tabTitle}</span></table></body></html>", tabContent)
            tabbedPane.setTabComponentAt(i++, JLabel(tab.tabTitle, JLabel.CENTER).enforceWidth(100))
        }
    }

    private fun doClose(persistData: Boolean) {
        isVisible = false
        bus.post(PreferencesWindowClosedEvent(persistData))
    }

    override fun initData(preferencesData: PreferencesData) {
        log.trace("initData(preferencesData={})", preferencesData)
        tabGeneral.inpUsername.text = preferencesData.username
        tabGeneral.inpCheckUpdates.isSelected = preferencesData.checkUpdates
        tabGeneral.inpTreatmentGoal.numberValue = preferencesData.treatmentGoal ?: 0

        tabConnectivity.inpProxy.text = preferencesData.proxy ?: ""
        tabConnectivity.inpGcalName.text = preferencesData.gcalName ?: ""
        tabConnectivity.inpGmailAddress.text = preferencesData.gmailAddress ?: ""
        tabConnectivity.inpGapiClientId.text = preferencesData.gapiCredentials?.clientId
        tabConnectivity.inpGapiClientSecret.text = preferencesData.gapiCredentials?.clientSecret
        tabConnectivity.inpConfirmMailSubject.text = preferencesData.templateConfirmSubject ?: ""
        tabConnectivity.inpConfirmMailBody.text = preferencesData.templateConfirmBody ?: ""
    }

    override fun readData(): PreferencesData {
        return PreferencesData(
                username = tabGeneral.inpUsername.text,
                checkUpdates = tabGeneral.inpCheckUpdates.isSelected,
                treatmentGoal = if (tabGeneral.inpTreatmentGoal.numberValue <= 0) null else tabGeneral.inpTreatmentGoal.numberValue,

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

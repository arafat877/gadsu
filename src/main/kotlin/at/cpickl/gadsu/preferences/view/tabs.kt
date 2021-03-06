package at.cpickl.gadsu.preferences.view

import at.cpickl.gadsu.client.view.ThresholdResult
import at.cpickl.gadsu.client.xprops.view.GridBagFill
import at.cpickl.gadsu.color
import at.cpickl.gadsu.mail.AppointmentConfirmationTemplateDeclaration
import at.cpickl.gadsu.preferences.PreferencesData
import at.cpickl.gadsu.version.CheckForUpdatesEvent
import at.cpickl.gadsu.view.KTab
import at.cpickl.gadsu.view.SwingFactory
import at.cpickl.gadsu.view.components.MyTextArea
import at.cpickl.gadsu.view.components.inputs.HtmlEditorPane
import at.cpickl.gadsu.view.components.inputs.NumberField
import at.cpickl.gadsu.view.components.newEventButton
import at.cpickl.gadsu.view.components.panels.FormPanel
import at.cpickl.gadsu.view.swing.disableFocusable
import at.cpickl.gadsu.view.swing.disabled
import at.cpickl.gadsu.view.swing.leftAligned
import at.cpickl.gadsu.view.swing.scrolled
import at.cpickl.gadsu.view.swing.selectAllOnFocus
import at.cpickl.gadsu.view.swing.transparent
import at.cpickl.gadsu.view.swing.viewName
import java.awt.GridBagConstraints
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField


abstract class PrefsTab(
        override val tabTitle: String,
        override val scrolled: Boolean = true
) : KTab {

    protected val VGAP_BETWEEN_COMPONENTS = 10

}

class PrefsTabGeneral(swing: SwingFactory) : PrefsTab("Allgemein") {

    val inpUsername = JTextField().viewName { Preferences.InputUsername }
    val inpCheckUpdates = JCheckBox("Beim Start prüfen")
    val inpTreatmentGoal = NumberField(4).selectAllOnFocus().leftAligned()

    val inpApplicationDirectory = JTextField().disabled().disableFocusable()
    val inpLatestBackup = JTextField().disabled().disableFocusable()
    val inpThresholdAttention = NumberField(4).selectAllOnFocus().leftAligned().viewName { Preferences.InputThresholdAttention }
    val inpThresholdWarn = NumberField(4).selectAllOnFocus().leftAligned().viewName { Preferences.InputThresholdWarn }
    val inpThresholdFatal = NumberField(4).selectAllOnFocus().leftAligned().viewName { Preferences.InputThresholdFatal }


    val btnCheckUpdate = swing.newEventButton("Jetzt prüfen", "", { CheckForUpdatesEvent() })

    override fun asComponent() = FormPanel(fillCellsGridy = false, labelAnchor = GridBagConstraints.NORTHWEST).apply {
        border = BorderFactory.createEmptyBorder(10, HGAP_FROM_WINDOW, 0, HGAP_FROM_WINDOW)

        addDescriptiveFormInput("Dein Name", inpUsername, "Dein vollständiger Name wird unter anderem<br/>auf Rechnungen und Berichte (Protokolle) angezeigt.")

        addDescriptiveFormInput("Auto Update", initPanelCheckUpdates(), "Um immer am aktuellsten Stand zu bleiben,<br/>empfiehlt es sich diese Option zu aktivieren.",
                GridBagFill.None, addTopInset = VGAP_BETWEEN_COMPONENTS)

        addDescriptiveFormInput("Behandlungsziel*", inpTreatmentGoal, "Setze dir ein Ziel wieviele (unprotokollierte) Behandlungen du schaffen m\u00f6chtest.")

        addDescriptiveFormInput("Einfärbungsgrenzen", initPanelThreshold(), "Anzahl der Tage zur Einfärbung der Farbleiste für Klienten.",
                GridBagFill.None, addTopInset = VGAP_BETWEEN_COMPONENTS)

        addDescriptiveFormInput("Programm Ordner", inpApplicationDirectory, "Hier werden die progamm-internen Daten gespeichert.",
                addTopInset = VGAP_BETWEEN_COMPONENTS)

        addDescriptiveFormInput("Letztes Backup", inpLatestBackup, "Gadsu erstellt für dich täglich ein Backup aller Informationen.",
                addTopInset = VGAP_BETWEEN_COMPONENTS)

        c.gridwidth = 2
        add(HtmlEditorPane("<b>*</b> ... <i>Neustart erforderlich</i>").disableFocusable())
        addLastColumnsFilled()
    }

    private fun initPanelThreshold() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        transparent()


        add(JLabel("${ThresholdResult.Attention.label}: ").color(ThresholdResult.Attention.color))
        add(inpThresholdAttention)
        add(JLabel("${ThresholdResult.Warn.label}: ").color(ThresholdResult.Warn.color))
        add(inpThresholdWarn)
        add(JLabel("${ThresholdResult.Fatal.label}: ").color(ThresholdResult.Fatal.color))
        add(inpThresholdFatal)
    }

    private fun initPanelCheckUpdates() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        transparent()

        add(inpCheckUpdates)
        add(btnCheckUpdate)
    }

    fun initData(preferencesData: PreferencesData) {
        inpUsername.text = preferencesData.username
        inpCheckUpdates.isSelected = preferencesData.checkUpdates
        inpTreatmentGoal.numberValue = preferencesData.treatmentGoal ?: 0
        inpThresholdAttention.numberValue = preferencesData.threshold.daysAttention
        inpThresholdWarn.numberValue = preferencesData.threshold.daysWarn
        inpThresholdFatal.numberValue = preferencesData.threshold.daysFatal
    }

}

class PrefsTabConnectivity : PrefsTab("Netzwerk") {

    val inpProxy = JTextField()
    val inpGcalName = JTextField()
    val inpGmailAddress = JTextField()
    val inpGapiClientId = JTextField()
    val inpGapiClientSecret = JTextField()
    val inpConfirmMailSubject = JTextField().apply { toolTipText = AppointmentConfirmationTemplateDeclaration.toolTipText }
    val inpConfirmMailBody = MyTextArea("", visibleRows = 6).apply { toolTipText = AppointmentConfirmationTemplateDeclaration.toolTipText }

    override fun asComponent() = FormPanel(
            fillCellsGridy = false,
            labelAnchor = GridBagConstraints.NORTHWEST,
            inputAnchor = GridBagConstraints.NORTHWEST).apply {
        border = BorderFactory.createEmptyBorder(10, HGAP_FROM_WINDOW, 0, HGAP_FROM_WINDOW)

        addDescriptiveFormInput("HTTP Proxy*", inpProxy, "Falls du \u00fcber einen Proxy ins Internet gelangst, dann konfiguriere diesen bitte hier.,br/>Z.B.: <tt>proxy.heim.at:8080</tt>")
        addDescriptiveFormInput("Google Calendar*", inpGcalName, "Trage hier den Kalendernamen ein um die Google Integration einzuschalten.")
        addDescriptiveFormInput("GMail Addresse", inpGmailAddress, "Trage hier deine GMail Adresse ein für das Versenden von E-Mails.")
        addDescriptiveFormInput("Google API ID", inpGapiClientId, "Um die Google API nutzen zu können, brauchst du eine Zugangs-ID.<br/>" +
                "Credentials sind erstellbar in der Google API Console.<br/>" +
                "Bsp.: <tt>123456789012-aaaabbbbccccddddeeeefffffaaaabb.apps.googleusercontent.com</tt>")
        addDescriptiveFormInput("Google API Secret", inpGapiClientSecret, "Das zugehörige Passwort.<br/>" +
                "Bsp.: <tt>AABBCCDDDaabbccdd12345678</tt>")
        addDescriptiveFormInput("Mail Betreff", inpConfirmMailSubject, "Bestätigungsmail Freemarker Template für den Mail Betreff.")
        // for available variables see: AppointmentConfirmationerImpl
        addDescriptiveFormInput(
                label = "Mail Text",
                input = inpConfirmMailBody.scrolled(),
                description = "Bestätigungsmail Freemarker Template für den Mail Inhalt.",
                fillType = GridBagFill.Both,
                inputWeighty = 1.0
        )
    }

    fun initData(preferencesData: PreferencesData) {
        inpProxy.text = preferencesData.proxy ?: ""
        inpGcalName.text = preferencesData.gcalName ?: ""
        inpGmailAddress.text = preferencesData.gmailAddress ?: ""
        inpGapiClientId.text = preferencesData.gapiCredentials?.clientId
        inpGapiClientSecret.text = preferencesData.gapiCredentials?.clientSecret
        inpConfirmMailSubject.text = preferencesData.templateConfirmSubject ?: ""
        inpConfirmMailBody.text = preferencesData.templateConfirmBody ?: ""
    }

}

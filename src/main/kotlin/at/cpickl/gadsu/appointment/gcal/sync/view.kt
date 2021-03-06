package at.cpickl.gadsu.appointment.gcal.sync

import at.cpickl.gadsu.appointment.Appointment
import at.cpickl.gadsu.appointment.gcal.GCalEvent
import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.service.LOG
import at.cpickl.gadsu.view.MainFrame
import at.cpickl.gadsu.view.components.MyEnableValue
import at.cpickl.gadsu.view.components.MyFrame
import at.cpickl.gadsu.view.components.MyTableModel
import at.cpickl.gadsu.view.components.TableColumn
import at.cpickl.gadsu.view.components.inputs.HtmlEditorPane
import at.cpickl.gadsu.view.components.panels.GridPanel
import at.cpickl.gadsu.view.swing.ClosableWindow
import at.cpickl.gadsu.view.swing.addCloseListener
import at.cpickl.gadsu.view.swing.registerCloseOnEscapeOrShortcutW
import at.cpickl.gadsu.view.swing.scrolled
import com.google.common.eventbus.EventBus
import org.joda.time.DateTime
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import javax.inject.Inject
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

interface SyncReportWindow : ClosableWindow {
    fun start()
    fun destroy()
    fun initReport(report: SyncReport, clients: List<Client>, gmailConfigured: Boolean)
    fun readImportAppointments(): List<ImportAppointment>
    fun readDeleteAppointments(): List<Appointment>
    fun readUpdateAppointments(): List<Appointment>

}

data class ImportAppointment(
        val event: GCalEvent,
        var enabled: Boolean,
        var sendConfirmation: Boolean,
        var selectedClient: Client,
        val allClients: List<Client>, // order is specific to this appointment
        val isGmailGloballyConfigured: Boolean // kind of a hack
) {

    companion object

    fun toAppointment(created: DateTime): Appointment {
        return Appointment(
                id = null, // gadsu appointment.ID is not known yet
                clientId = this.selectedClient.id!!,
                created = created,
                start = this.event.start,
                end = this.event.end,
                note = this.event.description,
                gcalId = this.event.id,
                gcalUrl = this.event.url
        )
    }
}

class SyncReportSwingWindow @Inject constructor(
        mainFrame: MainFrame,
        bus: EventBus
)
    : MyFrame("Sync Bericht"), SyncReportWindow {

    private val log = LOG(javaClass)

    companion object {
        private val COL_LIL = 30
    }

    private var deleteAppointments = emptyList<Appointment>()
    private var updateAppointments = emptyList<Appointment>()

    // keep in sync with SyncTable.COL indices!!!
    private val model = MyTableModel<ImportAppointment>(listOf(
            TableColumn("", { it.enabled }, COL_LIL, COL_LIL, COL_LIL),
            TableColumn("Titel", { it.event.summary }, 100, 100),
            TableColumn("Klient", { it.selectedClient.preferredName }, 150, 120),
            TableColumn("Zeit", { Pair(it.event.start, it.event.end) }, 130, 130, 130),
            TableColumn("", { MyEnableValue(
                    enabled = it.selectedClient.hasMailAndWantsMail && it.isGmailGloballyConfigured,
                    selected = it.sendConfirmation && it.isGmailGloballyConfigured
            ) }, COL_LIL, COL_LIL, COL_LIL),
            TableColumn("Mail", { it.selectedClient.contact.mail }, 150, 150)
    ))

    private val table = SyncTable(model)

    private val btnImport = JButton("Synchronisieren").apply {
        addActionListener {
            table.cellEditor?.stopCellEditing() // as we are communicating via editor stop events, rather the component's own change event
            bus.post(RequestImportSyncEvent())
        }
    }

    private val topText = HtmlEditorPane()

    init {
        addCloseListener { closeWindow() }
        registerCloseOnEscapeOrShortcutW()

        contentPane.add(GridPanel().apply {
            border = BorderFactory.createEmptyBorder(10, 15, 10, 15)

            c.weightx = 1.0
            c.weighty = 0.0
            c.fill = GridBagConstraints.HORIZONTAL
            add(topText)

            c.gridy++
            c.weighty = 1.0
            c.fill = GridBagConstraints.BOTH
            add(table.scrolled())

            c.gridy++
            c.weighty = 0.0
            c.fill = GridBagConstraints.NONE
            add(JPanel(BorderLayout()).apply {
                add(btnImport, BorderLayout.WEST)
                add(JButton("Abbrechen").apply { addActionListener { closeWindow() } }, BorderLayout.EAST)
            })
        })
        pack()
        setLocationRelativeTo(mainFrame.asJFrame())

        rootPane.defaultButton = btnImport
    }

    override fun readImportAppointments(): List<ImportAppointment> = model.getData()

    override fun readDeleteAppointments(): List<Appointment> = deleteAppointments

    override fun readUpdateAppointments(): List<Appointment> = updateAppointments

    // MINOR @VIEW - outsource logic from proper starting, see PreferencesWindow
    override fun start() {
        isVisible = true
    }

    override fun initReport(
            report: SyncReport,
            clients: List<Client>,
            gmailConfigured: Boolean
    ) {
        log.debug("initReport(report={}, clients={}, gmailConfigured={})", report, clients, gmailConfigured)
        val defaultSelected = clients.first()
        model.resetData(report.importEvents.map { (gcalEvent, suggestedClients) ->
            val selectedClient = suggestedClients.firstOrNull() ?: defaultSelected
            ImportAppointment(
                    event = gcalEvent,
                    enabled = true,
                    sendConfirmation = selectedClient.hasMailAndWantsMail,
                    selectedClient = selectedClient,
                    allClients = clientsOrdered(suggestedClients, clients),
                    isGmailGloballyConfigured = gmailConfigured)
        })

        deleteAppointments = report.deleteAppointments

        // copy values from GCalEvent to local Appointment
        updateAppointments = report.updateAppointments.map {
            it.value.copy(
                    start = it.key.start,
                    end = it.key.end,
                    note = it.key.description
            )
        }

        val isSingular = model.size == 1
        topText.text = "Folgende${if (isSingular) "r" else ""} ${model.size} Termin${if (isSingular) " kann" else "e können"} importiert werden " +
                "(${deleteAppointments.size} zum Löschen, ${updateAppointments.size} zum Updaten):"
    }

    private fun clientsOrdered(topClients: List<Client>, allClients: List<Client>) =
            topClients.union(allClients.minus(topClients)).toList()

    override fun closeWindow() {
        isVisible = false
    }

    override fun destroy() {
        isVisible = false
        dispose()
    }

}

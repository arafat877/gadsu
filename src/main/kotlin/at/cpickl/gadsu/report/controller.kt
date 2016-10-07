package at.cpickl.gadsu.report

import at.cpickl.gadsu.QuitEvent
import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.client.ClientService
import at.cpickl.gadsu.client.CurrentClient
import at.cpickl.gadsu.preferences.PreferencesData
import at.cpickl.gadsu.preferences.Prefs
import at.cpickl.gadsu.report.multiprotocol.MultiProtocolCoverData
import at.cpickl.gadsu.report.multiprotocol.MultiProtocolGenerator
import at.cpickl.gadsu.report.multiprotocol.MultiProtocolRepository
import at.cpickl.gadsu.report.multiprotocol.MultiProtocolStatistics
import at.cpickl.gadsu.report.multiprotocol.MultiProtocolWindow
import at.cpickl.gadsu.report.multiprotocol.ReallyCreateMultiProtocolEvent
import at.cpickl.gadsu.report.multiprotocol.RequestCreateMultiProtocolEvent
import at.cpickl.gadsu.report.multiprotocol.TestCreateMultiProtocolEvent
import at.cpickl.gadsu.service.ChooseFile
import at.cpickl.gadsu.service.Clock
import at.cpickl.gadsu.service.Logged
import at.cpickl.gadsu.service.minutes
import at.cpickl.gadsu.service.nullIfEmpty
import at.cpickl.gadsu.service.toMinutes
import at.cpickl.gadsu.treatment.Treatment
import at.cpickl.gadsu.treatment.TreatmentRepository
import at.cpickl.gadsu.treatment.TreatmentService
import at.cpickl.gadsu.view.components.DialogType
import at.cpickl.gadsu.view.components.Dialogs
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.Subscribe
import com.google.inject.Provider
import org.jfree.data.time.DateRange
import java.io.File
import javax.inject.Inject
import javax.swing.SwingUtilities


@Logged
open class ReportController @Inject constructor(
        private val clientService: ClientService,
        private val treatmentService: TreatmentService,
        private val treatmentRepository: TreatmentRepository,
        private val protocolGenerator: ProtocolGenerator,
        private val clock: Clock,
        private val currentClient: CurrentClient,
        private val preferences: Provider<PreferencesData>,
        private val dialogs: Dialogs,
        private val multiProtocolGenerator: MultiProtocolGenerator,
        private val multiProtocolRepository: MultiProtocolRepository,
        private val windowProvider: Provider<MultiProtocolWindow>,
        private val prefs: Prefs
) {
    companion object {

        @VisibleForTesting fun generateStatistics(protocols: List<ProtocolReportData>): MultiProtocolStatistics {
            val numberOfClients = protocols.size
            val numberOfTreatments = protocols.sumBy { it.rows.size }
            val treatmentDateRange = DateRange(
                    protocols.flatMap { it.rows }.minBy { it.date }!!.date,
                    protocols.flatMap { it.rows }.maxBy { it.date }!!.date)
            val totalTreatmentTime = protocols.sumBy { it.rows.sumBy { it.duration } }
            return MultiProtocolStatistics(numberOfClients, numberOfTreatments, treatmentDateRange, minutes(totalTreatmentTime))
        }
    }

    private var recentWindow: MultiProtocolWindow? = null

    @Subscribe open fun onCreateProtocolEvent(event: CreateProtocolEvent) {
        val client = currentClient.data
        val treatments = treatmentService.findAllFor(client)

        if (treatments.isEmpty()) {
            dialogs.show(
                    title = "Sammelprotokoll",
                    message = "Es konnte kein Sammelprotokoll erstellt werden, da der Klient keine Behandlungen hat.",
                    type = DialogType.WARN
            )
            return
        }

        val report = newProtocolReportData(client)
        protocolGenerator.view(report)
    }

    @Subscribe open fun onRequestCreateMultiProtocolEvent(event: RequestCreateMultiProtocolEvent) {
        val protocolizableTreatments = treatmentRepository.countAllNonProtocolized()
        val window = windowProvider.get()
        recentWindow = window
        SwingUtilities.invokeLater { window.start(protocolizableTreatments) }
    }

    @Subscribe open fun onReallyCreateMultiProtocolEvent(event: ReallyCreateMultiProtocolEvent) {
        createAndSave() {
            it, cover, protocols -> multiProtocolGenerator.generatePdfPersistAndDispatch(it, cover, protocols, event.description)
            recentWindow?.closeWindow()
        }
    }

    @Subscribe open fun onTestCreateMultiProtocolEvent(event: TestCreateMultiProtocolEvent) {
        createAndSave() {
            it, cover, protocols -> multiProtocolGenerator.generatePdf(it, cover, protocols)
        }
    }

    @Subscribe open fun onQuitEvent(event: QuitEvent) {
        if (recentWindow != null) {
            recentWindow!!.closeWindow()
        }
    }

    private fun createAndSave(onSuccessCallback: (File, MultiProtocolCoverData, List<ProtocolReportData>) -> Unit) {
        val protocols = multiProtocolWizard()
        if (protocols.isEmpty()) {
            throw IllegalStateException("Expected protocols to have at least one treatment!")
        }

        val printDate = clock.now()
        val author = preferences.get().username
        val cover = MultiProtocolCoverData(printDate, author, generateStatistics(protocols))

        ChooseFile.savePdf(
                fileTypeLabel = "Sammelprotokoll",
                currentDirectory = prefs.recentSaveMultiProtocolFolder,
                onSuccess = {
                    // TODO show progress bar
                    onSuccessCallback(it, cover, protocols)
                    prefs.recentSaveMultiProtocolFolder = it.parentFile
                    dialogs.show(
                            title = "Sammelprotokoll erstellt",
                            message = "Das Sammelprotokoll wurde erfolgreich gespichert als:\n${it.absolutePath}",
                            type = DialogType.INFO
                    )
                }
        )
    }

    private fun multiProtocolWizard(): List<ProtocolReportData> {
        // ... use wizard to select data ...
        return clientService.findAll().map {
            newProtocolReportData(it) // just select all ATM
        }.filter { it.rows.isNotEmpty() }.toList()
    }

    private fun newProtocolReportData(client: Client): ProtocolReportData {
        val rows = treatmentService.findAllFor(client).sortedBy { it.number }.filter { !multiProtocolRepository.hasBeenProtocolizedYet(it) }.map { it.toReportData() }
        val author = preferences.get().username
        val printDate = clock.now()
        return ProtocolReportData(author, printDate, client.toReportData(), rows)
    }
}

private fun Treatment.toReportData() = TreatmentReportData(id!!, number, date, duration.toMinutes(),
        aboutDiscomfort.nullIfEmpty(), aboutDiagnosis.nullIfEmpty(), aboutContent.nullIfEmpty(), aboutFeedback.nullIfEmpty(), aboutHomework.nullIfEmpty(), aboutUpcoming.nullIfEmpty(), note.nullIfEmpty())

private fun Client.toReportData() = ClientReportData(
        anonymizedName = anonymizedName,
        picture = picture.toReportRepresentation(),

        since = created, // TODO compute date based on first treatment
        birthday = birthday,
        birthPlace = birthPlace,
        livePlace = contact.city,
        relationship = relationship.label,
        children = children.nullIfEmpty(),
        job = job.nullIfEmpty(),
        hobbies = hobbies,

        textsNotes = note,
        textsImpression = textImpression,
        textsMedical = textMedical,
        textsComplaints = textComplaints,
        textsPersonal = textPersonal,
        textsObjective = textObjective,

        tcmProps = CPropsComposer.compose(this),
        tcmNotes = tcmNote.nullIfEmpty()
)

private val Client.birthPlace: String get() {
    if (countryOfOrigin.isEmpty() && origin.isNotEmpty()) {
        return origin
    }
    if (origin.isEmpty() && countryOfOrigin.isNotEmpty()) {
        return countryOfOrigin
    }
    return "$countryOfOrigin ($origin)"
}

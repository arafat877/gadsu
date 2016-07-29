package at.cpickl.gadsu.client.view

import at.cpickl.gadsu.AppStartupEvent
import at.cpickl.gadsu.client.*
import at.cpickl.gadsu.client.view.detail.ClientTabSelected
import at.cpickl.gadsu.client.view.detail.ClientTabType
import at.cpickl.gadsu.client.view.detail.SelectClientTab
import at.cpickl.gadsu.image.DeleteImageEvent
import at.cpickl.gadsu.service.Clock
import at.cpickl.gadsu.service.CurrentPropertiesChangedEvent
import at.cpickl.gadsu.service.LOG
import at.cpickl.gadsu.service.Logged
import at.cpickl.gadsu.treatment.TreatmentCreatedEvent
import at.cpickl.gadsu.treatment.TreatmentDeletedEvent
import at.cpickl.gadsu.treatment.TreatmentRepository
import at.cpickl.gadsu.view.ChangeMainContentEvent
import at.cpickl.gadsu.view.MainContentChangedEvent
import at.cpickl.gadsu.view.components.DialogType
import at.cpickl.gadsu.view.components.Dialogs
import at.cpickl.gadsu.view.logic.*
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject


@Logged
@Suppress("UNUSED_PARAMETER")
open class ClientViewController @Inject constructor(
        private val bus: EventBus,
        private val clock: Clock,
        private val view: ClientView,
        private val clientService: ClientService,
        private val treatmentRepo: TreatmentRepository,
        private val currentClient: CurrentClient,
        private val dialogs: Dialogs
) {

    private val log = LOG(javaClass)

    private val changesChecker = ChangesChecker(dialogs, object : ChangesCheckerCallback {
        override fun isModified() = view.detailView.isModified()
        override fun save() = saveClient(view.detailView.readClient())
    })

    @Subscribe open fun onAppStartupEvent(event: AppStartupEvent) {
        view.masterView.initClients(clientService.findAll(ClientState.ACTIVE).map({ extendClient(it) })) // initially only display actives
        bus.post(ChangeMainContentEvent(view))
        bus.post(CreateNewClientEvent()) // show initial client view for insert prototype (update ui fields)
    }

    @Subscribe open fun onCreateNewClientEvent(event: CreateNewClientEvent) {
        if (changesChecker.checkChanges() === ChangeBehaviour.ABORT) {
            return
        }

        if (currentClient.data.yetPersisted) {
            // there was a client selected, and now we want to create a new client
            bus.post(ClientUnselectedEvent(currentClient.data))
        }

        view.detailView.changeTab(ClientTabType.MAIN)
        view.masterView.selectClient(null)
        val newCreatingClient = Client.INSERT_PROTOTYPE.copy(created = clock.now())

        currentClient.data = newCreatingClient
        view.detailView.focusFirst()
    }


    @Subscribe open fun onSaveClientEvent(event: SaveClientEvent) {
        val client = view.detailView.readClient()
        saveClient(client)
    }

    @Subscribe open fun onClientCreatedEvent(event: ClientCreatedEvent) {
        val xclient = extendClient(event.client)
        val index = view.masterView.model.calculateInsertIndex(xclient)
        currentClient.data = event.client

        view.masterView.insertClient(index, xclient)
        view.masterView.selectClient(event.client)
    }

    @Subscribe open fun onClientUpdatedEvent(event: ClientUpdatedEvent) {
        view.masterView.changeClient(event.client)
//        view.masterView.selectClient(event.client) ... nope, not needed

        currentClient.data = event.client
    }

    @Subscribe open fun onClientSelectedEvent(event: ClientSelectedEvent) {
        if (changesChecker.checkChanges() == ChangeBehaviour.ABORT) {
            view.masterView.selectClient(event.previousSelected) // reset selection
            return
        }
        currentClient.data = event.client
        view.closePreparations()
    }

    @Subscribe open fun onDeleteClientEvent(event: DeleteClientEvent) {
        dialogs.confirmedDelete("den Klienten '${event.client.fullName}'", {
            clientService.delete(event.client)

            if (event.client.id!!.equals(currentClient.data.id)) {
                bus.post(ClientUnselectedEvent(event.client))
            }
        })
    }

    @Subscribe open fun onDeleteImageEvent(event: DeleteImageEvent) {
        clientService.deletePicture(event.client)
    }

    @Subscribe open fun onClientDeletedEvent(event: ClientDeletedEvent) {
        view.masterView.deleteClient(event.client)

        if (currentClient.data.id != null && currentClient.data.id.equals(event.client.id)) {
            val newInsert = Client.INSERT_PROTOTYPE
            currentClient.data = newInsert
        }
    }

    @Subscribe open fun onCurrentPropertiesChangedEvent(event: CurrentPropertiesChangedEvent) {
        event.forClient { if (it != null && it.yetPersisted) view.masterView.changeClient(it) }
    }

    @Subscribe open fun onShowClientViewEvent(event: ShowClientViewEvent) {
        bus.post(ChangeMainContentEvent(view))
    }

    @Subscribe open fun onMainContentChangedEvent(event: MainContentChangedEvent) {
        if (event.oldContent === view) { // navigate away
            view.closePreparations()
        }
    }

    @Subscribe open fun onClientTabSelected(event: ClientTabSelected) {
        view.detailView.closePreparations()
    }

    @Subscribe open fun onSelectClientTab(event: SelectClientTab) {
        view.detailView.changeTab(event.tab)
    }

    @Subscribe open fun onShowInClientsListEvent(event: ShowInClientsListEvent) {
        val clients = clientService.findAll(filterState = if(event.showInactives) null else ClientState.ACTIVE)
        view.masterView.initClients(clients.map { extendClient(it) })
    }

    @Subscribe open fun onClientNavigateUpEvent(event: ClientNavigateUpEvent) {
        view.masterView.selectPrevious()
    }

    @Subscribe open fun onClientNavigateDownEvent(event: ClientNavigateDownEvent) {
        view.masterView.selectNext()
    }

    private fun extendClient(client: Client): ExtendedClient {
        // FIXME change number of treatments dynamically on create/delete treatment
        return ExtendedClient(client, treatmentRepo.countAllFor(client))
    }

    @Subscribe open fun onTreatmentCreatedEvent(event: TreatmentCreatedEvent) {
        view.masterView.treatmentCountIncrease(event.treatment.clientId)
    }

    @Subscribe open fun onTreatmentDeletedEvent(event: TreatmentDeletedEvent) {
        view.masterView.treatmentCountDecrease(event.treatment.clientId)
    }

    private fun saveClient(client: Client) {
        log.trace("saveClient(client={})", client)

        if (client.firstName.isEmpty() && client.lastName.isEmpty()) {
            dialogs.show(
                    title = "Speichern abgebrochen",
                    message = "Es muss zumindest entweder ein Vorname oder ein Nachname eingegeben werden.",
                    buttonLabels = arrayOf("Speichern Abbrechen"),
                    type = DialogType.WARN
            )
            return
        }

        clientService.insertOrUpdate(client)
    }

    fun checkChanges(): ChangeBehaviour {
        return changesChecker.checkChanges()
    }
}

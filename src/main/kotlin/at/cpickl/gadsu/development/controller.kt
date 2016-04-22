package at.cpickl.gadsu.development

import at.cpickl.gadsu.DUMMY_CREATED
import at.cpickl.gadsu.QuitUserEvent
import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.client.ClientCreatedEvent
import at.cpickl.gadsu.client.ClientRepository
import at.cpickl.gadsu.client.ClientService
import at.cpickl.gadsu.client.Contact
import at.cpickl.gadsu.client.Gender
import at.cpickl.gadsu.client.Relationship
import at.cpickl.gadsu.image.MyImage
import at.cpickl.gadsu.service.CurrentEvent
import at.cpickl.gadsu.service.DateFormats
import at.cpickl.gadsu.service.Logged
import at.cpickl.gadsu.service.forClient
import at.cpickl.gadsu.treatment.Treatment
import at.cpickl.gadsu.treatment.TreatmentCreatedEvent
import at.cpickl.gadsu.treatment.TreatmentRepository
import at.cpickl.gadsu.view.MainFrame
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.joda.time.DateTime
import javax.inject.Inject


@Logged
@Suppress("UNUSED_PARAMETER")
open class DevelopmentController @Inject constructor(
        private val clientRepo: ClientRepository,
        private val clientService: ClientService,
        private val treatmentRepo: TreatmentRepository,
        private val bus: EventBus,
        private val mainFrame: MainFrame
) {

    private var devFrame: DevFrame? = null

    @Subscribe open fun onShowDevWindowEvent(event: ShowDevWindowEvent) {
        devFrame = DevFrame(mainFrame.dockPositionRight)
        devFrame!!.start()
    }

    @Subscribe open fun onCurrentEvent(event: CurrentEvent) {
        event.forClient { devFrame?.updateClient(it) }
    }

    @Subscribe open fun onDevelopmentResetDataEvent(event: DevelopmentResetDataEvent) {
        deleteAll()

        arrayOf(
                Client(null, DUMMY_CREATED, "Max", "Mustermann",
                        Contact(
                                mail = "max@mustermann.at",
                                phone = "0699 11 22 33 432",
                                street = "Hauptstrasse 22/11/A",
                                zipCode = "1010",
                                city = "Wien"
                        ),
                        DateFormats.DATE.parseDateTime("26.10.1986"), Gender.MALE, "\u00d6sterreich",
                        Relationship.MARRIED, "Computermensch", "keine", "Meine supi wuzi Anmerkung.",
                        MyImage.DEFAULT_PROFILE_MAN
                ),
                Client.INSERT_PROTOTYPE.copy(
                        firstName = "Anna",
                        lastName = "Nym",
                        gender = Gender.FEMALE,
                        picture = MyImage.DEFAULT_PROFILE_WOMAN
                )
        ).forEach {
            val savedClient = clientRepo.insertWithoutPicture(it)
            bus.post(ClientCreatedEvent(savedClient))

            if (savedClient.firstName.equals("Max")) {
                arrayOf(
                        Treatment.insertPrototype(
                                clientId = savedClient.id!!,
                                number = 1,
                                date = DateTime.now(),
                                note = "my note for treatment 1 for maxiiii"
                        )
                ).forEach {
                    treatmentRepo.insert(it)
                    bus.post(TreatmentCreatedEvent(it))
                }
            }
        }
    }

    @Subscribe open fun onDevelopmentClearDataEvent(event: DevelopmentClearDataEvent) {
        deleteAll()
    }

    @Subscribe open fun onQuitUserEvent(event: QuitUserEvent) {
        devFrame?.close()
    }

    private fun deleteAll() {
        clientService.findAll().forEach { // not directly supported in service, as this is a DEV feature only!
            clientService.delete(it)
        }
    }

}
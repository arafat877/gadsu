package at.cpickl.gadsu.client

import at.cpickl.gadsu.client.xprops.ROW_MAPPER
import at.cpickl.gadsu.client.xprops.SProp
import at.cpickl.gadsu.client.xprops.XPropsService
import at.cpickl.gadsu.client.xprops.XPropsServiceImpl
import at.cpickl.gadsu.client.xprops.XPropsSqlJdbcRepository
import at.cpickl.gadsu.client.xprops.XPropsSqlRepository
import at.cpickl.gadsu.client.xprops.model.CProps
import at.cpickl.gadsu.tcm.model.XProps
import at.cpickl.gadsu.testinfra.HsqldbTest
import at.cpickl.gadsu.testinfra.copyWithoutCprops
import at.cpickl.gadsu.testinfra.unsavedValidInstance
import at.cpickl.gadsu.treatment.Treatment
import at.cpickl.gadsu.treatment.TreatmentJdbcRepository
import at.cpickl.gadsu.treatment.TreatmentRepository
import at.cpickl.gadsu.treatment.TreatmentService
import at.cpickl.gadsu.treatment.TreatmentServiceImpl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test


@Test(groups = arrayOf("hsqldb", "integration"))
class ClientServiceImplIntegrationTest : HsqldbTest() {

    private val unsavedClient = Client.unsavedValidInstance().copy(cprops = CProps.builder
        .add(XProps.Sleep, XProps.SleepOpts.NeedMuch).build()
    )

    private lateinit var clientRepo: ClientRepository
    private lateinit var propsRepo: XPropsSqlRepository
    private lateinit var propsService: XPropsService
    private lateinit var treatmentRepo: TreatmentRepository
    private lateinit var treatmentService: TreatmentService

    private lateinit var testee: ClientService

    @BeforeMethod
    fun setUp() {
        clientRepo = ClientJdbcRepository(jdbcx, idGenerator)
        propsRepo = XPropsSqlJdbcRepository(jdbcx)
        propsService = XPropsServiceImpl(propsRepo)
        treatmentRepo = TreatmentJdbcRepository(jdbcx, idGenerator)
        treatmentService = TreatmentServiceImpl(treatmentRepo, jdbcx, bus, clock)

        testee = ClientServiceImpl(clientRepo, propsService, treatmentService, jdbcx, bus, clock, currentClient)
    }

    fun `insert client sunshine`() {
        val savedClient = testee.insertOrUpdate(unsavedClient)

        assertThat(savedClient, equalTo(unsavedClient.copy(id = "1")))

        assertRows(TABLE_CLIENT, Client.ROW_MAPPER, savedClient.copyWithoutCprops())
        assertEmptyTable(TABLE_TREATMENT)
        assertRows(TABLE_XPROPS, SProp.ROW_MAPPER, SProp("Sleep", "Sleep_NeedMuch"))

        busListener.assertContains(ClientCreatedEvent(savedClient))
    }

    fun `update client sunshine`() {
        val oldClient = insertClientViaRepo(unsavedClient.copyWithoutCprops())
        val pleaseUpdateMe = oldClient.copy(job = "Leader")

        // MINOR TEST do same for client with picture, and different xprops
        testee.insertOrUpdate(pleaseUpdateMe)

        assertRows(TABLE_CLIENT, Client.ROW_MAPPER, pleaseUpdateMe)
        assertEmptyTable(TABLE_TREATMENT)
        assertEmptyTable(TABLE_XPROPS)

        busListener.assertContains(ClientUpdatedEvent(pleaseUpdateMe))
    }

    fun deleteClientWithSomeTreatments_willSucceedAsInternallyDeletesAllTreatmentsFirst() {
        val savedClient = clientRepo.insertWithoutPicture(unsavedClient)
        treatmentRepo.insert(Treatment.unsavedValidInstance(savedClient.id!!))

        testee.delete(savedClient)

        assertEmptyTable(TABLE_CLIENT)
        assertEmptyTable(TABLE_TREATMENT)
        assertEmptyTable(TABLE_XPROPS)
    }

}
